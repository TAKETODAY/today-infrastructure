/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.framework.server.light;

import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import cn.taketoday.web.Constant;
import cn.taketoday.web.http.DefaultHttpHeaders;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.http.HttpStatus;

import static cn.taketoday.framework.server.light.Utils.escapeHTML;
import static cn.taketoday.framework.server.light.Utils.isCompressible;
import static cn.taketoday.framework.server.light.Utils.splitElements;
import static cn.taketoday.framework.server.light.Utils.transfer;

/**
 * The {@code Response} class encapsulates a single HTTP response.
 *
 * @author TODAY 2021/4/13 11:31
 */
public class Response implements Closeable {
  private static final byte[] COLON = ": ".getBytes(StandardCharsets.UTF_8);
  private static final byte[] BLANK_SPACE = " ".getBytes(StandardCharsets.UTF_8);
  private static final byte[] VERSION_BYTES = "HTTP/1.1 ".getBytes(StandardCharsets.UTF_8);

  protected final OutputStream out; // the underlying output stream
  protected OutputStream[] encoders = new OutputStream[4]; // chained encoder streams
  protected final DefaultHttpHeaders headers;
  protected boolean discardBody;
  protected int state; // nothing sent, headers sent, or closed
  protected LightRequest req; // request used in determining client capabilities

  /**
   * Constructs a Response whose output is written to the given stream.
   *
   * @param out
   *         the stream to which the response is written
   */
  public Response(OutputStream out) {
    this.out = out;
    this.headers = new DefaultHttpHeaders();
  }

  /**
   * Sets whether this response's body is discarded or sent.
   *
   * @param discardBody
   *         specifies whether the body is discarded or not
   */
  public void setDiscardBody(boolean discardBody) {
    this.discardBody = discardBody;
  }

  /**
   * Sets the request which is used in determining the capabilities
   * supported by the client (e.g. compression, encoding, etc.)
   *
   * @param req
   *         the request
   */
  public void setClientCapabilities(LightRequest req) {
    this.req = req;
  }

  /**
   * Returns the request headers collection.
   *
   * @return the request headers collection
   */
  public HttpHeaders getHeaders() {
    return headers;
  }

  /**
   * Returns the underlying output stream to which the response is written.
   * Except for special cases, you should use {@link #getBody()} instead.
   *
   * @return the underlying output stream to which the response is written
   */
  public OutputStream getOutputStream() {
    return out;
  }

  /**
   * Returns whether the response headers were already sent.
   *
   * @return whether the response headers were already sent
   */
  public boolean headersSent() {
    return state == 1;
  }

  /**
   * Returns an output stream into which the response body can be written.
   * The stream applies encodings (e.g. compression) according to the sent headers.
   * This method must be called after response headers have been sent
   * that indicate there is a body. Normally, the content should be
   * prepared (not sent) even before the headers are sent, so that any
   * errors during processing can be caught and a proper error response returned -
   * after the headers are sent, it's too late to change the status into an error.
   *
   * @return an output stream into which the response body can be written,
   * or null if the body should not be written (e.g. it is discarded)
   *
   * @throws IOException
   *         if an error occurs
   */
  public OutputStream getBody() throws IOException {
    if (encoders[0] != null || discardBody)
      return encoders[0]; // return the existing stream (or null)
    // set up chain of encoding streams according to headers
//    final List<String> te = headers.get("Transfer-Encoding");
//    final List<String> ce = headers.get("Content-Encoding");

    List<String> te = Arrays.asList(splitElements(headers.getFirst("Transfer-Encoding"), true));
    List<String> ce = Arrays.asList(splitElements(headers.getFirst("Content-Encoding"), true));

    int i = encoders.length - 1;
    encoders[i] = new FilterOutputStream(out) {
      @Override
      public void close() {} // keep underlying connection stream open for now

      @Override // override the very inefficient default implementation
      public void write(byte[] b, int off, int len) throws IOException { out.write(b, off, len); }
    };

    if (te.contains("chunked"))
      encoders[--i] = new ChunkedOutputStream(encoders[i + 1]);
    if (ce.contains("gzip") || te.contains("gzip"))
      encoders[--i] = new GZIPOutputStream(encoders[i + 1], 4096);
    else if (ce.contains("deflate") || te.contains("deflate"))
      encoders[--i] = new DeflaterOutputStream(encoders[i + 1]);
    encoders[0] = encoders[i];
    encoders[i] = null; // prevent duplicate reference
    return encoders[0]; // returned stream is always first
  }

  /**
   * Closes this response and flushes all output.
   *
   * @throws IOException
   *         if an error occurs
   */
  public void close() throws IOException {
    state = -1; // closed
    if (encoders[0] != null)
      encoders[0].close(); // close all chained streams (except the underlying one)
    out.flush(); // always flush underlying stream (even if getBody was never called)
  }

