/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.mock.api.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Enumeration;

import cn.taketoday.mock.api.GenericMock;
import cn.taketoday.mock.api.MockApi;
import cn.taketoday.mock.api.MockConfig;
import cn.taketoday.mock.api.MockOutputStream;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.mock.api.MockException;
import cn.taketoday.mock.api.WriteListener;

/**
 * Provides an abstract class to be subclassed to create an HTTP servlet suitable for a Web site. A subclass of
 * <code>HttpServlet</code> must override at least one method, usually one of these:
 *
 * <ul>
 * <li><code>doGet</code>, if the servlet supports HTTP GET requests
 * <li><code>doPost</code>, for HTTP POST requests
 * <li><code>doPut</code>, for HTTP PUT requests
 * <li><code>doDelete</code>, for HTTP DELETE requests
 * <li><code>init</code> and <code>destroy</code>, to manage resources that are held for the life of the servlet
 * <li><code>getServletInfo</code>, which the servlet uses to provide information about itself
 * </ul>
 *
 * <p>
 * There's almost no reason to override the <code>service</code> method. <code>service</code> handles standard HTTP
 * requests by dispatching them to the handler methods for each HTTP request type (the <code>do</code><i>XXX</i> methods
 * listed above).
 *
 * <p>
 * Likewise, there's almost no reason to override the <code>doOptions</code> and <code>doTrace</code> methods.
 *
 * <p>
 * Servlets typically run on multithreaded servers, so be aware that a servlet must handle concurrent requests and be
 * careful to synchronize access to shared resources. Shared resources include in-memory data such as instance or class
 * variables and external objects such as files, database connections, and network connections. See the
 * <a href="https://docs.oracle.com/javase/tutorial/essential/concurrency/"> Java Tutorial on Multithreaded
 * Programming</a> for more information on handling multiple threads in a Java program.
 *
 * @author Various
 */
public abstract class HttpMock extends GenericMock {
  private static final long serialVersionUID = 8466325577512134784L;

  private static final String METHOD_DELETE = "DELETE";
  private static final String METHOD_HEAD = "HEAD";
  private static final String METHOD_GET = "GET";
  private static final String METHOD_OPTIONS = "OPTIONS";
  private static final String METHOD_POST = "POST";
  private static final String METHOD_PUT = "PUT";
  private static final String METHOD_TRACE = "TRACE";

  private static final String HEADER_IFMODSINCE = "If-Modified-Since";
  private static final String HEADER_LASTMOD = "Last-Modified";

  /**
   * The parameter obtained {@link MockConfig#getInitParameter(String)} to determine if legacy processing of
   * {@link #doHead(HttpMockRequest, HttpMockResponse)} is provided.
   *
   * @since Servlet 6.0
   * @deprecated may be removed in future releases
   */
  @Deprecated(forRemoval = true, since = "Servlet 6.0")
  public static final String LEGACY_DO_HEAD = "cn.taketoday.mock.api.http.legacyDoHead";

  private boolean legacyHeadHandling;

  /**
   * Does nothing, because this is an abstract class.
   */

  public HttpMock() {
  }

  @Override
  public void init(MockConfig config) throws MockException {
    super.init(config);
    legacyHeadHandling = Boolean.parseBoolean(config.getInitParameter(LEGACY_DO_HEAD));
  }

