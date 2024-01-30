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

package cn.taketoday.framework.web.netty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.server.ServerHttpResponse;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.DispatcherHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.async.AsyncWebRequest;
import cn.taketoday.web.context.async.WebAsyncManager;
import cn.taketoday.web.multipart.MultipartRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.http.CombinedHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostStandardRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.util.internal.ObjectPool;

/**
 * Netty Request context
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-04 21:24
 */
public class NettyRequestContext extends RequestContext {

  FullHttpRequest request;

  ChannelHandlerContext channelContext;

  long requestTimeMillis;

  @Nullable
  private InterfaceHttpPostRequestDecoder requestDecoder;

  private final NettyRequestConfig config;

  // headers and status-code is written? default = false
  private final AtomicBoolean committed = new AtomicBoolean();

  // response
  @Nullable
  private Boolean keepAlive;

  @Nullable
  private String remoteAddress;

  private HttpResponseStatus status = HttpResponseStatus.OK;

  @Nullable
  private /* volatile ?*/ ByteBuf responseBody;

  @Nullable
  private FileRegion fileToSend;

  /**
   * response headers
   */
  final HttpHeaders nettyResponseHeaders;

  @Nullable
  private Integer queryStringIndex;

  @Nullable
  private InetSocketAddress inetSocketAddress;

  /**
   * @since 4.0
   */
  private final ObjectPool.Handle<NettyRequestContext> handle;

  public NettyRequestContext(ApplicationContext context, NettyRequestConfig config,
          DispatcherHandler dispatcherHandler, ObjectPool.Handle<NettyRequestContext> handle) {
    super(context, dispatcherHandler);
    this.config = config;
    this.nettyResponseHeaders =
            config.isSingleFieldHeaders()
            ? new io.netty.handler.codec.http.DefaultHttpHeaders(config.isValidateHeaders())
            : new CombinedHttpHeaders(config.isValidateHeaders());
    this.handle = handle;
  }

  @Override
  public long getRequestTimeMillis() {
    return requestTimeMillis;
  }

  @Nullable
  @Override
  protected String initId() {
    return channelContext.channel().id().asShortText();
  }

  @Override
  public String getScheme() {
    int port = inetSocketAddress().getPort();
    if (port == 443) {
      return Constant.HTTPS;
    }
    return Constant.HTTP;
  }

  private InetSocketAddress inetSocketAddress() {
    InetSocketAddress inetSocketAddress = this.inetSocketAddress;
    if (inetSocketAddress == null) {
      SocketAddress socketAddress = channelContext.channel().localAddress();
      if (socketAddress instanceof InetSocketAddress address) {
        inetSocketAddress = address;
      }
      else {
        inetSocketAddress = new InetSocketAddress("localhost", 8080);
      }
      this.inetSocketAddress = inetSocketAddress;
    }
    return inetSocketAddress;
  }

  @Override
  public int getServerPort() {
    return inetSocketAddress().getPort();
  }

  @Override
  public String getServerName() {
    return inetSocketAddress().getHostString();
  }

  @Override
  protected final String doGetRequestURI() {
    String uri = request.uri();
    int index = queryStringIndex(uri);
    if (index == -1) {
      return uri;
    }
    else {
      return uri.substring(0, index);
    }
  }

  @Override
  public final String doGetQueryString() {
    String uri = request.uri();
    int index = queryStringIndex(uri);
    if (index == -1) {
      return Constant.BLANK;
    }
    else {
      return uri.substring(index + 1);
    }
  }

  private int queryStringIndex(String uri) {
    Integer index = queryStringIndex;
    if (index == null) {
      index = uri.indexOf('?');
      this.queryStringIndex = index;
    }
    return index;
  }

  @Override
  public String getRequestURL() {
    String host = request.headers().get(DefaultHttpHeaders.HOST);
    if (host == null) {
      host = "localhost";
    }
    return getScheme() + "://" + host + StringUtils.prependLeadingSlash(getRequestURI());
  }

  @Override
  public final String getRemoteAddress() {
    if (remoteAddress == null) {
      InetSocketAddress remote = (InetSocketAddress) channelContext.channel().remoteAddress();
      remoteAddress = remote.getAddress().getHostAddress();
    }
    return remoteAddress;
  }

