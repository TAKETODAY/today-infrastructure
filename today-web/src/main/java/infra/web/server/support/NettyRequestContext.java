/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.server.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.context.ApplicationContext;
import infra.http.DefaultHttpHeaders;
import infra.http.HttpCookie;
import infra.http.HttpMethod;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.server.ServerHttpResponse;
import infra.http.support.Netty4HttpHeaders;
import infra.lang.Constant;
import infra.lang.Nullable;
import infra.lang.TodayStrategies;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import infra.web.DispatcherHandler;
import infra.web.RequestContext;
import infra.web.async.AsyncWebRequest;
import infra.web.async.WebAsyncManager;
import infra.web.multipart.MultipartRequest;
import infra.web.server.error.SendErrorHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.DefaultHeaders;
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
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostStandardRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.PlatformDependent;

import static io.netty.util.internal.StringUtil.decodeHexByte;

/**
 * Netty Request context
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-04 21:24
 */
public class NettyRequestContext extends RequestContext {

  /**
   * System property to configure the nio file chunk size when ssl is enabled.
   * <p>Can also be configured via the {@link TodayStrategies} mechanism.
   *
   * @since 5.0
   */
  private static final int nioFileChunkSize = TodayStrategies.getInt("infra.web.ssl.nio-file-chunked-size", 4096);

  /**
   * RFC 3986 (the URI standard) makes no mention of using '+' to encode a space in a URI query component. The
   * whatwg HTML standard, however, defines the query to be encoded with the
   * {@code application/x-www-form-urlencoded} serializer defined in the whatwg URL standard, which does use '+'
   * to encode a space instead of {@code %20}.
   * <p>This flag controls whether the decoding should happen according to HTML rules, which decodes the '+' to a
   * space. The default is {@code true}.
   *
   * @since 5.0
   */
  private static final boolean htmlQueryDecoding = TodayStrategies.getFlag("infra.web.htmlQueryDecoding", true);

  /**
   * Maximum number of query parameters allowed, to mitigate HashDOS.
   * 1024 by default.
   *
   * @since 5.0
   */
  private static final int maxQueryParams = TodayStrategies.getInt("infra.web.maxQueryParams", 1024);

  /**
   * {@code false} by default. If set to {@code true}, instead of allowing query parameters to be separated by
   * semicolons, treat the semicolon as a normal character in a query value.
   *
   * @since 5.0
   */
  private static final boolean semicolonAsNormalChar = TodayStrategies.getFlag("infra.web.semicolonAsNormalChar", false);

  /**
   * For Chunk file written
   *
   * @see ChunkedWriteHandler
   * @since 5.0
   */
  private static final String CHUNKED_WRITER_NAME = "chunkedWriter";

  // UNSAFE fields
  public final NettyRequestConfig config;

  public final Channel channel;

  /**
   * response headers
   */
  public final HttpHeaders nettyResponseHeaders;
  // UNSAFE fields END

  private final FullHttpRequest request;

  // headers and status-code is written? default = false
  private final AtomicBoolean committed = new AtomicBoolean();

  private final long requestTimeMillis = System.currentTimeMillis();

  @Nullable
  private String remoteAddress;

  @Nullable
  private InterfaceHttpPostRequestDecoder requestDecoder;

  // response
  @Nullable
  private Boolean keepAlive;

  private HttpResponseStatus status = HttpResponseStatus.OK;

  @Nullable
  private /* volatile ?*/ ByteBuf responseBody;

  @Nullable
  private Object fileToSend;

  @Nullable
  private Integer queryStringIndex;

  @Nullable
  private ServerHttpResponse httpOutputMessage;

  protected NettyRequestContext(ApplicationContext context, ChannelHandlerContext ctx,
          FullHttpRequest request, NettyRequestConfig config, DispatcherHandler dispatcherHandler) {
    super(context, dispatcherHandler);
    this.config = config;
    this.request = request;
    this.channel = ctx.channel();
    this.nettyResponseHeaders = config.httpHeadersFactory.newHeaders();
  }

  @Override
  public long getRequestTimeMillis() {
    return requestTimeMillis;
  }

  @Override
  public String getScheme() {
    return config.secure ? Constant.HTTPS : Constant.HTTP;
  }

  @Override
  public int getServerPort() {
    SocketAddress socketAddress = localAddress();
    if (socketAddress instanceof InetSocketAddress) {
      return ((InetSocketAddress) socketAddress).getPort();
    }
    throw new IllegalStateException("unable to determine server port");
  }

  @Override
  public String getServerName() {
    SocketAddress socketAddress = localAddress();
    if (socketAddress instanceof InetSocketAddress) {
      return ((InetSocketAddress) socketAddress).getHostString();
    }
    throw new IllegalStateException("unable to determine server name");
  }