  /**
   * Called by the server (via the <code>service</code> method) to allow a servlet to handle a GET request.
   *
   * <p>
   * Overriding this method to support a GET request also automatically supports an HTTP HEAD request. A HEAD request is a
   * GET request that returns no body in the response, only the request header fields.
   *
   * <p>
   * When overriding this method, read the request data, write the response headers, get the response's writer or output
   * stream object, and finally, write the response data. It's best to include content type and encoding. When using a
   * <code>PrintWriter</code> object to return the response, set the content type before accessing the
   * <code>PrintWriter</code> object.
   *
   * <p>
   * The servlet container must write the headers before committing the response, because in HTTP the headers must be sent
   * before the response body.
   *
   * <p>
   * Where possible, set the Content-Length header (with the {@link MockResponse#setContentLength}
   * method), to allow the servlet container to use a persistent connection to return its response to the client,
   * improving performance. The content length is automatically set if the entire response fits inside the response
   * buffer.
   *
   * <p>
   * When using HTTP 1.1 chunked encoding (which means that the response has a Transfer-Encoding header), do not set the
   * Content-Length header.
   *
   * <p>
   * The GET method should be safe, that is, without any side effects for which users are held responsible. For example,
   * most form queries have no side effects. If a client request is intended to change stored data, the request should use
   * some other HTTP method.
   *
   * <p>
   * The GET method should also be idempotent, meaning that it can be safely repeated. Sometimes making a method safe also
   * makes it idempotent. For example, repeating queries is both safe and idempotent, but buying a product online or
   * modifying data is neither safe nor idempotent.
   *
   * <p>
   * If the request is incorrectly formatted, <code>doGet</code> returns an HTTP "Bad Request" message.
   *
   * @param req an {@link HttpMockRequest} object that contains the request the client has made of the servlet
   * @param resp an {@link HttpMockResponse} object that contains the response the servlet sends to the client
   * @throws IOException if an input or output error is detected when the servlet handles the GET request
   * @throws MockException if the request for the GET could not be handled
   * @see MockResponse#setContentType
   */
  protected void doGet(HttpMockRequest req, HttpMockResponse resp) throws MockException, IOException {
    String protocol = req.getProtocol();
    resp.sendError(getMethodNotSupportedCode(protocol),
            "HTTP method GET is not supported by this URL");
  }

  /**
   * Returns the time the <code>HttpServletRequest</code> object was last modified, in milliseconds since midnight January
   * 1, 1970 GMT. If the time is unknown, this method returns a negative number (the default).
   *
   * <p>
   * Servlets that support HTTP GET requests and can quickly determine their last modification time should override this
   * method. This makes browser and proxy caches work more effectively, reducing the load on server and network resources.
   *
   * @param req the <code>HttpServletRequest</code> object that is sent to the servlet
   * @return a <code>long</code> integer specifying the time the <code>HttpServletRequest</code> object was last modified,
   * in milliseconds since midnight, January 1, 1970 GMT, or -1 if the time is not known
   */
  protected long getLastModified(HttpMockRequest req) {
    return -1;
  }

  /**
   * <p>
   * Receives an HTTP HEAD request from the protected <code>service</code> method and handles the request. The client
   * sends a HEAD request when it wants to see only the headers of a response, such as Content-Type or Content-Length. The
   * HTTP HEAD method counts the output bytes in the response to set the Content-Length header accurately.
   *
   * <p>
   * If you override this method, you can avoid computing the response body and just set the response headers directly to
   * improve performance. Make sure that the <code>doHead</code> method you write is both safe and idempotent (that is,
   * protects itself from being called multiple times for one HTTP HEAD request).
   *
   * <p>
   * The default implementation calls {@link #doGet(HttpMockRequest, HttpMockResponse)}. If the
   * {@link MockConfig} init parameter {@link #LEGACY_DO_HEAD} is set to "TRUE", then the response instance is wrapped
   * so that the response body is discarded.
   *
   * <p>
   * If the HTTP HEAD request is incorrectly formatted, <code>doHead</code> returns an HTTP "Bad Request" message.
   *
   * @param req the request object that is passed to the servlet
   * @param resp the response object that the servlet uses to return the headers to the clien
   * @throws IOException if an input or output error occurs
   * @throws MockException if the request for the HEAD could not be handled
   */
  protected void doHead(HttpMockRequest req, HttpMockResponse resp) throws MockException, IOException {
    if (legacyHeadHandling) {
      NoBodyResponse response = new NoBodyResponse(resp);
      doGet(req, response);
      response.setContentLength();
    }
    else {
      doGet(req, resp);
    }
  }