  @Override
  public final String doGetMethod() {
    return this.request.method().name();
  }

  @Override
  protected InputStream doGetInputStream() {
    return new ByteBufInputStream(request.content());
  }

  @Override
  public cn.taketoday.http.HttpHeaders requestHeaders() {
    var requestHeaders = this.requestHeaders;
    if (requestHeaders == null) {
      requestHeaders = new DefaultHttpHeaders();
      for (Map.Entry<String, String> header : request.headers()) {
        requestHeaders.add(header.getKey(), header.getValue());
      }
      this.requestHeaders = requestHeaders;
    }
    else if (requestHeaders.isEmpty()) {
      for (Map.Entry<String, String> header : request.headers()) {
        requestHeaders.add(header.getKey(), header.getValue());
      }
    }
    return requestHeaders;
  }

  @Override
  protected cn.taketoday.http.HttpHeaders createRequestHeaders() {
    HttpHeaders headers = request.headers();
    DefaultHttpHeaders ret = new DefaultHttpHeaders();
    for (Map.Entry<String, String> header : headers) {
      ret.add(header.getKey(), header.getValue());
    }
    new NettyHttpHeaders(nettyResponseHeaders);
    return ret;
  }

  @Override
  public String getContentType() {
    return request.headers().get(DefaultHttpHeaders.CONTENT_TYPE);
  }

  @Override
  public boolean containsResponseHeader(String name) {
    return nettyResponseHeaders.contains(name);
  }

  @Nullable
  @Override
  public String getResponseContentType() {
    String contentType = this.responseContentType;
    if (contentType == null) {
      contentType = nettyResponseHeaders.get(DefaultHttpHeaders.CONTENT_TYPE);
      if (contentType != null) {
        this.responseContentType = contentType;
      }
    }
    return contentType;
  }

  @Override
  protected cn.taketoday.http.HttpHeaders createResponseHeaders() {
    return new NettyHttpHeaders(nettyResponseHeaders);
  }

  @Override
  public HttpCookie[] doGetCookies() {
    List<String> allCookie = request.headers().getAll(DefaultHttpHeaders.COOKIE);
    if (CollectionUtils.isEmpty(allCookie)) {
      return EMPTY_COOKIES;
    }
    Set<Cookie> decoded;
    ServerCookieDecoder cookieDecoder = config.getCookieDecoder();
    if (allCookie.size() == 1) {
      decoded = cookieDecoder.decode(allCookie.get(0));
    }
    else {
      decoded = new TreeSet<>();
      for (String header : allCookie) {
        decoded.addAll(cookieDecoder.decode(header));
      }
    }
    if (CollectionUtils.isEmpty(decoded)) {
      return EMPTY_COOKIES;
    }
    else {
      int i = 0;
      HttpCookie[] ret = new HttpCookie[decoded.size()];
      for (Cookie cookie : decoded) {
        ret[i++] = new HttpCookie(cookie.name(), cookie.value());
      }
      return ret;
    }
  }

