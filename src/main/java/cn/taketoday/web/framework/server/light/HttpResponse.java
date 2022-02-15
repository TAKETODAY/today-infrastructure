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

import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
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

import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.web.framework.server.light.Utils.escapeHTML;
import static cn.taketoday.web.framework.server.light.Utils.splitElements;

/**
 * The {@code Response} class encapsulates a single HTTP response.
 *
 * @author TODAY 2021/4/13 11:31
 */
public class HttpResponse implements Closeable {
  /** A convenience array containing the carriage-return and line feed chars. */
  public static final byte[] CRLF = { 0x0d, 0x0a };
  public static final byte[] COLON = ": ".getBytes(StandardCharsets.UTF_8);
  public static final byte[] BLANK_SPACE = " ".getBytes(StandardCharsets.UTF_8);
  public static final byte[] VERSION_BYTES = "HTTP/1.1 ".getBytes(StandardCharsets.UTF_8);

  protected final OutputStream out; // the underlying output stream
  protected OutputStream[] encoders = new OutputStream[4]; // chained encoder streams
  protected boolean discardBody;
  protected HttpRequest req; // request used in determining client capabilities

  protected HttpStatus status = HttpStatus.OK;
  protected String serverHeader = "JLHTTP/2.5";
  protected final DefaultHttpHeaders headers = new DefaultHttpHeaders();

  protected boolean committed = false;

  /**
   * Constructs a Response whose output is written to the given stream.
   *
   * @param out the stream to which the response is written
   */
  protected HttpResponse(OutputStream out) {
    this.out = out;
  }

  /**
   * Sets whether this response's body is discarded or sent.
   *
   * @param discardBody specifies whether the body is discarded or not
   */
  public void setDiscardBody(boolean discardBody) {
    this.discardBody = discardBody;
  }