  /**
   * Called by the server (via the <code>service</code> method) to allow a servlet to handle a POST request.
   *
   * The HTTP POST method allows the client to send data of unlimited length to the Web server a single time and is useful
   * when posting information such as credit card numbers.
   *
   * <p>
   * When overriding this method, read the request data, write the response headers, get the response's writer or output
   * stream object, and finally, write the response data. It's best to include content type and encoding. When using a
   * <code>PrintWriter</code> object to return the response, set the content type before accessing the
   * <code>PrintWriter</code> object.
   *
   * <p>
   * The servlet container must write the headers before committing the response, because in HTTP the headers must be sent
   * before the response body.
   *
   * <p>
   * Where possible, set the Content-Length header (with the {@link MockResponse#setContentLength}
   * method), to allow the servlet container to use a persistent connection to return its response to the client,
   * improving performance. The content length is automatically set if the entire response fits inside the response
   * buffer.
   *
   * <p>
   * When using HTTP 1.1 chunked encoding (which means that the response has a Transfer-Encoding header), do not set the
   * Content-Length header.
   *
   * <p>
   * This method does not need to be either safe or idempotent. Operations requested through POST can have side effects
   * for which the user can be held accountable, for example, updating stored data or buying items online.
   *
   * <p>
   * If the HTTP POST request is incorrectly formatted, <code>doPost</code> returns an HTTP "Bad Request" message.
   *
   * @param req an {@link HttpMockRequest} object that contains the request the client has made of the servlet
   * @param resp an {@link HttpMockResponse} object that contains the response the servlet sends to the client
   * @throws IOException if an input or output error is detected when the servlet handles the request
   * @throws MockException if the request for the POST could not be handled
   * @see MockOutputStream
   * @see MockResponse#setContentType
   */
  protected void doPost(HttpMockRequest req, HttpMockResponse resp) throws MockException, IOException {
    String protocol = req.getProtocol();
    resp.sendError(getMethodNotSupportedCode(protocol), "HTTP method POST is not supported by this URL");
  }

  /**
   * Called by the server (via the <code>service</code> method) to allow a servlet to handle a PUT request.
   *
   * The PUT operation allows a client to place a file on the server and is similar to sending a file by FTP.
   *
   * <p>
   * When overriding this method, leave intact any content headers sent with the request (including Content-Length,
   * Content-Type, Content-Transfer-Encoding, Content-Encoding, Content-Base, Content-Language, Content-Location,
   * Content-MD5, and Content-Range). If your method cannot handle a content header, it must issue an error message (HTTP
   * 501 - Not Implemented) and discard the request. For more information on HTTP 1.1 and the PUT method, see RFC 7231
   * <a href="http://www.ietf.org/rfc/rfc7231.txt"></a>.
   *
   * <p>
   * This method does not need to be either safe or idempotent. Operations that <code>doPut</code> performs can have side
   * effects for which the user can be held accountable. When using this method, it may be useful to save a copy of the
   * affected URL in temporary storage.
   *
   * <p>
   * If the HTTP PUT request is incorrectly formatted, <code>doPut</code> returns an HTTP "Bad Request" message.
   *
   * @param req the {@link HttpMockRequest} object that contains the request the client made of the servlet
   * @param resp the {@link HttpMockResponse} object that contains the response the servlet returns to the client
   * @throws IOException if an input or output error occurs while the servlet is handling the PUT request
   * @throws MockException if the request for the PUT cannot be handled
   */
  protected void doPut(HttpMockRequest req, HttpMockResponse resp) throws MockException, IOException {
    String protocol = req.getProtocol();
    resp.sendError(getMethodNotSupportedCode(protocol), "HTTP method PUT is not supported by this URL");
  }

  /**
   * Called by the server (via the <code>service</code> method) to allow a servlet to handle a DELETE request.
   *
   * The DELETE operation allows a client to remove a document or Web page from the server.
   *
   * <p>
   * This method does not need to be either safe or idempotent. Operations requested through DELETE can have side effects
   * for which users can be held accountable. When using this method, it may be useful to save a copy of the affected URL
   * in temporary storage.
   *
   * <p>
   * If the HTTP DELETE request is incorrectly formatted, <code>doDelete</code> returns an HTTP "Bad Request" message.
   *
   * @param req the {@link HttpMockRequest} object that contains the request the client made of the servlet
   * @param resp the {@link HttpMockResponse} object that contains the response the servlet returns to the client
   * @throws IOException if an input or output error occurs while the servlet is handling the DELETE request
   * @throws MockException if the request for the DELETE cannot be handled
   */
  protected void doDelete(HttpMockRequest req, HttpMockResponse resp) throws MockException, IOException {
    String protocol = req.getProtocol();
    resp.sendError(getMethodNotSupportedCode(protocol), "Http method DELETE is not supported by this URL");
  }

  private int getMethodNotSupportedCode(String protocol) {
    return switch (protocol) {
      case "HTTP/0.9", "HTTP/1.0" -> HttpMockResponse.SC_BAD_REQUEST;
      default -> HttpMockResponse.SC_METHOD_NOT_ALLOWED;
    };
  }

