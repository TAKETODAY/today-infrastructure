/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.framework.server.light;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.web.RequestContextUtils;

import static cn.taketoday.web.framework.server.light.Utils.detectLocalHostName;
import static cn.taketoday.web.framework.server.light.Utils.parseRange;
import static cn.taketoday.web.framework.server.light.Utils.parseULong;
import static cn.taketoday.web.framework.server.light.Utils.readHeaders;
import static cn.taketoday.web.framework.server.light.Utils.readLine;
import static cn.taketoday.web.framework.server.light.Utils.readToken;
import static cn.taketoday.web.framework.server.light.Utils.split;
import static cn.taketoday.web.framework.server.light.Utils.splitElements;
import static cn.taketoday.web.framework.server.light.Utils.trimDuplicates;

/**
 * The {@code Request} class encapsulates a single HTTP request.
 *
 * @author TODAY 2021/4/13 11:32
 */
public final class HttpRequest {

  private final Socket socket;
  protected String method;
  protected URI uri;
  protected URL baseURL; // cached value
  protected String version;
  //  protected Headers headers;
  protected HttpHeaders requestHeaders;
  protected InputStream body;
  protected MultiValueMap<String, String> params; // cached value
  protected boolean secure;
  protected int port;

  protected String requestURI;

  public String getRequestURI() {
    return requestURI;
  }

  public Socket getSocket() {
    return socket;
  }

  /**
   * Constructs a Request from the data in the given input stream.
   *
   * @param in the input stream from which the request is read
   * @throws IOException if an error occurs
   */
  public HttpRequest(InputStream in, Socket socket, LightHttpConfig config) throws IOException {
    readRequestLine(in);
    this.socket = socket;
    final HttpHeaders requestHeaders = readHeaders(in, config);
    this.requestHeaders = requestHeaders;
    // RFC2616#3.6 - if "chunked" is used, it must be the last one
    // RFC2616#4.4 - if non-identity Transfer-Encoding is present,
    // it must either include "chunked" or close the connection after
    // the body, and in any case ignore Content-Length.
    // if there is no such Transfer-Encoding, use Content-Length
    // if neither header exists, there is no body
    String header = requestHeaders.getFirst(HttpHeaders.TRANSFER_ENCODING);
    if (header != null && !header.toLowerCase(Locale.US).equals(HttpHeaders.IDENTITY)) {
      if (Arrays.asList(splitElements(header, true)).contains(HttpHeaders.CHUNKED))
        body = new ChunkedInputStream(in, requestHeaders, config);
      else
        body = in; // body ends when connection closes
    }
    else {
      header = requestHeaders.getFirst(HttpHeaders.CONTENT_LENGTH);
      long len = header == null ? 0 : parseULong(header, 10);
      body = new LimitedInputStream(in, len, false);
    }
  }

  /**
   * Returns the request method.
   *
   * @return the request method
   */
  public String getMethod() {
    return method;
  }

  /**
   * Returns the request URI.
   *
   * @return the request URI
   */
  public URI getURI() {
    return uri;
  }

  /**
   * Returns the request version string.
   *
   * @return the request version string
   */
  public String getVersion() {
    return version;
  }

  /**
   * Returns the request headers.
   *
   * @return the request headers
   */
  public HttpHeaders getHeaders() {
    return requestHeaders;
  }

  /**
   * Returns the input stream containing the request body.
   *
   * @return the input stream containing the request body
   */
  public InputStream getBody() {
    return body;
  }

  /**
   * Returns the path component of the request URI, after
   * URL decoding has been applied (using the UTF-8 charset).
   *
   * @return the decoded path component of the request URI
   */
  public String getPath() {
    return uri.getPath();
  }