  /**
   * Sets the request which is used in determining the capabilities
   * supported by the client (e.g. compression, encoding, etc.)
   *
   * @param req the request
   */
  public void setClientCapabilities(HttpRequest req) {
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

  public void setServerHeader(String serverHeader) {
    this.serverHeader = serverHeader;
  }

  public String getServerHeader() {
    return serverHeader;
  }

  public void setStatus(HttpStatus status) {
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }

  /**
   * Sends the response headers with the given response status.
   * A Date header is added if it does not already exist.
   * If the response has a body, the Content-Length/Transfer-Encoding
   * and Content-Type headers must be set before sending the headers.
   *
   * @param status the response status
   * @throws IOException if an error occurs or headers were already sent
   * @see HttpStatus
   */
  public void send(final HttpStatus status) throws IOException {
    send(status, null);
  }

  /**
   * Sends the full response with the given status, and the given string
   * as the body. The text is sent in the UTF-8 charset. If a
   * Content-Type header was not explicitly set, it will be set to
   * text/html, and so the text must contain valid (and properly
   * {@link Utils#escapeHTML escaped}) HTML.
   *
   * @param status the response status
   * @param text the text body (sent as text/html)
   * @throws IOException if an error occurs
   * @see HttpStatus
   */
  public void send(final HttpStatus status, final String text) throws IOException {
    this.status = status;
    if (text != null) {
      final byte[] content = text.getBytes(StandardCharsets.UTF_8);
      final DefaultHttpHeaders headers = this.headers;
      headers.setETag("W/\"" + Integer.toHexString(text.hashCode()) + "\"");
      headers.set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
      write(status, headers, ResponseOutputBuffer.ofBytes(content));
    }
    else {
      write(status, headers, null);
    }
  }

  /**
   * Sends an error response with the given status and default body.
   *
   * @param status the response status
   * @throws IOException if an error occurs
   */
  public void sendError(final HttpStatus status) throws IOException {
    sendError(status, null);
  }

  /**
   * Sends an error response with the given status and detailed message.
   * An HTML body is created containing the status and its description,
   * as well as the message, which is escaped using the
   * {@link Utils#escapeHTML escape} method.
   *
   * @param status the response status
   * @param text the text body (sent as text/html), can be {@code null}
   * @throws IOException if an error occurs
   */
  public void sendError(final HttpStatus status, final String text) throws IOException {
    final StringBuilder builder = new StringBuilder(100);
    builder.append("<!DOCTYPE html>\n<html>\n<head><title>")
            .append(status.value()).append(" ").append(status.getReasonPhrase())
            .append("</title></head>")
            .append("<body><h1>")
            .append(status.value()).append(" ").append(status.getReasonPhrase())
            .append("</h1>\n<p>");

    if (StringUtils.isNotEmpty(text)) {
      builder.append(escapeHTML(text));
    }

    builder.append("</p>\n</body></html>");
    send(status, builder.toString());
  }

  /**
   * Sends a 301 or 302 response, redirecting the client to the given URL.
   *
   * @param url the absolute URL to which the client is redirected
   * @param permanent specifies whether a permanent (301) or
   * temporary (302) redirect status is sent
   * @throws IOException if an IO error occurs or url is malformed
   */
  public void redirect(String url, final boolean permanent) throws IOException {
    try {
      url = new URI(url).toASCIIString();
    }
    catch (URISyntaxException e) {
      throw new IOException("malformed URL: " + url);
    }
    headers.add(HttpHeaders.LOCATION, url);
    // some user-agents expect a body, so we send it
    if (permanent)
      send(HttpStatus.PERMANENT_REDIRECT, "Permanently moved to " + url);
    else
      send(HttpStatus.TEMPORARY_REDIRECT, "Temporarily moved to " + url);
  }

  //

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
   * @throws IOException if an error occurs
   */
  public OutputStream getBody() throws IOException {
    if (encoders[0] != null || discardBody)
      return encoders[0]; // return the existing stream (or null)
    // set up chain of encoding streams according to headers

    List<String> te = Arrays.asList(splitElements(headers.getFirst("Transfer-Encoding"), true));
    List<String> ce = Arrays.asList(splitElements(headers.getFirst("Content-Encoding"), true));

    int i = encoders.length - 1;
    encoders[i] = new FilterOutputStream(out) {
      @Override
      public void close() { } // keep underlying connection stream open for now

      @Override // override the very inefficient default implementation
      public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
      }
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
   * @throws IOException if an error occurs
   */
  public void close() throws IOException {
    final OutputStream encoder = encoders[0];
    if (encoder != null) {
      encoder.close(); // close all chained streams (except the underlying one)
    }
    out.flush(); // always flush underlying stream (even if getBody was never called)
  }

  public boolean committed() {
    return committed;
  }

  /**
   * reset status, headers, commit-status
   */
  public void reset() {
    committed = false;
    status = HttpStatus.OK;
    headers.clear();
  }

  public void writeBytes(final byte[] responseBody) throws IOException {
    write(headers, new ResponseOutputBuffer(responseBody));
  }

  public void write(final ResponseOutputBuffer responseBody) throws IOException {
    write(headers, responseBody);
  }

  public void write(final HttpHeaders headers, final ResponseOutputBuffer responseBody) throws IOException {
    write(status, headers, responseBody);
  }

  /**
   * Sends the response headers, including the given response status
   * and description, and all response headers. If they do not already
   * exist, the following headers are added as necessary:
   * Content-Range, Content-Type, Transfer-Encoding, Content-Encoding,
   * Content-Length, Last-Modified, ETag, Connection  and Date. Ranges are
   * properly calculated as well, with a 200 status changed to a 206 status.
   *
   * After receiving and interpreting a request message, a server responds with an HTTP response message.
   *
   * <pre>
   *        Response      = Status-Line               ; Section 6.1
   *                        *(( general-header        ; Section 4.5
   *                         | response-header        ; Section 6.2
   *                         | entity-header ) CRLF)  ; Section 7.1
   *                        CRLF
   *                        [ message-body ]          ; Section 7.2
   * </pre>
   *
   * @param status for Status-Line
   * @param headers for response-header
   * @param responseBody for message-body
   * @throws IOException if an I/O error occurs.
   * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html">HTTP/1.1: Response</a>
   */
  public final void write(HttpStatus status, HttpHeaders headers, ResponseOutputBuffer responseBody) throws IOException {
    committed = true;
    final OutputStream output = this.out;
    // write status line
    writeStatusLine(output, status);
    output.write(CRLF);

    prepareHttpHeaders(headers, responseBody);

    // write headers
    writeHttpHeaders(headers, output);
    output.write(CRLF);
    // write response body
    if (responseBody != null) {
      OutputStream out = getBody();
      if (out != null) {
        responseBody.writeTo(out);
      }
    }
  }

  protected void prepareHttpHeaders(HttpHeaders headers, ResponseOutputBuffer responseBody) {
    if (!headers.containsKey(HttpHeaders.DATE)) {
//      final String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(LocalDateTime.now());
//      headers.add(Constant.DATE, date);
      headers.setDate(System.currentTimeMillis()); // todo
    }
    if (serverHeader != null) {
      headers.add(HttpHeaders.SERVER, serverHeader);
    }
    if (!headers.containsKey(HttpHeaders.VARY)) {
      // RFC7231#7.1.4: Vary field should include headers
      headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING); // that are used in selecting representation
    }
    if (responseBody == null) {
      headers.setContentLength(0);
    }
    else {
      headers.setContentLength(responseBody.size());
    }
  }

  /**
   * The first line of a Response message is the Status-Line,
   * consisting of the protocol version followed by a numeric
   * status code and its associated textual phrase, with each
   * element separated by SP characters. No CR or LF is allowed
   * except in the final CRLF sequence.
   *
   * <p>
   * Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
   * </p>
   *
   * @throws IOException if an I/O error occurs.
   * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html">HTTP/1.1: Response</a>
   */
  static void writeStatusLine(final OutputStream output, final HttpStatus status) throws IOException {
    final Charset charset = StandardCharsets.ISO_8859_1;
    // status line
    output.write(VERSION_BYTES);
    output.write(Integer.toString(status.value()).getBytes(charset));
    output.write(BLANK_SPACE);
    output.write(status.getReasonPhrase().getBytes(charset));
  }

  /**
   * The response-header fields allow the server to pass additional
   * information about the response which cannot be placed in the S
   * tatus- Line. These header fields give information about the
   * server and about further access to the resource identified by
   * the Request-URI.
   * <pre>
   *        response-header = Accept-Ranges           ; Section 14.5
   *                        | Age                     ; Section 14.6
   *                        | ETag                    ; Section 14.19
   *                        | Location                ; Section 14.30
   *                        | Proxy-Authenticate      ; Section 14.33
   *                        | Retry-After             ; Section 14.37
   *                        | Server                  ; Section 14.38
   *                        | Vary                    ; Section 14.44
   *                        | WWW-Authenticate        ; Section 14.47
   * </pre>
   * Response-header field names can be extended reliably only in
   * combination with a change in the protocol version. However,
   * new or experimental header fields MAY be given the semantics
   * of response- header fields if all parties in the communication
   * recognize them to be response-header fields. Unrecognized header
   * fields are treated as entity-header fields.
   *
   * @throws IOException if an I/O error occurs.
   * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html">HTTP/1.1: Response</a>
   */
  static void writeHttpHeaders(HttpHeaders headers, final OutputStream output) throws IOException {
    final Charset charset = StandardCharsets.ISO_8859_1; // todo 编码
    // response headers
    for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
      final String name = entry.getKey();
      final byte[] nameBytes = name.getBytes(charset);
      for (final String value : entry.getValue()) {
        output.write(nameBytes);
        output.write(COLON); // ': '
        output.write(value.getBytes(charset));
        output.write(HttpResponse.CRLF);
      }
    }
  }

}