  private Method[] getAllDeclaredMethods(Class<? extends HttpMock> c) {

    Class<?> clazz = c;
    Method[] allMethods = null;

    while (!clazz.equals(HttpMock.class)) {
      Method[] thisMethods = clazz.getDeclaredMethods();
      if (allMethods != null && allMethods.length > 0) {
        Method[] subClassMethods = allMethods;
        allMethods = new Method[thisMethods.length + subClassMethods.length];
        System.arraycopy(thisMethods, 0, allMethods, 0, thisMethods.length);
        System.arraycopy(subClassMethods, 0, allMethods, thisMethods.length, subClassMethods.length);
      }
      else {
        allMethods = thisMethods;
      }

      clazz = clazz.getSuperclass();
    }

    return ((allMethods != null) ? allMethods : new Method[0]);
  }

  /**
   * Called by the server (via the <code>service</code> method) to allow a servlet to handle a OPTIONS request.
   *
   * The OPTIONS request determines which HTTP methods the server supports and returns an appropriate header. For example,
   * if a servlet overrides <code>doGet</code>, this method returns the following header:
   *
   * <p>
   * <code>Allow: GET, HEAD, TRACE, OPTIONS</code>
   *
   * <p>
   * There's no need to override this method unless the servlet implements new HTTP methods, beyond those implemented by
   * HTTP 1.1.
   *
   * @param req the {@link HttpMockRequest} object that contains the request the client made of the servlet
   * @param resp the {@link HttpMockResponse} object that contains the response the servlet returns to the client
   * @throws IOException if an input or output error occurs while the servlet is handling the OPTIONS request
   * @throws MockException if the request for the OPTIONS cannot be handled
   */
  protected void doOptions(HttpMockRequest req, HttpMockResponse resp) throws MockException, IOException {
    Method[] methods = getAllDeclaredMethods(this.getClass());

    boolean ALLOW_GET = false;
    boolean ALLOW_HEAD = false;
    boolean ALLOW_POST = false;
    boolean ALLOW_PUT = false;
    boolean ALLOW_DELETE = false;
    boolean ALLOW_TRACE = true;
    boolean ALLOW_OPTIONS = true;

    for (int i = 0; i < methods.length; i++) {
      String methodName = methods[i].getName();

      if (methodName.equals("doGet")) {
        ALLOW_GET = true;
        ALLOW_HEAD = true;
      }
      else if (methodName.equals("doPost")) {
        ALLOW_POST = true;
      }
      else if (methodName.equals("doPut")) {
        ALLOW_PUT = true;
      }
      else if (methodName.equals("doDelete")) {
        ALLOW_DELETE = true;
      }

    }

    // we know "allow" is not null as ALLOW_OPTIONS = true
    // when this method is invoked
    StringBuilder allow = new StringBuilder();
    if (ALLOW_GET) {
      allow.append(METHOD_GET);
    }
    if (ALLOW_HEAD) {
      if (allow.length() > 0) {
        allow.append(", ");
      }
      allow.append(METHOD_HEAD);
    }
    if (ALLOW_POST) {
      if (allow.length() > 0) {
        allow.append(", ");
      }
      allow.append(METHOD_POST);
    }
    if (ALLOW_PUT) {
      if (allow.length() > 0) {
        allow.append(", ");
      }
      allow.append(METHOD_PUT);
    }
    if (ALLOW_DELETE) {
      if (allow.length() > 0) {
        allow.append(", ");
      }
      allow.append(METHOD_DELETE);
    }
    if (ALLOW_TRACE) {
      if (allow.length() > 0) {
        allow.append(", ");
      }
      allow.append(METHOD_TRACE);
    }
    if (ALLOW_OPTIONS) {
      if (allow.length() > 0) {
        allow.append(", ");
      }
      allow.append(METHOD_OPTIONS);
    }

    resp.setHeader("Allow", allow.toString());
  }