  @Override
  protected void postGetParameters(MultiValueMap<String, String> parameters) {
    HttpMethod method = getMethod();
    if (method != HttpMethod.GET && method != HttpMethod.HEAD
            && StringUtils.startsWithIgnoreCase(getContentType(), MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
      for (InterfaceHttpData data : requestDecoder().getBodyHttpDatas()) {
        if (data instanceof Attribute) {
          try {
            parameters.add(data.getName(), ((Attribute) data).getValue());
          }
          catch (IOException e) {
            throw new HttpMessageNotReadableException("'application/x-www-form-urlencoded' content netty read failed", e, this);
          }
        }
      }
    }
  }

  InterfaceHttpPostRequestDecoder requestDecoder() {
    InterfaceHttpPostRequestDecoder requestDecoder = this.requestDecoder;
    if (requestDecoder == null) {
      Charset charset = config.getPostRequestDecoderCharset();
      HttpDataFactory httpDataFactory = config.getHttpDataFactory();
      if (isMultipart()) {
        requestDecoder = new HttpPostMultipartRequestDecoder(httpDataFactory, request, charset);
      }
      else {
        requestDecoder = new HttpPostStandardRequestDecoder(httpDataFactory, request, charset);
      }
      requestDecoder.setDiscardThreshold(0);
      this.requestDecoder = requestDecoder;
    }
    return requestDecoder;
  }

  @Override
  public long getContentLength() {
    return request.content().readableBytes();
  }

  @Override
  public void sendRedirect(String location) {
    this.status = HttpResponseStatus.FOUND;
    nettyResponseHeaders.set(DefaultHttpHeaders.LOCATION, location);
    commit();
  }

  public void setKeepAlive(boolean keepAlive) {
    this.keepAlive = keepAlive;
  }

  private boolean isKeepAlive() {
    Boolean keepAlive = this.keepAlive;
    if (keepAlive == null) {
      keepAlive = HttpUtil.isKeepAlive(request);
      this.keepAlive = keepAlive;
    }
    return keepAlive;
  }

  @Override
  public void setStatus(int sc) {
    this.status = HttpResponseStatus.valueOf(sc);
  }

  @Override
  public void setStatus(HttpStatusCode status) {
    this.status = HttpResponseStatus.valueOf(status.value());
  }

  @Override
  public int getStatus() {
    return status.code();
  }

  @Override
  public void sendError(int sc) throws IOException {
    sendError(sc, null);
  }

  @Override
  public void sendError(int sc, @Nullable String msg) throws IOException {
    reset();
    this.status = HttpResponseStatus.valueOf(sc);
    config.getSendErrorHandler().handleError(this, msg);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final FullHttpRequest nativeRequest() {
    return request;
  }

  @Override
  @Nullable
  public <T> T unwrapRequest(Class<T> requestClass) {
    if (requestClass.isInstance(request)) {
      return requestClass.cast(request);
    }
    return null;
  }

  @Override
  protected MultipartRequest createMultipartRequest() {
    return new NettyMultipartRequest(this);
  }

  @Override
  protected AsyncWebRequest createAsyncWebRequest() {
    return new NettyAsyncWebRequest(this);
  }

  /**
   * HTTP response body
   *
   * @return HTTP response body
   */
  public final ByteBuf responseBody() {
    ByteBuf responseBody = this.responseBody;
    if (responseBody == null) {
      Function<RequestContext, ByteBuf> bodyFactory = config.getResponseBodyFactory();
      if (bodyFactory != null) {
        responseBody = bodyFactory.apply(this); // may null
      }
      if (responseBody == null) {
        // fallback
        responseBody = createResponseBody(channelContext, config);
      }
      this.responseBody = responseBody;
    }
    return responseBody;
  }

  protected ByteBuf createResponseBody(ChannelHandlerContext channelContext, NettyRequestConfig config) {
    return channelContext.alloc().ioBuffer(config.getBodyInitialSize());
  }

  @Override
  public ServerHttpResponse asHttpOutputMessage() {
    return new NettyHttpOutputMessage();
  }

  @Override
  public void flush() {
    if (writer != null) {
      writer.flush();
    }

    writeHeaders();

    FileRegion fileToSend;
    ByteBuf responseBody = this.responseBody;
    if (responseBody != null) {
      this.responseBody = null;
      // DefaultHttpContent
      channelContext.writeAndFlush(responseBody);
    }
    else if ((fileToSend = this.fileToSend) != null) {
      channelContext.writeAndFlush(fileToSend);
    }
  }

  @Override
  protected OutputStream doGetOutputStream() {
    return getMethod() == HttpMethod.HEAD ? new NoBodyOutputStream() : new ResponseBodyOutputStream();
  }

  @Override
  protected void postRequestCompleted(@Nullable Throwable notHandled) {
    if (notHandled != null) {
      recycle();
      return;
    }

    flush();

    var trailerHeadersConsumer = config.getTrailerHeadersConsumer();
    LastHttpContent lastHttpContent = LastHttpContent.EMPTY_LAST_CONTENT;
    // https://datatracker.ietf.org/doc/html/rfc7230#section-4.1.2
    // A trailer allows the sender to include additional fields at the end
    // of a chunked message in order to supply metadata that might be
    // dynamically generated while the message body is sent, such as a
    // message integrity check, digital signature, or post-processing
    // status.
    if (trailerHeadersConsumer != null && isTransferEncodingChunked(nettyResponseHeaders)) {
      // https://datatracker.ietf.org/doc/html/rfc7230#section-4.4
      // When a message includes a message body encoded with the chunked
      // transfer coding and the sender desires to send metadata in the form
      // of trailer fields at the end of the message, the sender SHOULD
      // generate a Trailer header field before the message body to indicate
      // which fields will be present in the trailers.
      String declaredHeaderNames = nettyResponseHeaders.get(DefaultHttpHeaders.TRAILER);
      if (declaredHeaderNames != null) {
        HttpHeaders trailerHeaders = new TrailerHeaders(declaredHeaderNames);
        try {
          trailerHeadersConsumer.accept(trailerHeaders);
        }
        catch (IllegalArgumentException e) {
          // A sender MUST NOT generate a trailer when header names are
          // TrailerHeaders.DISALLOWED_TRAILER_HEADER_NAMES
        }
        if (!trailerHeaders.isEmpty()) {
          lastHttpContent = new DefaultLastHttpContent();
          lastHttpContent.trailingHeaders().set(trailerHeaders);
        }
      }
    }

    if (isKeepAlive()) {
      channelContext.writeAndFlush(lastHttpContent);
    }
    else {
      channelContext.writeAndFlush(lastHttpContent)
              .addListener(ChannelFutureListener.CLOSE);
    }

    InterfaceHttpPostRequestDecoder requestDecoder = this.requestDecoder;
    if (requestDecoder != null) {
      requestDecoder.destroy();
    }

    recycle();
  }

  /**
   * Send HTTP message to the client
   *
   * @throws IllegalStateException If the response has been committed
   */
  private void commit() {
    assertNotCommitted();
    writeHeaders();
  }

  @Override
  public void writeHeaders() {
    if (committed.compareAndSet(false, true)) {
      // ---------------------------------------------
      // apply Status code and headers
      // ---------------------------------------------
      HttpHeaders headers = nettyResponseHeaders;
      // set Content-Length header
      if (MediaType.TEXT_EVENT_STREAM_VALUE.equals(getResponseContentType())) {
        headers.set(DefaultHttpHeaders.TRANSFER_ENCODING, DefaultHttpHeaders.CHUNKED);
        headers.remove(DefaultHttpHeaders.CONTENT_LENGTH);
      }
      else if (!isTransferEncodingChunked(headers)) {
        if (headers.get(DefaultHttpHeaders.CONTENT_LENGTH) == null) {
          ByteBuf responseBody = this.responseBody;
          if (responseBody == null) {
            if (getMethod() == HttpMethod.HEAD && outputStream instanceof NoBodyOutputStream nbStream) {
              headers.set(DefaultHttpHeaders.CONTENT_LENGTH, nbStream.contentLength);
            }
            else {
              headers.setInt(DefaultHttpHeaders.CONTENT_LENGTH, 0);
            }
          }
          else {
            headers.setInt(DefaultHttpHeaders.CONTENT_LENGTH, responseBody.readableBytes());
          }
        }
      }

      // apply cookies
      ArrayList<HttpCookie> responseCookies = this.responseCookies;
      if (responseCookies != null) {
        ServerCookieEncoder cookieEncoder = config.getCookieEncoder();
        for (HttpCookie cookie : responseCookies) {
          DefaultCookie nettyCookie = new DefaultCookie(cookie.getName(), cookie.getValue());
          if (cookie instanceof ResponseCookie responseCookie) {
            nettyCookie.setPath(responseCookie.getPath());
            nettyCookie.setDomain(responseCookie.getDomain());
            nettyCookie.setMaxAge(responseCookie.getMaxAge().getSeconds());
            nettyCookie.setSecure(responseCookie.isSecure());
            nettyCookie.setHttpOnly(responseCookie.isHttpOnly());
            nettyCookie.setSameSite(forSameSite(responseCookie.getSameSite()));
          }

          headers.add(DefaultHttpHeaders.SET_COOKIE, cookieEncoder.encode(nettyCookie));
        }
      }

      // write response
      HttpVersion httpVersion = config.getHttpVersion();
      if (isKeepAlive()) {
        HttpUtil.setKeepAlive(headers, httpVersion, true);
      }

      var noBody = new DefaultHttpResponse(httpVersion, status, headers);
      channelContext.write(noBody);
    }

  }

  @Override
  public void setContentLength(long length) {
    nettyResponseHeaders.set(DefaultHttpHeaders.CONTENT_LENGTH, length);
  }

  @Override
  public boolean isCommitted() {
    return committed.get();
  }

  @Override
  public void reset() {
    assertNotCommitted();
    nettyResponseHeaders.clear();

    ByteBuf responseBody = this.responseBody;
    if (responseBody != null) {
      responseBody.resetWriterIndex();
      responseBody.resetReaderIndex();
      writer = null;
    }
    status = HttpResponseStatus.OK;
  }

  /**
   * assert that response is committed?
   *
   * @throws IllegalStateException if response is committed
   */
  private void assertNotCommitted() {
    if (committed.get()) {
      throw new IllegalStateException("The response has been committed");
    }
  }

  @Override
  protected String doGetContextPath() {
    return config.getContextPath();
  }

  /**
   * write result to client
   *
   * @param concurrentResult async result
   * @throws Throwable dispatch error
   */
  void dispatchConcurrentResult(Object concurrentResult) throws Throwable {
    Object handler = WebAsyncManager.findHttpRequestHandler(this);
    dispatcherHandler.handleConcurrentResult(this, handler, concurrentResult);
  }

  /**
   * reset state
   */
  private void recycle() {
    inetSocketAddress = null;
    queryStringIndex = null;
    nettyResponseHeaders.clear();
    fileToSend = null;
    responseBody = null;
    status = HttpResponseStatus.OK;
    remoteAddress = null;
    keepAlive = null;
    requestDecoder = null;
    contextPath = null;
    cookies = null;
    writer = null;
    reader = null;
    inputStream = null;

    method = null;
    requestURI = null;
    requestPath = null;
    parameters = null;
    queryString = null;
    uri = null;
    httpMethod = null;
    lookupPath = null;
    locale = null;
    responseContentType = null;
    multipartRequest = null;
    asyncWebRequest = null;
    webAsyncManager = null;
    notModified = false;
    matchingMetadata = null;
    bindingContext = null;
    redirectModel = null;
    multipartFlag = null;
    preFlightRequestFlag = null;
    corsRequestFlag = null;
    requestDestructionCallbacks = null;
    requestCompletedTimeMillis = 0;
    id = null;

    clearAttributes();

    reset();

    if (outputStream instanceof NoBodyOutputStream nbStream) {
      nbStream.contentLength = 0;
    }

    if (requestHeaders != null) {
      requestHeaders.clear();
    }

    if (hasResponseCookie()) {
      responseCookies.clear();
    }

    handle.recycle(this);
  }

  /**
   * Return the enum value corresponding to the passed in same-site-flag, using a case insensitive comparison.
   *
   * @param name value for the SameSite Attribute
   * @return enum value for the provided name or null
   */
  @Nullable
  static CookieHeaderNames.SameSite forSameSite(@Nullable String name) {
    if (name != null) {
      for (var each : CookieHeaderNames.SameSite.class.getEnumConstants()) {
        if (each.name().equalsIgnoreCase(name)) {
          return each;
        }
      }
    }
    return null;
  }

  /**
   * Checks to see if the transfer encoding in a specified {@link HttpMessage} is chunked
   *
   * @return True if transfer encoding is chunked, otherwise false
   */
  private static boolean isTransferEncodingChunked(HttpHeaders responseHeaders) {
    return responseHeaders.containsValue(
            DefaultHttpHeaders.TRANSFER_ENCODING, DefaultHttpHeaders.CHUNKED, true);
  }

  private static final class TrailerHeaders extends io.netty.handler.codec.http.DefaultHttpHeaders {

    static final HashSet<String> DISALLOWED_TRAILER_HEADER_NAMES = new HashSet<>(14);

    static {
      // https://datatracker.ietf.org/doc/html/rfc7230#section-4.1.2
      // A sender MUST NOT generate a trailer that contains a field necessary
      // for message framing (e.g., Transfer-Encoding and Content-Length),
      // routing (e.g., Host), request modifiers (e.g., controls and
      // conditionals in Section 5 of [RFC7231]), authentication (e.g., see
      // [RFC7235] and [RFC6265]), response control data (e.g., see Section
      // 7.1 of [RFC7231]), or determining how to process the payload (e.g.,
      // Content-Encoding, Content-Type, Content-Range, and Trailer).
      DISALLOWED_TRAILER_HEADER_NAMES.add("age");
      DISALLOWED_TRAILER_HEADER_NAMES.add("cache-control");
      DISALLOWED_TRAILER_HEADER_NAMES.add("content-encoding");
      DISALLOWED_TRAILER_HEADER_NAMES.add("content-length");
      DISALLOWED_TRAILER_HEADER_NAMES.add("content-range");
      DISALLOWED_TRAILER_HEADER_NAMES.add("content-type");
      DISALLOWED_TRAILER_HEADER_NAMES.add("date");
      DISALLOWED_TRAILER_HEADER_NAMES.add("expires");
      DISALLOWED_TRAILER_HEADER_NAMES.add("location");
      DISALLOWED_TRAILER_HEADER_NAMES.add("retry-after");
      DISALLOWED_TRAILER_HEADER_NAMES.add("trailer");
      DISALLOWED_TRAILER_HEADER_NAMES.add("transfer-encoding");
      DISALLOWED_TRAILER_HEADER_NAMES.add("vary");
      DISALLOWED_TRAILER_HEADER_NAMES.add("warning");
    }

    TrailerHeaders(String declaredHeaderNames) {
      super(true, new TrailerNameValidator(filterHeaderNames(declaredHeaderNames)));
    }

    static HashSet<String> filterHeaderNames(String declaredHeaderNames) {
      HashSet<String> result = new HashSet<>();
      String[] names = declaredHeaderNames.split(",", -1);
      for (String name : names) {
        String trimmedStr = name.trim();
        if (trimmedStr.isEmpty() ||
                DISALLOWED_TRAILER_HEADER_NAMES.contains(trimmedStr.toLowerCase(Locale.ENGLISH))) {
          continue;
        }
        result.add(trimmedStr);
      }
      return result;
    }

  }

  static final class TrailerNameValidator implements DefaultHeaders.NameValidator<CharSequence> {

    /**
     * Contains the headers names specified with {@link DefaultHttpHeaders#TRAILER}
     */
    final HashSet<String> declaredHeaderNames;

    TrailerNameValidator(HashSet<String> declaredHeaderNames) {
      this.declaredHeaderNames = declaredHeaderNames;
    }

    @Override
    public void validateName(CharSequence name) {
      if (!declaredHeaderNames.contains(name.toString())) {
        throw new IllegalArgumentException("Trailer header name [" + name +
                "] not declared with [Trailer] header, or it is not a valid trailer header name");
      }
    }
  }

  static final class NoBodyOutputStream extends OutputStream {

    public int contentLength = 0;

    @Override
    public void write(int b) {
      contentLength++;
    }

    @Override
    public void write(byte[] buf, int offset, int len) {
      if (offset < 0 || len < 0 || offset + len > buf.length) {
        throw new IndexOutOfBoundsException("Invalid offset [%s] and / or length [%s] specified for array of size [%s]"
                .formatted(offset, len, buf.length));
      }

      contentLength += len;
    }

  }

  final class ResponseBodyOutputStream extends OutputStream {

    @Override
    public void write(int b) {
      responseBody().writeByte(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      if (len != 0) {
        responseBody().writeBytes(b, off, len);
      }
    }

  }

  final class NettyHttpOutputMessage implements ServerHttpResponse {

    @Override
    public void setStatusCode(HttpStatusCode status) {
      setStatus(status);
    }

    @Override
    public void flush() {
      NettyRequestContext.this.flush();
    }

    @Override
    public void close() {
      writeHeaders();
    }

    @Override
    public OutputStream getBody() throws IOException {
      return getOutputStream();
    }

    @Override
    public cn.taketoday.http.HttpHeaders getHeaders() {
      return responseHeaders();
    }

    @Override
    public boolean supportsZeroCopy() {
      return true;
    }

    @Override
    public void sendFile(Path file, long position, long count) {
      sendFile(file.toFile(), position, count);
    }

    @Override
    public void sendFile(File file, long position, long count) {
      fileToSend = new DefaultFileRegion(file, position, count);
    }

  }

}
