/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.test.web.mock;

import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import infra.context.ApplicationContext;
import infra.http.DefaultHttpHeaders;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRange;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.session.Session;
import infra.session.SessionManager;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import infra.web.AbstractHttpContext;
import infra.web.DispatcherHandler;
import infra.web.HttpContext;
import infra.web.async.AsyncWebRequest;
import infra.web.mock.MockSession;
import infra.web.mock.api.DispatcherType;
import infra.web.multipart.MultipartRequest;
import infra.web.multipart.Part;
import infra.test.web.mock.request.HttpContextRequestPostProcessor;
import infra.web.util.UriComponents;
import infra.web.util.UriComponentsBuilder;

/**
 * A self-contained {@link HttpContext} implementation for testing purposes.
 *
 * <p>Unlike {@link infra.web.mock.MockHttpContext}, this context does not rely
 * on separate request/response mock objects: it holds all request and response
 * data directly. Obtain a builder via the static {@code get}/{@code post}/{@code put}...
 * factory methods, e.g. {@code TestingHttpContext.post("/api").build()}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@SuppressWarnings("NullAway")
public class TestingHttpContext extends AbstractHttpContext {

  private static final String HTTP = "http";

  private static final String HTTPS = "https";

  // ---------------------------------------------------------------------
  // Request data
  // ---------------------------------------------------------------------

  private String method = "GET";

  private String scheme = HTTP;

  private String serverName = "localhost";

  private int serverPort = 80;

  private String remoteAddr = "127.0.0.1";

  private String remoteHost = "localhost";

  private int remotePort;

  private String localName;

  private String localAddr = "127.0.0.1";

  private int localPort;

  private String protocol = "HTTP/1.1";

  private boolean secure;

  private @Nullable String characterEncoding;

  private @Nullable String contentType;

  private byte @Nullable [] content;

  private final List<Locale> locales = new LinkedList<>();

  private @Nullable Session session;

  private @Nullable String authType;

  private @Nullable Principal userPrincipal;

  private @Nullable String requestedSessionId;

  private boolean requestedSessionIdValid = true;

  private boolean requestedSessionIdFromCookie = true;

  private boolean requestedSessionIdFromURL;

  private @Nullable String uriTemplate;

  private DispatcherType dispatcherType = DispatcherType.REQUEST;

  private boolean asyncStarted;

  private boolean asyncSupported;

  private final MultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();

  // ---------------------------------------------------------------------
  // Response data
  // ---------------------------------------------------------------------

  private int status = 200;

  private boolean committed;

  private final ByteArrayOutputStream responseContent = new ByteArrayOutputStream();

  private @Nullable String forwardedUrl;

  private final long requestTimeMillis = System.currentTimeMillis();

  public TestingHttpContext() {
    this(null);
  }

  public TestingHttpContext(@Nullable ApplicationContext context) {
    this(context, null);
  }

  public TestingHttpContext(@Nullable ApplicationContext context, @Nullable DispatcherHandler dispatcherHandler) {
    super(context, dispatcherHandler);
    this.locales.add(Locale.ENGLISH);
    this.requestURI = "";
    this.requestHeaders = new DefaultHttpHeaders();
    this.parameters = new LinkedMultiValueMap<>();
  }

  /**
   * Return the {@link DispatcherHandler} that this context runs in, if any.
   */
  public @Nullable DispatcherHandler getDispatcherHandler() {
    return dispatcherHandler;
  }

  // ---------------------------------------------------------------------
  // Static factory methods
  // ---------------------------------------------------------------------

  /**
   * Create a builder for an HTTP GET request with the given URI template.
   * The URI may contain a query string, or parameters may be added later via
   * {@link BaseBuilder#queryParam}.
   *
   * @param url the URI template
   * @param uriVars zero or more URI variables to expand the template
   * @return the created builder
   */
  public static BaseBuilder<?> get(String url, Object... uriVars) {
    return method(HttpMethod.GET, url, uriVars);
  }

  /**
   * Create a builder for an HTTP HEAD request.
   *
   * @see #get(String, Object...)
   */
  public static BaseBuilder<?> head(String url, Object... uriVars) {
    return method(HttpMethod.HEAD, url, uriVars);
  }

  /**
   * Create a builder for an HTTP POST request.
   *
   * @see #get(String, Object...)
   */
  public static BodyBuilder post(String url, Object... uriVars) {
    return method(HttpMethod.POST, url, uriVars);
  }

  /**
   * Create a builder for an HTTP PUT request.
   *
   * @see #get(String, Object...)
   */
  public static BodyBuilder put(String url, Object... uriVars) {
    return method(HttpMethod.PUT, url, uriVars);
  }

  /**
   * Create a builder for an HTTP PATCH request.
   *
   * @see #get(String, Object...)
   */
  public static BodyBuilder patch(String url, Object... uriVars) {
    return method(HttpMethod.PATCH, url, uriVars);
  }

  /**
   * Create a builder for an HTTP DELETE request.
   *
   * @see #get(String, Object...)
   */
  public static BaseBuilder<?> delete(String url, Object... uriVars) {
    return method(HttpMethod.DELETE, url, uriVars);
  }

  /**
   * Create a builder for an HTTP OPTIONS request.
   *
   * @see #get(String, Object...)
   */
  public static BaseBuilder<?> options(String url, Object... uriVars) {
    return method(HttpMethod.OPTIONS, url, uriVars);
  }

  /**
   * Create a builder with the given HTTP method and URI template.
   *
   * @param method the HTTP method (GET, POST, etc)
   * @param url the URI template
   * @param uriVars zero or more URI variables to expand the template
   * @return the created builder
   */
  public static BodyBuilder method(HttpMethod method, String url, Object... uriVars) {
    Assert.notNull(method, "HTTP method is required");
    return new DefaultBodyBuilder(method, url, uriVars);
  }

  // ---------------------------------------------------------------------
  // HttpContext / AbstractHttpContext implementations
  // ---------------------------------------------------------------------

  @Override
  public long getRequestTimeMillis() {
    return requestTimeMillis;
  }

  @Override
  public String getScheme() {
    return scheme;
  }

  @Override
  public boolean isSecure() {
    return secure || HTTPS.equalsIgnoreCase(scheme);
  }

  @Override
  public String getServerName() {
    return serverName;
  }

  @Override
  public int getServerPort() {
    return serverPort;
  }

  @Override
  public int getRemotePort() {
    return remotePort;
  }

  @Override
  public String getRemoteAddress() {
    return remoteAddr;
  }

  @Override
  public InetSocketAddress remoteAddress() {
    return InetSocketAddress.createUnresolved(remoteHost, remotePort);
  }

  @Override
  public SocketAddress localAddress() {
    return InetSocketAddress.createUnresolved(localAddr, localPort);
  }

  @Override
  protected String readMethod() {
    return method;
  }

  @Override
  public String getMethodAsString() {
    return method;
  }

  @Override
  protected String readRequestURI() {
    return requestURI;
  }

  @Override
  protected String readQueryString() {
    return queryString;
  }

  @Override
  protected HttpCookie[] readCookies() {
    return cookies != null ? cookies : EMPTY_COOKIES;
  }

  @Override
  protected MultiValueMap<String, String> readParameters() {
    return parameters;
  }

  @Override
  protected Locale readLocale() {
    return locales.get(0);
  }

  @Override
  protected HttpHeaders createRequestHeaders() {
    return requestHeaders;
  }

  @Override
  public long getContentLength() {
    return content != null ? content.length : -1;
  }

  @Override
  public @Nullable String getContentTypeAsString() {
    return contentType;
  }

  @Override
  protected InputStream createInputStream() throws IOException {
    return content != null ? new ByteArrayInputStream(content) : InputStream.nullInputStream();
  }

  @Override
  protected OutputStream createOutputStream() throws IOException {
    return responseContent;
  }

  @Override
  public BufferedReader createReader() throws IOException {
    InputStream source = content != null ? new ByteArrayInputStream(content) : InputStream.nullInputStream();
    Reader sourceReader = characterEncoding != null
            ? new InputStreamReader(source, characterEncoding)
            : new InputStreamReader(source, StandardCharsets.UTF_8);
    return new BufferedReader(sourceReader);
  }

  @Override
  protected MultipartRequest createMultipartRequest() {
    return new TestingMultipartRequest(parts);
  }

  @Override
  protected AsyncWebRequest createAsyncWebRequest() {
    return new TestingAsyncWebRequest();
  }

  // ---------------------------------------------------------------------
  // Response
  // ---------------------------------------------------------------------

  @Override
  public void setStatus(int sc) {
    this.status = sc;
  }

  @Override
  public int getStatus() {
    return status;
  }

  @Override
  public boolean isCommitted() {
    return committed;
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    setStatus(302);
    setHeader(HttpHeaders.LOCATION, location);
  }

  @Override
  public void forward(String path) throws Exception {
    if (dispatcherHandler != null) {
      super.forward(path);
    }
    setForwardedUrl(path);
  }

  @Override
  public void sendError(int sc) throws IOException {
    setStatus(sc);
    this.committed = true;
  }

  @Override
  public void sendError(int sc, @Nullable String msg) throws IOException {
    setStatus(sc);
    setContentType("text/html");
    if (msg != null) {
      byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
      responseContent.write(bytes);
      setContentLength(bytes.length);
    }
    this.committed = true;
  }

  @Override
  public void reset() {
    super.reset();
    this.status = 200;
    this.committed = false;
    this.forwardedUrl = null;
    this.responseContent.reset();
  }

  @Override
  protected void writeHeaders() {
    if (!committed) {
      onCommitting();
      this.committed = true;
      onCommitted();
    }
  }

  @Override
  public void flush() throws IOException {
    writeHeaders();
    responseContent.flush();
  }

  @Override
  protected void requestCompletedInternal(@Nullable Throwable notHandled) {
    if (notHandled == null) {
      try {
        flush();
      }
      catch (IOException ex) {
        throw new IllegalStateException("Failed to flush response", ex);
      }
    }
  }

  /**
   * Signal that the request has been completed, flushing the response.
   *
   * @see AbstractHttpContext#requestCompleted(Throwable)
   */
  public void requestCompleted() {
    requestCompleted(null);
  }

  // ---------------------------------------------------------------------
  // Session
  // ---------------------------------------------------------------------

  @Override
  protected SessionManager sessionManager() {
    return new TestingSessionManager();
  }

  @Override
  public @Nullable Session getSession(boolean create) {
    if (session instanceof MockSession mockSession && mockSession.isInvalid()) {
      session = null;
    }
    if (session == null && create) {
      session = new MockSession();
    }
    return session;
  }

  @Override
  public Session getSession() {
    return getSession(true);
  }

  // ---------------------------------------------------------------------
  // Request convenience methods (absorbed from MockRequest)
  // ---------------------------------------------------------------------

  public void setMethod(@Nullable String method) {
    this.method = method;
  }

  public void setRequestURI(@Nullable String requestURI) {
    this.requestURI = requestURI;
  }

  public void setQueryString(@Nullable String queryString) {
    this.queryString = queryString;
  }

  public void setUriTemplate(@Nullable String uriTemplate) {
    this.uriTemplate = uriTemplate;
  }

  public @Nullable String getUriTemplate() {
    return uriTemplate;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  public void setServerPort(int serverPort) {
    this.serverPort = serverPort;
  }

  public void setRemoteAddr(String remoteAddr) {
    this.remoteAddr = remoteAddr;
  }

  public String getRemoteAddr() {
    return remoteAddr;
  }

  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public String getRemoteHost() {
    return remoteHost;
  }

  public void setRemotePort(int remotePort) {
    this.remotePort = remotePort;
  }

  public void setLocalName(String localName) {
    this.localName = localName;
  }

  public String getLocalName() {
    return localName;
  }

  public void setLocalAddr(String localAddr) {
    this.localAddr = localAddr;
  }

  public String getLocalAddr() {
    return localAddr;
  }

  public void setLocalPort(int localPort) {
    this.localPort = localPort;
  }

  public int getLocalPort() {
    return localPort;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public void setCharacterEncoding(@Nullable String characterEncoding) {
    this.characterEncoding = characterEncoding;
    updateContentTypeHeader();
  }

  public @Nullable String getCharacterEncoding() {
    return characterEncoding;
  }

  private void updateContentTypeHeader() {
    if (StringUtils.isNotEmpty(contentType)) {
      String value = contentType;
      if (StringUtils.isNotEmpty(characterEncoding)
              && !contentType.toLowerCase(Locale.ROOT).contains("charset=")) {
        value += ";charset=" + characterEncoding;
      }
      requestHeaders().set(HttpHeaders.CONTENT_TYPE, value);
    }
    else {
      requestHeaders().remove(HttpHeaders.CONTENT_TYPE);
    }
  }

  public void setContent(byte @Nullable [] content) {
    this.content = content;
    this.inputStream = null;
    this.reader = null;
  }

  public byte @Nullable [] getContent() {
    return content;
  }

  public @Nullable String getContentAsString() {
    if (content == null) {
      return null;
    }
    return characterEncoding != null
            ? new String(content, Charset.forName(characterEncoding))
            : new String(content, StandardCharsets.UTF_8);
  }

  public void setRequestContentType(@Nullable String contentType) {
    this.contentType = contentType;
    if (contentType != null) {
      try {
        MediaType mediaType = MediaType.parseMediaType(contentType);
        if (mediaType.getCharset() != null) {
          this.characterEncoding = mediaType.getCharset().name();
        }
      }
      catch (IllegalArgumentException ex) {
        int charsetIndex = contentType.toLowerCase(Locale.ROOT).indexOf("charset=");
        if (charsetIndex != -1) {
          this.characterEncoding = contentType.substring(charsetIndex + "charset=".length());
        }
      }
    }
    updateContentTypeHeader();
  }

  // headers

  public void addRequestHeader(String name, String value) {
    if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
      setRequestContentType(value);
    }
    else {
      requestHeaders().add(name, value);
    }
  }

  public void removeRequestHeader(String name) {
    requestHeaders().remove(name);
  }

  public @Nullable String getHeader(String name) {
    return requestHeaders().getFirst(name);
  }

  public List<String> getHeaders(String name) {
    return requestHeaders().get(name);
  }

  public Collection<String> getRequestHeaderNames() {
    return requestHeaders().names();
  }

  // parameters

  public void setParameter(String name, String value) {
    getParameters().set(name, value);
  }

  public void setParameter(String name, String... values) {
    Assert.notNull(name, "Parameter name is required");
    getParameters().setOrRemove(name, List.of(values));
  }

  public void setParameters(Map<String, ?> params) {
    Assert.notNull(params, "Parameter map is required");
    params.forEach((key, value) -> {
      if (value instanceof String str) {
        setParameter(key, str);
      }
      else if (value instanceof String[] strings) {
        setParameter(key, strings);
      }
      else {
        throw new IllegalArgumentException("Parameter map value must be single value or array of type [String]");
      }
    });
  }

  public void addParameter(String name, @Nullable String value) {
    getParameters().add(name, value);
  }

  public void addParameter(String name, String... values) {
    Assert.notNull(name, "Parameter name is required");
    getParameters().addAll(name, List.of(values));
  }

  public void addParameters(Map<String, ?> params) {
    Assert.notNull(params, "Parameter map is required");
    params.forEach((key, value) -> {
      if (value instanceof String str) {
        addParameter(key, str);
      }
      else if (value instanceof String[] strings) {
        addParameter(key, strings);
      }
      else {
        throw new IllegalArgumentException("Parameter map value must be single value or array of type [String]");
      }
    });
  }

  public void removeParameter(String name) {
    getParameters().remove(name);
  }

  public void removeAllParameters() {
    getParameters().clear();
  }

  public @Nullable String getParameter(String name) {
    return CollectionUtils.firstElement(getParameters().get(name));
  }

  public String[] getParameterValues(String name) {
    List<String> values = getParameters().get(name);
    return values != null ? values.toArray(String[]::new) : null;
  }

  public Collection<String> getRequestParameterNames() {
    return getParameters().keySet();
  }

  public Map<String, List<String>> getParameterMap() {
    return getParameters();
  }

  // locale

  public void addPreferredLocale(Locale locale) {
    Assert.notNull(locale, "Locale is required");
    locales.add(0, locale);
  }

  public void setPreferredLocales(List<Locale> locales) {
    Assert.notEmpty(locales, "Locale list must not be empty");
    this.locales.clear();
    this.locales.addAll(locales);
  }

  public Locale getLocale() {
    return locales.get(0);
  }

  public List<Locale> getLocales() {
    return locales;
  }

  // cookies

  public void setCookies(HttpCookie... cookies) {
    this.cookies = cookies;
    if (cookies == null) {
      requestHeaders().remove(HttpHeaders.COOKIE);
    }
    else {
      requestHeaders().set(HttpHeaders.COOKIE, encodeCookies(cookies));
    }
  }

  private static String encodeCookies(HttpCookie... cookies) {
    return java.util.Arrays.stream(cookies)
            .map(c -> c.getName() + '=' + (c.getValue() == null ? "" : c.getValue()))
            .collect(java.util.stream.Collectors.joining("; "));
  }

  public HttpCookie @Nullable [] getCookies() {
    return cookies;
  }

  // session

  public void setSession(Session session) {
    this.session = session;
  }

  public String changeSessionId() {
    Assert.isTrue(session != null, "The request does not have a session");
    return session.changeSessionId();
  }

  // async

  public void setAsyncStarted(boolean asyncStarted) {
    this.asyncStarted = asyncStarted;
  }

  public boolean isAsyncStarted() {
    return asyncStarted;
  }

  public void setAsyncSupported(boolean asyncSupported) {
    this.asyncSupported = asyncSupported;
  }

  public boolean isAsyncSupported() {
    return asyncSupported;
  }

  public void setDispatcherType(DispatcherType dispatcherType) {
    this.dispatcherType = dispatcherType;
  }

  public DispatcherType getDispatcherType() {
    return dispatcherType;
  }

  // auth

  public void setAuthType(@Nullable String authType) {
    this.authType = authType;
  }

  public @Nullable String getAuthType() {
    return authType;
  }

  public void setUserPrincipal(@Nullable Principal userPrincipal) {
    this.userPrincipal = userPrincipal;
  }

  public @Nullable Principal getUserPrincipal() {
    return userPrincipal;
  }

  public void logout() {
    this.userPrincipal = null;
    this.authType = null;
  }

  // requested session id

  public void setRequestedSessionId(@Nullable String requestedSessionId) {
    this.requestedSessionId = requestedSessionId;
  }

  public @Nullable String getRequestedSessionId() {
    return requestedSessionId;
  }

  public void setRequestedSessionIdValid(boolean requestedSessionIdValid) {
    this.requestedSessionIdValid = requestedSessionIdValid;
  }

  public boolean isRequestedSessionIdValid() {
    return requestedSessionIdValid;
  }

  public void setRequestedSessionIdFromCookie(boolean requestedSessionIdFromCookie) {
    this.requestedSessionIdFromCookie = requestedSessionIdFromCookie;
  }

  public boolean isRequestedSessionIdFromCookie() {
    return requestedSessionIdFromCookie;
  }

  public void setRequestedSessionIdFromURL(boolean requestedSessionIdFromURL) {
    this.requestedSessionIdFromURL = requestedSessionIdFromURL;
  }

  public boolean isRequestedSessionIdFromURL() {
    return requestedSessionIdFromURL;
  }

  // multipart parts

  public void addPart(Part part) {
    parts.add(part.getName(), part);
  }

  public @Nullable Part getPart(String name) {
    return parts.getFirst(name);
  }

  public Collection<Part> getParts() {
    List<Part> result = new LinkedList<>();
    for (List<Part> list : parts.values()) {
      result.addAll(list);
    }
    return result;
  }

  // ---------------------------------------------------------------------
  // Response convenience methods
  // ---------------------------------------------------------------------

  public byte[] getResponseContentAsByteArray() {
    return responseContent.toByteArray();
  }

  public String getResponseContentAsString() {
    return responseContent.toString(getResponseCharset());
  }

  public String getResponseContentAsString(Charset charset) {
    return responseContent.toString(charset);
  }

  private Charset getResponseCharset() {
    MediaType contentType = responseHeaders().getContentType();
    if (contentType != null && contentType.getCharset() != null) {
      return contentType.getCharset();
    }
    return StandardCharsets.UTF_8;
  }

  public @Nullable String getResponseHeader(String name) {
    return responseHeaders().getFirst(name);
  }

  public List<String> getResponseHeaders(String name) {
    return responseHeaders().get(name);
  }

  public Collection<String> getResponseHeaderNames() {
    return responseHeaders().names();
  }

  public void setForwardedUrl(@Nullable String forwardedUrl) {
    this.forwardedUrl = forwardedUrl;
  }

  public @Nullable String getForwardedUrl() {
    return forwardedUrl;
  }

  public List<ResponseCookie> getResponseCookies() {
    return responseCookies();
  }

  // ---------------------------------------------------------------------
  // Inner implementations
  // ---------------------------------------------------------------------

  private final class TestingSessionManager implements SessionManager {

    @Override
    public Session createSession() {
      return new MockSession();
    }

    @Override
    public Session createSession(HttpContext context) {
      return new MockSession();
    }

    @Override
    public @Nullable Session getSession(@Nullable String sessionId) {
      return new MockSession(sessionId);
    }

    @Override
    public Session getSession(HttpContext context) {
      return TestingHttpContext.this.getSession(true);
    }

    @Override
    public @Nullable Session getSession(HttpContext context, boolean create) {
      return TestingHttpContext.this.getSession(create);
    }

  }

  private final class TestingAsyncWebRequest extends AsyncWebRequest {

    @Override
    public void startAsync() {
      asyncStarted = true;
    }

    @Override
    public boolean isAsyncStarted() {
      return asyncStarted;
    }

    @Override
    public void dispatch(@Nullable Object concurrentResult) {
      // no-op
    }

  }

  private static final class TestingMultipartRequest implements MultipartRequest {

    private final MultiValueMap<String, Part> parts;

    TestingMultipartRequest(MultiValueMap<String, Part> parts) {
      this.parts = parts;
    }

    @Override
    public MultiValueMap<String, Part> getParts() {
      return parts;
    }

    @Override
    public @Nullable List<Part> getParts(String name) {
      return parts.get(name);
    }

    @Override
    public @Nullable Part getPart(String name) {
      return parts.getFirst(name);
    }

    @Override
    public Iterable<String> getPartNames() {
      return parts.keySet();
    }

    @Override
    public @Nullable HttpHeaders getHeaders(String name) {
      return null;
    }

    @Override
    public void cleanup() {
      // no-op
    }

  }

  /**
   * Request builder exposing properties not related to the body.
   *
   * @param <B> the builder sub-class
   */
  public interface BaseBuilder<B extends BaseBuilder<B>> {

    /**
     * Append the given query parameter. The resulting query string is also
     * made available through {@link TestingHttpContext#getParameter}.
     *
     * @param name the parameter name
     * @param values the parameter values
     */
    B queryParam(String name, Object... values);

    /**
     * Append the given query parameters.
     *
     * @param params the parameters
     */
    B queryParams(MultiValueMap<String, String> params);

    /**
     * Add the given single header value(s) under the given name.
     *
     * @param name the header name
     * @param values the header value(s)
     */
    B header(String name, String... values);

    /**
     * Add the given header values.
     *
     * @param headers the headers
     */
    B headers(MultiValueMap<String, String> headers);

    /**
     * Add one or more cookies.
     */
    B cookie(HttpCookie... cookies);

    /**
     * Add one or more cookie values.
     */
    B cookie(String name, String... values);

    /**
     * Set the current {@link Session}.
     */
    B session(Session session);

    /**
     * Set the {@link ApplicationContext} that this context runs in.
     */
    B applicationContext(ApplicationContext applicationContext);

    /**
     * Set the {@link DispatcherHandler} that this context runs in.
     */
    B dispatcherHandler(DispatcherHandler dispatcherHandler);

    /**
     * Set the preferred locales, in descending order.
     */
    B locale(Locale... locales);

    /**
     * Set the {@code secure} flag.
     */
    B secure(boolean secure);

    /**
     * Set the {@code scheme} (e.g. "https").
     */
    B scheme(String scheme);

    /**
     * Set the {@code serverName}.
     */
    B serverName(String serverName);

    /**
     * Set the {@code serverPort}.
     */
    B serverPort(int serverPort);

    /**
     * Set the {@code remoteAddr}.
     */
    B remoteAddr(String remoteAddr);

    /**
     * Set the character encoding.
     */
    B characterEncoding(String characterEncoding);

    /**
     * Add a multipart {@link Part}.
     */
    B part(Part... parts);

    /**
     * Set the response status code.
     */
    B status(int status);

    /**
     * Add a response header.
     */
    B responseHeader(String name, String... values);

    /**
     * Apply a post-processor to the context after it has been built, before it
     * is returned. Since the context is a {@link TestingHttpContext}, the
     * post-processor receives the concrete, fully-available context.
     *
     * @param postProcessor the post-processor to apply, receiving the built context
     */
    B with(HttpContextRequestPostProcessor<TestingHttpContext> postProcessor);

    /**
     * Build the context.
     *
     * @return the newly built context
     */
    TestingHttpContext build();
  }

  /**
   * A builder that additionally supports configuring the request body.
   */
  public interface BodyBuilder extends BaseBuilder<BodyBuilder> {

    /**
     * Set the request body as a byte array.
     */
    BodyBuilder content(byte[] content);

    /**
     * Set the request body as text, using the given charset.
     */
    BodyBuilder content(String content, Charset charset);

    /**
     * Set the request body as text (UTF-8 encoded).
     */
    BodyBuilder content(String content);

    /**
     * Set the request Content-Type.
     */
    BodyBuilder contentType(String contentType);

    /**
     * Set the request Content-Type.
     */
    BodyBuilder contentType(MediaType contentType);

    /**
     * Set the request {@code Content-Length} header.
     */
    BodyBuilder contentLength(long contentLength);

    /**
     * Set the list of acceptable {@linkplain MediaType media types}, as
     * specified by the {@code Accept} header.
     */
    BodyBuilder accept(MediaType... acceptableMediaTypes);

    /**
     * Set the list of acceptable {@linkplain Charset charsets}, as specified
     * by the {@code Accept-Charset} header.
     */
    BodyBuilder acceptCharset(Charset... acceptableCharsets);

    /**
     * Set the list of acceptable {@linkplain Locale locales}, as specified
     * by the {@code Accept-Language} header.
     */
    BodyBuilder acceptLanguage(Locale... acceptableLocales);

    /**
     * Set the value of the {@code If-Modified-Since} header, in milliseconds
     * since January 1, 1970 GMT.
     */
    BodyBuilder ifModifiedSince(long ifModifiedSince);

    /**
     * Set the value of the {@code If-Unmodified-Since} header, in milliseconds
     * since January 1, 1970 GMT.
     */
    BodyBuilder ifUnmodifiedSince(long ifUnmodifiedSince);

    /**
     * Set the values of the {@code If-None-Match} header.
     */
    BodyBuilder ifNoneMatch(String... ifNoneMatches);

    /**
     * Set the value of the {@code Range} header.
     */
    BodyBuilder range(HttpRange... ranges);
  }

  private static final class DefaultBodyBuilder implements BodyBuilder {

    private final HttpMethod method;

    private final String requestPath;

    private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

    private final HttpHeaders headers = new DefaultHttpHeaders();

    private final LinkedMultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();

    private final List<Part> parts = new LinkedList<>();

    private final HttpHeaders responseHeaders = new DefaultHttpHeaders();

    private final List<HttpContextRequestPostProcessor<TestingHttpContext>> postProcessors = new ArrayList<>();

    @Nullable
    private Session session;

    @Nullable
    private ApplicationContext applicationContext;

    @Nullable
    private DispatcherHandler dispatcherHandler;

    private final List<Locale> locales = new LinkedList<>();

    private boolean secure;

    @Nullable
    private String scheme;

    @Nullable
    private String serverName;

    private int serverPort = -1;

    @Nullable
    private String remoteAddr;

    @Nullable
    private String characterEncoding;

    private int status = 200;

    private byte @Nullable [] content;

    @Nullable
    private String contentType;

    DefaultBodyBuilder(HttpMethod method, String url, Object... uriVars) {
      this.method = method;
      UriComponents components = UriComponentsBuilder.forURIString(url).buildAndExpand(uriVars);
      this.requestPath = components.getPath() != null ? components.getPath() : "";
      this.queryParams.putAll(components.getQueryParams());
    }

    @Override
    public BodyBuilder queryParam(String name, Object... values) {
      for (Object value : values) {
        this.queryParams.add(name, value != null ? value.toString() : null);
      }
      return this;
    }

    @Override
    public BodyBuilder queryParams(MultiValueMap<String, String> params) {
      this.queryParams.addAll(params);
      return this;
    }

    @Override
    public BodyBuilder header(String name, String... values) {
      for (String value : values) {
        this.headers.add(name, value);
      }
      return this;
    }

    @Override
    public BodyBuilder headers(MultiValueMap<String, String> headers) {
      this.headers.setAll(headers);
      return this;
    }

    @Override
    public BodyBuilder cookie(HttpCookie... cookies) {
      for (HttpCookie cookie : cookies) {
        this.cookies.add(cookie.getName(), cookie.getValue());
      }
      return this;
    }

    @Override
    public BodyBuilder cookie(String name, String... values) {
      for (String value : values) {
        this.cookies.add(name, value);
      }
      return this;
    }

    @Override
    public BodyBuilder session(Session session) {
      this.session = session;
      return this;
    }

    @Override
    public BodyBuilder applicationContext(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
      return this;
    }

    @Override
    public BodyBuilder dispatcherHandler(DispatcherHandler dispatcherHandler) {
      this.dispatcherHandler = dispatcherHandler;
      return this;
    }

    @Override
    public BodyBuilder locale(Locale... locales) {
      this.locales.addAll(Arrays.asList(locales));
      return this;
    }

    @Override
    public BodyBuilder secure(boolean secure) {
      this.secure = secure;
      return this;
    }

    @Override
    public BodyBuilder scheme(String scheme) {
      this.scheme = scheme;
      return this;
    }

    @Override
    public BodyBuilder serverName(String serverName) {
      this.serverName = serverName;
      return this;
    }

    @Override
    public BodyBuilder serverPort(int serverPort) {
      this.serverPort = serverPort;
      return this;
    }

    @Override
    public BodyBuilder remoteAddr(String remoteAddr) {
      this.remoteAddr = remoteAddr;
      return this;
    }

    @Override
    public BodyBuilder characterEncoding(String characterEncoding) {
      this.characterEncoding = characterEncoding;
      return this;
    }

    @Override
    public BodyBuilder part(Part... parts) {
      this.parts.addAll(Arrays.asList(parts));
      return this;
    }

    @Override
    public BodyBuilder status(int status) {
      this.status = status;
      return this;
    }

    @Override
    public BodyBuilder responseHeader(String name, String... values) {
      for (String value : values) {
        this.responseHeaders.add(name, value);
      }
      return this;
    }

    @Override
    public BodyBuilder with(HttpContextRequestPostProcessor<TestingHttpContext> postProcessor) {
      this.postProcessors.add(postProcessor);
      return this;
    }

    @Override
    public BodyBuilder content(byte[] content) {
      this.content = content;
      return this;
    }

    @Override
    public BodyBuilder content(String content, Charset charset) {
      return content(content.getBytes(charset));
    }

    @Override
    public BodyBuilder content(String content) {
      return content(content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public BodyBuilder contentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    @Override
    public BodyBuilder contentType(MediaType contentType) {
      return contentType(contentType.toString());
    }

    @Override
    public BodyBuilder contentLength(long contentLength) {
      this.headers.set(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
      return this;
    }

    @Override
    public BodyBuilder accept(MediaType... acceptableMediaTypes) {
      this.headers.setAccept(Arrays.asList(acceptableMediaTypes));
      return this;
    }

    @Override
    public BodyBuilder acceptCharset(Charset... acceptableCharsets) {
      this.headers.setAcceptCharset(Arrays.asList(acceptableCharsets));
      return this;
    }

    @Override
    public BodyBuilder acceptLanguage(Locale... acceptableLocales) {
      this.headers.setAcceptLanguageAsLocales(Arrays.asList(acceptableLocales));
      return this;
    }

    @Override
    public BodyBuilder ifModifiedSince(long ifModifiedSince) {
      this.headers.setIfModifiedSince(ifModifiedSince);
      return this;
    }

    @Override
    public BodyBuilder ifUnmodifiedSince(long ifUnmodifiedSince) {
      this.headers.setIfUnmodifiedSince(ifUnmodifiedSince);
      return this;
    }

    @Override
    public BodyBuilder ifNoneMatch(String... ifNoneMatches) {
      this.headers.setIfNoneMatch(Arrays.asList(ifNoneMatches));
      return this;
    }

    @Override
    public BodyBuilder range(HttpRange... ranges) {
      this.headers.setRange(Arrays.asList(ranges));
      return this;
    }

    @Override
    public TestingHttpContext build() {
      TestingHttpContext context = new TestingHttpContext(applicationContext, dispatcherHandler);
      context.setMethod(method.name());
      context.setRequestURI(requestPath);

      if (!queryParams.isEmpty()) {
        context.setQueryString(buildQueryString());
        queryParams.forEach((name, values) -> values.forEach(value -> context.addParameter(name, value)));
      }

      headers.forEach((name, values) -> values.forEach(value -> context.addRequestHeader(name, value)));
      responseHeaders.forEach((name, values) -> values.forEach(value -> context.responseHeaders().add(name, value)));

      if (!cookies.isEmpty()) {
        HttpCookie[] cookieArray = new HttpCookie[cookies.size()];
        int i = 0;
        for (var entry : cookies.entrySet()) {
          for (String value : entry.getValue()) {
            cookieArray[i++] = new HttpCookie(entry.getKey(), value);
          }
        }
        context.setCookies(cookieArray);
      }

      if (session != null) {
        context.setSession(session);
      }
      if (!locales.isEmpty()) {
        context.setPreferredLocales(locales);
      }
      context.setSecure(secure);
      if (scheme != null) {
        context.setScheme(scheme);
      }
      if (serverName != null) {
        context.setServerName(serverName);
      }
      if (serverPort != -1) {
        context.setServerPort(serverPort);
      }
      if (remoteAddr != null) {
        context.setRemoteAddr(remoteAddr);
      }
      if (characterEncoding != null) {
        context.setCharacterEncoding(characterEncoding);
      }
      parts.forEach(context::addPart);
      context.setStatus(status);
      if (content != null) {
        context.setContent(content);
      }
      if (contentType != null) {
        context.setRequestContentType(contentType);
      }
      TestingHttpContext result = context;
      for (HttpContextRequestPostProcessor<TestingHttpContext> postProcessor : postProcessors) {
        result = postProcessor.postProcessRequest(result);
      }
      return result;
    }

    private String buildQueryString() {
      StringBuilder sb = new StringBuilder();
      queryParams.forEach((name, values) -> {
        for (String value : values) {
          if (sb.length() > 0) {
            sb.append('&');
          }
          sb.append(name).append('=').append(value);
        }
      });
      return sb.toString();
    }
  }

}