  /**
   * Called by the server (via the <code>service</code> method) to allow a servlet to handle a TRACE request.
   *
   * A TRACE returns the headers sent with the TRACE request to the client, so that they can be used in debugging. There's
   * no need to override this method.
   *
   * @param req the {@link HttpMockRequest} object that contains the request the client made of the servlet
   * @param resp the {@link HttpMockResponse} object that contains the response the servlet returns to the client
   * @throws IOException if an input or output error occurs while the servlet is handling the TRACE request
   * @throws MockException if the request for the TRACE cannot be handled
   */
  protected void doTrace(HttpMockRequest req, HttpMockResponse resp) throws MockException, IOException {

    int responseLength;

    String CRLF = "\r\n";
    StringBuilder buffer = new StringBuilder("TRACE ").append(req.getRequestURI()).append(" ")
            .append(req.getProtocol());

    Enumeration<String> reqHeaderEnum = req.getHeaderNames();

    while (reqHeaderEnum.hasMoreElements()) {
      String headerName = reqHeaderEnum.nextElement();
      buffer.append(CRLF).append(headerName).append(": ").append(req.getHeader(headerName));
    }

    buffer.append(CRLF);

    responseLength = buffer.length();

    resp.setContentType("message/http");
    resp.setContentLength(responseLength);
    MockOutputStream out = resp.getOutputStream();
    out.print(buffer.toString());
  }

  /**
   * Receives standard HTTP requests from the public <code>service</code> method and dispatches them to the
   * <code>do</code><i>XXX</i> methods defined in this class. This method is an HTTP-specific version of the
   * {@link MockApi#service} method. There's no need to override this method.
   *
   * @param req the {@link HttpMockRequest} object that contains the request the client made of the servlet
   * @param resp the {@link HttpMockResponse} object that contains the response the servlet returns to the client
   * @throws IOException if an input or output error occurs while the servlet is handling the HTTP request
   * @throws MockException if the HTTP request cannot be handled
   * @see MockApi#service
   */
  protected void service(HttpMockRequest req, HttpMockResponse resp) throws MockException, IOException {
    String method = req.getMethod();

    switch (method) {
      case METHOD_GET -> {
        long lastModified = getLastModified(req);
        if (lastModified == -1) {
          // servlet doesn't support if-modified-since, no reason
          // to go through further expensive logic
          doGet(req, resp);
        }
        else {
          long ifModifiedSince = req.getDateHeader(HEADER_IFMODSINCE);
          if (ifModifiedSince < lastModified) {
            // If the servlet mod time is later, call doGet()
            // Round down to the nearest second for a proper compare
            // A ifModifiedSince of -1 will always be less
            maybeSetLastModified(resp, lastModified);
            doGet(req, resp);
          }
          else {
            resp.setStatus(HttpMockResponse.SC_NOT_MODIFIED);
          }
        }

      }
      case METHOD_HEAD -> {
        long lastModified = getLastModified(req);
        maybeSetLastModified(resp, lastModified);
        doHead(req, resp);

      }
      case METHOD_POST -> doPost(req, resp);
      case METHOD_PUT -> doPut(req, resp);
      case METHOD_DELETE -> doDelete(req, resp);
      case METHOD_OPTIONS -> doOptions(req, resp);
      case METHOD_TRACE -> doTrace(req, resp);
      //
      // Note that this means NO servlet supports whatever
      // method was requested, anywhere on this server.
      //
      default -> resp.sendError(HttpMockResponse.SC_NOT_IMPLEMENTED, "Method %s is not defined in RFC 2068 and is not supported"
              .formatted(method));
    }
  }

  /*
   * Sets the Last-Modified entity header field, if it has not already been set and if the value is meaningful. Called
   * before doGet, to ensure that headers are set before response data is written. A subclass might have set this header
   * already, so we check.
   */
  private void maybeSetLastModified(HttpMockResponse resp, long lastModified) {
    if (resp.containsHeader(HEADER_LASTMOD))
      return;
    if (lastModified >= 0)
      resp.setDateHeader(HEADER_LASTMOD, lastModified);
  }

  /**
   * Dispatches client requests to the protected <code>service</code> method. There's no need to override this method.
   *
   * @param req the {@link HttpMockRequest} object that contains the request the client made of the servlet
   * @param res the {@link HttpMockResponse} object that contains the response the servlet returns to the client
   * @throws IOException if an input or output error occurs while the servlet is handling the HTTP request
   * @throws MockException if the HTTP request cannot be handled or if either parameter is not an instance of its
   * respective {@link HttpMockRequest} or {@link HttpMockResponse} counterparts.
   * @see MockApi#service
   */
  @Override
  public void service(MockRequest req, MockResponse res) throws MockException, IOException {
    HttpMockRequest request;
    HttpMockResponse response;

    if (!(req instanceof HttpMockRequest && res instanceof HttpMockResponse)) {
      throw new MockException("non-HTTP request or response");
    }

    request = (HttpMockRequest) req;
    response = (HttpMockResponse) res;

    service(request, response);
  }
}