  /**
   * Sends the response headers with the given response status.
   * A Date header is added if it does not already exist.
   * If the response has a body, the Content-Length/Transfer-Encoding
   * and Content-Type headers must be set before sending the headers.
   *
   * @param status
   *         the response status
   *
   * @throws IOException
   *         if an error occurs or headers were already sent
   * @see #sendHeaders(int, long, long, String, String, long[])
   */
  public void sendHeaders(int status) throws IOException {
    sendHttpHeaders(HttpStatus.valueOf(status));
  }

  public void sendHttpHeaders(HttpStatus status) throws IOException {
    if (headersSent()) {
      throw new IOException("headers were already sent");
    }
    final DefaultHttpHeaders headers = this.headers;
    if (!headers.containsKey(Constant.DATE)) {
      headers.add(Constant.DATE, Utils.formatDate(System.currentTimeMillis()));
    }
    if (serverHeader != null) {
      headers.add(Constant.SERVER, serverHeader);
    }

    final OutputStream output = this.out;
    final Charset utf8 = StandardCharsets.UTF_8;
    // response line
    output.write(VERSION_BYTES);
    output.write(Integer.toString(status.value()).getBytes(utf8));
    output.write(BLANK_SPACE);
    output.write(status.getReasonPhrase().getBytes(utf8));
    output.write(HTTPServer.CRLF);

    // response headers
    for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
      final String name = entry.getKey();
      final byte[] nameBytes = name.getBytes(utf8);
      for (final String value : entry.getValue()) {
        output.write(nameBytes);
        output.write(COLON); // ': '
        output.write(value.getBytes(utf8));
        output.write(HTTPServer.CRLF);
      }
    }