  /**
   * Sets the path component of the request URI. This can be useful
   * in URL rewriting, etc.
   *
   * @param path the path to set
   * @throws IllegalArgumentException if the given path is malformed
   */
  public void setPath(String path) {
    try {
      uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
              trimDuplicates(path, '/'), uri.getQuery(), uri.getFragment());
    }
    catch (URISyntaxException use) {
      throw new IllegalArgumentException("error setting path", use);
    }
  }

  /**
   * Returns the base URL (scheme, host and port) of the request resource.
   * The host name is taken from the request URI or the Host header or a
   * default host (see RFC2616#5.2).
   *
   * @return the base URL of the requested resource, or null if it
   * is malformed
   */
  public URL getBaseURL() {
    if (baseURL != null)
      return baseURL;
    // normalize host header
    String host = uri.getHost();
    if (host == null) {
      host = requestHeaders.getFirst(HttpHeaders.HOST);
      if (host == null) // missing in HTTP/1.0
        host = detectLocalHostName();
    }
    int pos = host.indexOf(':');
    host = pos < 0 ? host : host.substring(0, pos);
    try {
      return baseURL = new URL(secure ? "https" : "http", host, port, "");
    }
    catch (MalformedURLException e) {
      return null;
    }
  }

  /**
   * Returns the request parameters, which are parsed both from the query
   * part of the request URI, and from the request body if its content
   * type is "application/x-www-form-urlencoded" (i.e. a submitted form).
   * UTF-8 encoding is assumed in both cases.
   * <p>
   * The parameters are returned as a list of string arrays, each containing
   * the parameter name as the first element and its corresponding value
   * as the second element (or an empty string if there is no value).
   * <p>
   * The list retains the original order of the parameters.
   *
   * @return the request parameters name-value pairs,
   * or an empty list if there are none
   * @throws IOException if an error occurs
   * @see Utils#parseParamsList(String)
   */
  public MultiValueMap<String, String> parseParameters() throws IOException {
    MultiValueMap<String, String> parameters = RequestContextUtils.parseParameters(uri.getRawQuery());
    String ct = requestHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
    if (ct != null && ct.toLowerCase(Locale.US).startsWith(HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED)) {
      String bodyString = readToken(body, -1, StandardCharsets.UTF_8, 2097152); // 2MB limit
      RequestContextUtils.parseParameters(parameters, bodyString);
    }
    return parameters;
  }

  /**
   * Returns the request parameters, which are parsed both from the query
   * part of the request URI, and from the request body if its content
   * type is "application/x-www-form-urlencoded" (i.e. a submitted form).
   * UTF-8 encoding is assumed in both cases.
   * <p>
   * For multivalued parameters (i.e. multiple parameters with the same
   * name), only the first one is considered. For access to all values,
   * use {@link #parseParameters()} instead.
   * <p>
   * The map iteration retains the original order of the parameters.
   *
   * @return the request parameters name-value pairs,
   * or an empty map if there are none
   * @throws IOException if an error occurs
   * @see #parseParameters()
   */
  public MultiValueMap<String, String> getParameters() throws IOException {
    if (params == null) {
      params = parseParameters();
    }
    return params;
  }

  /**
   * Returns the absolute (zero-based) content range value read
   * from the Range header. If multiple ranges are requested, a single
   * range containing all of them is returned.
   *
   * @param length the full length of the requested resource
   * @return the requested range, or null if the Range header
   * is missing or invalid
   */
  public long[] getRange(long length) {
    String header = requestHeaders.getFirst(HttpHeaders.RANGE);
    return header == null || !header.startsWith("bytes=")
           ? null : parseRange(header.substring(6), length);
  }

  /**
   * Reads the request line, parsing the method, URI and version string.
   *
   * @param in the input stream from which the request line is read
   * @throws IOException if an error occurs or the request line is invalid
   */
  protected void readRequestLine(InputStream in) throws IOException {
    // RFC2616#4.1: should accept empty lines before request line
    // RFC2616#19.3: tolerate additional whitespace between tokens
    String line;
    try {
      do {
        line = readLine(in);
      }
      while (line.length() == 0);
    }
    catch (IOException ioe) { // if EOF, timeout etc.
      throw new IOException("missing request line", ioe); // signal that the request did not begin
    }
    String[] tokens = split(line, " ", -1);
    if (tokens.length != 3)
      throw new IOException("invalid request line: \"" + line + "\"");
    try {
      method = tokens[0];
      // must remove '//' prefix which constructor parses as host name
      this.uri = new URI(trimDuplicates(tokens[1], '/'));
      this.requestURI = uri.getRawPath();
      version = tokens[2]; // RFC2616#2.1: allow implied LWS; RFC7230#3.1.1: disallow it
    }
    catch (URISyntaxException use) {
      throw new IOException("invalid URI: " + use.getMessage());
    }
  }

}