/*
 * A response that includes no body, for use in (dumb) "HEAD" support. This just swallows that body, counting the bytes
 * in order to set the content length appropriately. All other methods delegate directly to the wrapped HTTP Servlet
 * Response object.
 */
// file private
//@Deprecated(forRemoval = true, since = "Servlet 6.0")
class NoBodyResponse extends HttpMockResponseWrapper {

  private NoBodyOutputStream noBody;
  private PrintWriter writer;
  private boolean didSetContentLength;
  private boolean usingOutputStream;

  // file private
  NoBodyResponse(HttpMockResponse r) {
    super(r);
    noBody = new NoBodyOutputStream();
  }

  // file private
  void setContentLength() {
    if (!didSetContentLength) {
      if (writer != null) {
        writer.flush();
      }
      setContentLength(noBody.getContentLength());
    }
  }

  @Override
  public void setContentLength(int len) {
    super.setContentLength(len);
    didSetContentLength = true;
  }

  @Override
  public void setContentLengthLong(long len) {
    super.setContentLengthLong(len);
    didSetContentLength = true;
  }

  @Override
  public void setHeader(String name, String value) {
    super.setHeader(name, value);
    checkHeader(name);
  }

  @Override
  public void addHeader(String name, String value) {
    super.addHeader(name, value);
    checkHeader(name);
  }

  @Override
  public void setIntHeader(String name, int value) {
    super.setIntHeader(name, value);
    checkHeader(name);
  }

  @Override
  public void addIntHeader(String name, int value) {
    super.addIntHeader(name, value);
    checkHeader(name);
  }

  private void checkHeader(String name) {
    if ("content-length".equalsIgnoreCase(name)) {
      didSetContentLength = true;
    }
  }

  @Override
  public void reset() {
    super.reset();
    noBody.reset();
    usingOutputStream = false;
    writer = null;
    didSetContentLength = false;
  }

  @Override
  public void resetBuffer() {
    super.resetBuffer();
    if (writer != null) {
      try {
        NoBodyOutputStream.disableFlush.set(Boolean.TRUE);
        writer.flush();
      }
      finally {
        NoBodyOutputStream.disableFlush.remove();
      }
    }
    noBody.reset();
  }

  @Override
  public MockOutputStream getOutputStream() throws IOException {

    if (writer != null) {
      throw new IllegalStateException("Illegal to call getOutputStream() after getWriter() has been called");
    }
    usingOutputStream = true;

    return noBody;
  }

  @Override
  public PrintWriter getWriter() throws UnsupportedEncodingException {

    if (usingOutputStream) {
      throw new IllegalStateException("Illegal to call getWriter() after getOutputStream() has been called");
    }

    if (writer == null) {
      OutputStreamWriter w = new OutputStreamWriter(noBody, getCharacterEncoding());
      writer = new PrintWriter(w);
    }

    return writer;
  }
}

/*
 * output stream that gobbles up all its data.
 */
// file private
class NoBodyOutputStream extends MockOutputStream {

  static ThreadLocal<Boolean> disableFlush = new ThreadLocal<>();

  private int contentLength = 0;

  // file private
  NoBodyOutputStream() {
  }

  void reset() {
    contentLength = 0;
  }

  // file private
  int getContentLength() {
    return contentLength;
  }

  @Override
  public void write(int b) {
    contentLength++;
  }

  @Override
  public void write(byte buf[], int offset, int len) throws IOException {
    if (buf == null) {
      throw new NullPointerException("Null passed for byte array in write method");
    }

    if (offset < 0 || len < 0 || offset + len > buf.length) {
      throw new IndexOutOfBoundsException("Invalid offset [%s] and / or length [%s] specified for array of size [%s]"
              .formatted(offset, len, buf.length));
    }

    contentLength += len;
  }

  @Override
  public void flush() throws IOException {
    if (Boolean.TRUE.equals(disableFlush.get()))
      super.flush();
  }

  @Override
  public boolean isReady() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setWriteListener(WriteListener writeListener) {
    throw new UnsupportedOperationException();
  }
}