  @Override
  public String getRemoteAddress() {
    if (remoteAddress == null) {
      remoteAddress = remoteAddress().getAddress().getHostAddress();
    }
    return remoteAddress;
  }

  @Override
  public InetSocketAddress remoteAddress() {
    return (InetSocketAddress) channel.remoteAddress();
  }

  @Override
  public SocketAddress localAddress() {
    return channel.localAddress();
  }

  @Override
  protected final String readRequestURI() {
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
  public final String readQueryString() {
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
  public final String readMethod() {
    return request.method().name();
  }

  @Override
  protected PrintWriter createWriter() throws IOException {
    return new PrintWriter(getOutputStream(), true, config.writerCharset);
  }

  @Override
  protected InputStream createInputStream() {
    return new ByteBufInputStream(request.content());
  }

  @Override
  protected infra.http.HttpHeaders createRequestHeaders() {
    HttpHeaders headers = request.headers();
    DefaultHttpHeaders ret = new DefaultHttpHeaders();
    for (Map.Entry<String, String> header : headers) {
      ret.add(header.getKey(), header.getValue());
    }
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
  protected infra.http.HttpHeaders createResponseHeaders() {
    return new Netty4HttpHeaders(nettyResponseHeaders);
  }

  @Override
  public HttpCookie[] readCookies() {
    List<String> allCookie = request.headers().getAll(DefaultHttpHeaders.COOKIE);
    if (CollectionUtils.isEmpty(allCookie)) {
      return EMPTY_COOKIES;
    }
    Set<Cookie> decoded;
    ServerCookieDecoder cookieDecoder = config.cookieDecoder;
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
  protected MultiValueMap<String, String> readParameters() {
    String queryString = getQueryString();
    MultiValueMap<String, String> parameters = MultiValueMap.forSmartListAdaption(new LinkedHashMap<>());
    if (StringUtils.isNotEmpty(queryString)) {
      parseParameters(parameters, queryString);
    }

    HttpMethod method = getMethod();
    if (method != HttpMethod.GET && method != HttpMethod.HEAD
            && StringUtils.startsWithIgnoreCase(getContentType(), MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
      for (InterfaceHttpData data : requestDecoder().getBodyHttpDatas()) {
        if (data instanceof Attribute) {
          try {
            parameters.add(data.getName(), ((Attribute) data).getValue());
          }
          catch (IOException e) {
            throw new HttpMessageNotReadableException("'application/x-www-form-urlencoded' content read failed", e, this);
          }
        }
      }
    }
    return parameters;
  }

  InterfaceHttpPostRequestDecoder requestDecoder() {
    InterfaceHttpPostRequestDecoder requestDecoder = this.requestDecoder;
    if (requestDecoder == null) {
      if (isMultipart()) {
        requestDecoder = new HttpPostMultipartRequestDecoder(config.httpDataFactory, request, config.postRequestDecoderCharset);
      }
      else {
        requestDecoder = new HttpPostStandardRequestDecoder(config.httpDataFactory, request, config.postRequestDecoderCharset);
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

  private boolean isKeepAlive() {
    Boolean keepAlive = this.keepAlive;
    if (keepAlive == null) {
      keepAlive = HttpUtil.isKeepAlive(request);
      this.keepAlive = keepAlive;
    }
    return keepAlive;
  }

  /**
   * HTTP response body
   *
   * @return HTTP response body
   */
  public final ByteBuf responseBody() {
    ByteBuf responseBody = this.responseBody;
    if (responseBody == null) {
      var bodyFactory = config.responseBodyFactory;
      if (bodyFactory != null) {
        responseBody = bodyFactory.apply(this); // may null
      }
      if (responseBody == null) {
        // fallback
        responseBody = createResponseBody(config);
      }
      this.responseBody = responseBody;
    }
    return responseBody;
  }

  protected ByteBuf createResponseBody(NettyRequestConfig config) {
    return channel.alloc().ioBuffer(config.responseBodyInitialCapacity);
  }

  @Override
  public ServerHttpResponse asHttpOutputMessage() {
    ServerHttpResponse response = this.httpOutputMessage;
    if (response == null) {
      response = new NettyHttpOutputMessage();
      this.httpOutputMessage = response;
    }
    return response;
  }

  @Override
  public void flush() {
    writeHeaders();

    Object fileToSend;
    ByteBuf responseBody = this.responseBody;
    if (responseBody != null) {
      this.responseBody = null;
      channel.writeAndFlush(responseBody);
    }
    else if ((fileToSend = this.fileToSend) != null) {
      channel.writeAndFlush(fileToSend);
    }
  }

  @Override
  protected OutputStream createOutputStream() {
    return getMethod() == HttpMethod.HEAD ? new NoBodyOutputStream() : new ResponseBodyOutputStream();
  }

  @Override
  protected void requestCompletedInternal(@Nullable Throwable notHandled) {
    int cnt = request.refCnt();
    if (cnt != 0) {
      ReferenceCountUtil.safeRelease(request);
    }
    if (requestDecoder != null) {
      requestDecoder.destroy();
    }
    if (notHandled != null) {
      return;
    }

    flush();

    LastHttpContent lastHttpContent = LastHttpContent.EMPTY_LAST_CONTENT;
    // https://datatracker.ietf.org/doc/html/rfc7230#section-4.1.2
    // A trailer allows the sender to include additional fields at the end
    // of a chunked message in order to supply metadata that might be
    // dynamically generated while the message body is sent, such as a
    // message integrity check, digital signature, or post-processing
    // status.
    if (config.trailerHeadersConsumer != null && isTransferEncodingChunked(nettyResponseHeaders)) {
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
          config.trailerHeadersConsumer.accept(trailerHeaders);
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
      channel.writeAndFlush(lastHttpContent);
    }
    else {
      channel.writeAndFlush(lastHttpContent)
              .addListener(ChannelFutureListener.CLOSE);
    }

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
  protected void writeHeaders() {
    if (committed.compareAndSet(false, true)) {
      // ---------------------------------------------
      // apply Status code and headers
      // ---------------------------------------------
      HttpHeaders headers = nettyResponseHeaders;
      if (status != HttpResponseStatus.SWITCHING_PROTOCOLS) {
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
      }

      // apply cookies
      var responseCookies = this.responseCookies;
      if (responseCookies != null) {
        var cookieEncoder = config.cookieEncoder;
        for (HttpCookie cookie : responseCookies) {
          DefaultCookie nc = new DefaultCookie(cookie.getName(), cookie.getValue());
          if (cookie instanceof ResponseCookie rc) {
            nc.setPath(rc.getPath());
            nc.setDomain(rc.getDomain());
            nc.setMaxAge(rc.getMaxAge().getSeconds());
            nc.setSecure(rc.isSecure());
            nc.setHttpOnly(rc.isHttpOnly());
            nc.setSameSite(forSameSite(rc.getSameSite()));
            nc.setPartitioned(rc.isPartitioned());
          }

          headers.add(DefaultHttpHeaders.SET_COOKIE, cookieEncoder.encode(nc));
        }
      }

      channel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers));
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
  public void setStatus(int sc) {
    this.status = HttpResponseStatus.valueOf(sc);
  }

  @Override
  public void setStatus(HttpStatusCode status) {
    this.status = HttpResponseStatus.valueOf(status.value());
  }

  /**
   * Set response status
   *
   * @param status http status
   * @since 5.0
   */
  public void setStatus(HttpResponseStatus status) {
    this.status = status;
  }

  @Override
  public int getStatus() {
    return status.code();
  }

  @Override
  public void sendError(int sc) throws IOException {
    sendError(sc, null);
  }

  /**
   * Sends an HTTP error response to the client with the specified status code and
   * an optional error message. This method resets the current response state before
   * setting the new status code and delegating the error handling logic to the
   * configured {@code sendErrorHandler}.
   *
   * <p>This method is typically used to notify the client of an error condition,
   * such as a missing resource (404 Not Found) or an invalid request (400 Bad Request).
   * The optional error message can provide additional details about the error, which
   * may be logged or included in the response depending on the implementation of the
   * {@code sendErrorHandler}.
   *
   * <p><strong>Usage Example:</strong></p>
   * <pre>{@code
   * try {
   *   processRequest(request);
   * }
   * catch (InvalidInputException e) {
   *   sendError(HttpResponseStatus.BAD_REQUEST.code(), "Invalid input: " + e.getMessage());
   * }
   * }</pre>
   *
   * <p>In the example above, if the {@code processRequest} method throws an
   * {@code InvalidInputException}, the server responds with a 400 status code and
   * includes a descriptive error message.
   *
   * <p><strong>Handling Internal Server Errors:</strong></p>
   * <pre>{@code
   * try {
   *   executeCriticalOperation();
   * }
   * catch (Exception e) {
   *   sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "An unexpected error occurred");
   * }
   * }</pre>
   *
   * <p>Here, the method is used to handle unexpected exceptions by sending a 500
   * status code and a generic error message to the client.
   *
   * @param sc the HTTP status code to send (e.g., 404 for Not Found, 500 for Internal Server Error)
   * @param msg an optional error message describing the issue; may be {@code null}
   * if no specific message is available
   * @throws IOException if an I/O error occurs while sending the error response
   * @see SendErrorHandler#handleError(RequestContext, String)
   */
  @Override
  public void sendError(int sc, @Nullable String msg) throws IOException {
    reset();
    this.status = HttpResponseStatus.valueOf(sc);
    config.sendErrorHandler.handleError(this, msg);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final FullHttpRequest nativeRequest() {
    return request;
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
   * write result to client
   *
   * @param concurrentResult async result
   * @throws Throwable dispatch error
   */
  void dispatchConcurrentResult(@Nullable Object concurrentResult) throws Throwable {
    Object handler = WebAsyncManager.findHttpRequestHandler(this);
    dispatcherHandler.handleConcurrentResult(this, handler, concurrentResult);
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

  static void parseParameters(MultiValueMap<String, String> params, String s) {
    parseParameters(params, s, semicolonAsNormalChar);
  }

  @SuppressWarnings("fallthrough")
  static void parseParameters(MultiValueMap<String, String> params, String s, boolean semicolonAsNormalChar) {
    int paramsLimit = maxQueryParams;
    int nameStart = 0;
    int valueStart = -1;
    int i;
    int len = s.length();
    loop:
    for (i = 0; i < len; i++) {
      switch (s.charAt(i)) {
        case '=':
          if (nameStart == i) {
            nameStart = i + 1;
          }
          else if (valueStart < nameStart) {
            valueStart = i + 1;
          }
          break;
        case ';':
          if (semicolonAsNormalChar) {
            continue;
          }
          // fall through
        case '&':
          if (addParam(s, nameStart, valueStart, i, params)) {
            paramsLimit--;
            if (paramsLimit == 0) {
              return;
            }
          }
          nameStart = i + 1;
          break;
        case '#':
          break loop;
        default:
          // continue
      }
    }
    addParam(s, nameStart, valueStart, i, params);
  }

  private static boolean addParam(String s, int nameStart, int valueStart, int valueEnd, MultiValueMap<String, String> params) {
    if (nameStart >= valueEnd) {
      return false;
    }
    if (valueStart <= nameStart) {
      valueStart = valueEnd + 1;
    }
    String name = decodeComponent(s, nameStart, valueStart - 1, htmlQueryDecoding);
    String value = decodeComponent(s, valueStart, valueEnd, htmlQueryDecoding);
    params.add(name, value);
    return true;
  }

  private static String decodeComponent(String s, int from, int toExcluded, boolean plusToSpace) {
    int len = toExcluded - from;
    if (len <= 0) {
      return Constant.BLANK;
    }
    int firstEscaped = -1;
    for (int i = from; i < toExcluded; i++) {
      char c = s.charAt(i);
      if (c == '%' || (c == '+' && plusToSpace)) {
        firstEscaped = i;
        break;
      }
    }
    if (firstEscaped == -1) {
      return s.substring(from, toExcluded);
    }

    // Each encoded byte takes 3 characters (e.g. "%20")
    int decodedCapacity = (toExcluded - firstEscaped) / 3;
    byte[] buf = PlatformDependent.allocateUninitializedArray(decodedCapacity);
    int bufIdx;

    StringBuilder strBuf = new StringBuilder(len);
    strBuf.append(s, from, firstEscaped);

    for (int i = firstEscaped; i < toExcluded; i++) {
      char c = s.charAt(i);
      if (c != '%') {
        strBuf.append(c != '+' || !plusToSpace ? c : ' ');
        continue;
      }

      bufIdx = 0;
      do {
        if (i + 3 > toExcluded) {
          throw new IllegalArgumentException("unterminated escape sequence at index %d of: %s".formatted(i, s));
        }
        buf[bufIdx++] = decodeHexByte(s, i + 1);
        i += 3;
      }
      while (i < toExcluded && s.charAt(i) == '%');
      i--;

      strBuf.append(new String(buf, 0, bufIdx, StandardCharsets.UTF_8));
    }
    return strBuf.toString();
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
        throw new IllegalArgumentException("Trailer header name [%s] not declared with [Trailer] header, or it is not a valid trailer header name"
                .formatted(name));
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

    @Override
    public void flush() {
      NettyRequestContext.this.flush();
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
    public infra.http.HttpHeaders getHeaders() {
      return responseHeaders();
    }

    @Override
    public boolean supportsZeroCopy() {
      return true;
    }

    @Override
    public void sendFile(Path file, long position, long count) throws IOException {
      sendFile(file.toFile(), position, count);
    }

    @Override
    public void sendFile(File file, long position, long count) throws IOException {
      if (config.secure) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.context(CHUNKED_WRITER_NAME) == null) {
          pipeline.addLast(CHUNKED_WRITER_NAME, new ChunkedWriteHandler());
        }
        fileToSend = new ChunkedNioFile(FileChannel.open(file.toPath(), StandardOpenOption.READ),
                position, count, nioFileChunkSize);
      }
      else {
        fileToSend = new DefaultFileRegion(file, position, count);
      }
    }

  }

}