    state = 1; // headers sent
  }

  private String serverHeader = "JLHTTP/2.5";

  public void setServerHeader(String serverHeader) {
    this.serverHeader = serverHeader;
  }

  public String getServerHeader() {
    return serverHeader;
  }

  /**
   * Sends the response headers, including the given response status
   * and description, and all response headers. If they do not already
   * exist, the following headers are added as necessary:
   * Content-Range, Content-Type, Transfer-Encoding, Content-Encoding,
   * Content-Length, Last-Modified, ETag, Connection  and Date. Ranges are
   * properly calculated as well, with a 200 status changed to a 206 status.
   *
   * @param status
   *         the response status
   * @param length
   *         the response body length, or zero if there is no body,
   *         or negative if there is a body but its length is not yet known
   * @param lastModified
   *         the last modified date of the response resource,
   *         or non-positive if unknown. A time in the future will be
   *         replaced with the current system time.
   * @param etag
   *         the ETag of the response resource, or null if unknown
   *         (see RFC2616#3.11)
   * @param contentType
   *         the content type of the response resource, or null
   *         if unknown (in which case "application/octet-stream" will be sent)
   * @param range
   *         the content range that will be sent, or null if the
   *         entire resource will be sent
   *
   * @throws IOException
   *         if an error occurs
   */
  public void sendHeaders(int status, long length, long lastModified,
                          String etag, String contentType, long[] range) throws IOException {
    final DefaultHttpHeaders responseHeaders = this.headers;
    if (range != null) {
      responseHeaders.add(Constant.CONTENT_RANGE, "bytes " + range[0] + "-" +
              range[1] + "/" + (length >= 0 ? length : "*"));
      length = range[1] - range[0] + 1;
      if (status == 200)
        status = 206;
    }
    String ct = responseHeaders.getFirst(Constant.CONTENT_TYPE);
    if (ct == null) {
      ct = contentType != null ? contentType : Constant.APPLICATION_OCTET_STREAM;
      responseHeaders.add(Constant.CONTENT_TYPE, ct);
    }

    if (!responseHeaders.containsKey(Constant.CONTENT_LENGTH)
            && !responseHeaders.containsKey(Constant.TRANSFER_ENCODING)) {
      // RFC2616#3.6: transfer encodings are case-insensitive and must not be sent to an HTTP/1.0 client
      boolean modern = req != null && req.getVersion().endsWith("1.1");
      String accepted = req == null ? null : req.getHeaders().getFirst(Constant.ACCEPT_ENCODING);
      List<String> encodings = Arrays.asList(splitElements(accepted, true));
      String compression = encodings.contains(Constant.GZIP)
                           ? Constant.GZIP
                           : encodings.contains(Constant.DEFLATE)
                             ? Constant.DEFLATE
                             : null;
      if (compression != null && (length < 0 || length > 300) && isCompressible(ct) && modern) {
        responseHeaders.add(Constant.TRANSFER_ENCODING, Constant.CHUNKED); // compressed data is always unknown length
        responseHeaders.add(Constant.CONTENT_ENCODING, compression);
      }
      else if (length < 0 && modern) {
        responseHeaders.add(Constant.TRANSFER_ENCODING, Constant.CHUNKED); // unknown length
      }
      else if (length >= 0) {
        responseHeaders.setContentLength(length); // known length
      }
    }
    if (!responseHeaders.containsKey(Constant.VARY)) {
      // RFC7231#7.1.4: Vary field should include headers
      responseHeaders.add(Constant.VARY, Constant.ACCEPT_ENCODING); // that are used in selecting representation
    }
    if (lastModified > 0 && !responseHeaders.containsKey(Constant.LAST_MODIFIED)) {
      // RFC2616#14.29
      responseHeaders.add(Constant.LAST_MODIFIED, Utils.formatDate(Math.min(lastModified, System.currentTimeMillis())));
    }
    if (etag != null && !responseHeaders.containsKey(Constant.ETAG)) {
      responseHeaders.add(Constant.ETAG, etag);
    }
    if (req != null && Constant.CLOSE.equalsIgnoreCase(req.getHeaders().getFirst(Constant.CONNECTION))
            && !responseHeaders.containsKey(Constant.CONNECTION)) {
      responseHeaders.add(Constant.CONNECTION, Constant.CLOSE); // #RFC7230#6.6: should reply to close with close
    }
    sendHeaders(status);
  }

  /**
   * Sends the full response with the given status, and the given string
   * as the body. The text is sent in the UTF-8 charset. If a
   * Content-Type header was not explicitly set, it will be set to
   * text/html, and so the text must contain valid (and properly
   * {@link Utils#escapeHTML escaped}) HTML.
   *
   * @param status
   *         the response status
   * @param text
   *         the text body (sent as text/html)
   *
   * @throws IOException
   *         if an error occurs
   */
  public void send(int status, String text) throws IOException {
    byte[] content = text.getBytes(StandardCharsets.UTF_8);
    sendHeaders(status, content.length, -1,
                "W/\"" + Integer.toHexString(text.hashCode()) + "\"",
                "text/html; charset=utf-8", null);
    OutputStream out = getBody();
    if (out != null)
      out.write(content);
  }

  /**
   * Sends an error response with the given status and detailed message.
   * An HTML body is created containing the status and its description,
   * as well as the message, which is escaped using the
   * {@link Utils#escapeHTML escape} method.
   *
   * @param status
   *         the response status
   * @param text
   *         the text body (sent as text/html)
   *
   * @throws IOException
   *         if an error occurs
   */
  public void sendError(int status, String text) throws IOException {
    final HttpStatus httpStatus = HttpStatus.valueOf(status);
    send(status, String.format(
            "<!DOCTYPE html>%n<html>%n<head><title>%d %s</title></head>%n" +
                    "<body><h1>%d %s</h1>%n<p>%s</p>%n</body></html>",
            status, httpStatus.getReasonPhrase(), status, httpStatus.getReasonPhrase(), escapeHTML(text)));
  }

  /**
   * Sends an error response with the given status and default body.
   *
   * @param status
   *         the response status
   *
   * @throws IOException
   *         if an error occurs
   */
  public void sendError(int status) throws IOException {
    String text = status < 400 ? ":)" : "sorry it didn't work out :(";
    sendError(status, text);
  }

  /**
   * Sends the response body. This method must be called only after the
   * response headers have been sent (and indicate that there is a body).
   *
   * @param body
   *         a stream containing the response body
   * @param length
   *         the full length of the response body, or -1 for the whole stream
   * @param range
   *         the sub-range within the response body that should be
   *         sent, or null if the entire body should be sent
   *
   * @throws IOException
   *         if an error occurs
   */
  public void sendBody(InputStream body, long length, long[] range) throws IOException {
    OutputStream out = getBody();
    if (out != null) {
      if (range != null) {
        long offset = range[0];
        length = range[1] - range[0] + 1;
        while (offset > 0) {
          long skip = body.skip(offset);
          if (skip == 0)
            throw new IOException("can't skip to " + range[0]);
          offset -= skip;
        }
      }
      transfer(body, out, length);
    }
  }

  /**
   * Sends a 301 or 302 response, redirecting the client to the given URL.
   *
   * @param url
   *         the absolute URL to which the client is redirected
   * @param permanent
   *         specifies whether a permanent (301) or
   *         temporary (302) redirect status is sent
   *
   * @throws IOException
   *         if an IO error occurs or url is malformed
   */
  public void redirect(String url, boolean permanent) throws IOException {
    try {
      url = new URI(url).toASCIIString();
    }
    catch (URISyntaxException e) {
      throw new IOException("malformed URL: " + url);
    }
    headers.add("Location", url);
    // some user-agents expect a body, so we send it
    if (permanent)
      sendError(301, "Permanently moved to " + url);
    else
      sendError(302, "Temporarily moved to " + url);
  }
}
