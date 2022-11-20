/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.netty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.resolver.ParameterReadFailedException;
import cn.taketoday.web.context.async.AsyncWebRequest;
import cn.taketoday.web.multipart.MultipartRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.http.CombinedHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpContent;
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
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

/**
 * @author TODAY 2019-07-04 21:24
 */
public class NettyRequestContext extends RequestContext {
  private static final Logger log = LoggerFactory.getLogger(NettyRequestContext.class);

  @Nullable
  private String remoteAddress;

  // headers and status-code is written? default = false
  private final AtomicBoolean committed = new AtomicBoolean();

  private final FullHttpRequest request;
  private final ChannelHandlerContext channelContext;

  @Nullable
  private InterfaceHttpPostRequestDecoder requestDecoder;

  private final String uri; // none null

  private final NettyRequestConfig config;

  // response
  @Nullable
  private Boolean keepAlive;

  private HttpResponseStatus status = HttpResponseStatus.OK;

  @Nullable
  private ByteBuf responseBody;

  /**
   * response headers
   */
  private final HttpHeaders nettyResponseHeaders;

  private final int queryStringIndex; // for optimize

  @Nullable
  private InetSocketAddress inetSocketAddress;

  private final long requestTimeMillis = System.currentTimeMillis();

  public NettyRequestContext(
          ApplicationContext context, ChannelHandlerContext ctx,
          FullHttpRequest request, NettyRequestConfig config) {
    super(context);
    this.config = config;
    this.request = request;
    this.channelContext = ctx;
    String uri = request.uri();
    this.uri = uri;
    this.queryStringIndex = uri.indexOf('?');
    this.nettyResponseHeaders =
            config.isSingleFieldHeaders()
            ? new io.netty.handler.codec.http.DefaultHttpHeaders(config.isValidateHeaders())
            : new CombinedHttpHeaders(config.isValidateHeaders());
  }

  @Override
  public long getRequestTimeMillis() {
    return requestTimeMillis;
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
    int index = queryStringIndex;
    if (index == -1) {
      return uri;
    }
    else {
      return uri.substring(0, index);
    }
  }

  @Override
  public final String doGetQueryString() {
    int index = queryStringIndex;
    if (index == -1) {
      return Constant.BLANK;
    }
    else {
      return uri.substring(index + 1);
    }
  }

  @Override
  public String getRequestURL() {
    String host = request.headers().get(DefaultHttpHeaders.HOST);
    return getScheme() + "://" + host + StringUtils.formatURL(getRequestURI());
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
  protected cn.taketoday.http.HttpHeaders createRequestHeaders() {
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
  protected NettyHttpHeaders createResponseHeaders() {
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
    super.postGetParameters(parameters);

    if (isMultipart()) {
      for (InterfaceHttpData data : requestDecoder().getBodyHttpDatas()) {
        if (data instanceof Attribute) {
          try {
            String name = data.getName();
            parameters.add(name, ((Attribute) data).getValue());
          }
          catch (IOException e) {
            throw new ParameterReadFailedException("Netty http-data read failed", e);
          }
        }
      }
    }
  }

  InterfaceHttpPostRequestDecoder requestDecoder() {
    if (requestDecoder == null) {
      Charset charset = config.getPostRequestDecoderCharset();
      HttpDataFactory httpDataFactory = config.getHttpDataFactory();
      this.requestDecoder = new HttpPostRequestDecoder(httpDataFactory, request, charset);
      requestDecoder.setDiscardThreshold(0);
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

  /**
   * HTTP response body
   *
   * @return HTTP response body
   */
  public final ByteBuf responseBody() {
    ByteBuf responseBody = this.responseBody;
    if (responseBody == null) {
      Supplier<ByteBuf> bodyFactory = config.getResponseBodyFactory();
      if (bodyFactory != null) {
        responseBody = bodyFactory.get(); // may null
      }
      if (responseBody == null) {
        responseBody = channelContext.alloc().ioBuffer(config.getBodyInitialSize());
      }
      this.responseBody = responseBody;
    }
    return responseBody;
  }

  @Override
  public void flush() {
    writeHeaders();
    if (responseBody != null) {
      DefaultHttpContent httpContent = new DefaultHttpContent(responseBody);
      channelContext.writeAndFlush(httpContent);
      responseBody = null;
    }
  }

  @Override
  protected OutputStream doGetOutputStream() {
    return new ByteBufOutputStream();
  }

  final class ByteBufOutputStream extends OutputStream {

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

    }

  }

  @Override
  protected void postRequestCompleted() {
    flush();

    Consumer<? super HttpHeaders> trailerHeadersConsumer = config.getTrailerHeadersConsumer();
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

    ChannelFuture future = channelContext.writeAndFlush(lastHttpContent);
    if (!isKeepAlive()) {
      future.addListener(ChannelFutureListener.CLOSE);
    }

    if (requestDecoder != null) {
      requestDecoder.destroy();
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
  public void writeHeaders() {
    if (committed.compareAndSet(false, true)) {
      // ---------------------------------------------
      // apply Status code and headers
      // ---------------------------------------------
      HttpHeaders responseHeaders = nettyResponseHeaders;
      // set Content-Length header
      String contentType = getResponseContentType();
      if (MediaType.TEXT_EVENT_STREAM_VALUE.equals(contentType)) {
        responseHeaders.set(DefaultHttpHeaders.TRANSFER_ENCODING, DefaultHttpHeaders.CHUNKED);
        responseHeaders.remove(DefaultHttpHeaders.CONTENT_LENGTH);
      }
      else if (!isTransferEncodingChunked(responseHeaders)) {
        if (responseHeaders.get(DefaultHttpHeaders.CONTENT_LENGTH) == null) {
          ByteBuf responseBody = this.responseBody;
          if (responseBody == null) {
            responseHeaders.setInt(DefaultHttpHeaders.CONTENT_LENGTH, 0);
          }
          else {
            responseHeaders.setInt(DefaultHttpHeaders.CONTENT_LENGTH, responseBody.readableBytes());
          }
        }
      }

      // apply cookies
      ArrayList<HttpCookie> responseCookies = this.responseCookies;
      if (responseCookies != null) {
        String setCookie = DefaultHttpHeaders.SET_COOKIE;
        ServerCookieEncoder cookieEncoder = config.getCookieEncoder();
        for (HttpCookie cookie : responseCookies) {
          DefaultCookie nettyCookie = new DefaultCookie(cookie.getName(), cookie.getValue());
          if (cookie instanceof ResponseCookie responseCookie) {
            nettyCookie.setPath(responseCookie.getPath());
            nettyCookie.setDomain(responseCookie.getDomain());
            nettyCookie.setMaxAge(responseCookie.getMaxAge().getSeconds());
            nettyCookie.setSecure(responseCookie.isSecure());
            nettyCookie.setHttpOnly(responseCookie.isHttpOnly());
          }

          responseHeaders.add(setCookie, cookieEncoder.encode(nettyCookie));
        }
      }

      // write response
      HttpVersion httpVersion = config.getHttpVersion();
      if (isKeepAlive()) {
        HttpUtil.setKeepAlive(responseHeaders, httpVersion, true);
      }

      var noBody = new DefaultHttpResponse(httpVersion, status, responseHeaders);
      channelContext.write(noBody);
    }

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

    if (responseBody != null) {
      if (writer != null) {
        writer.flush(); // flush to responseBody
      }
      responseBody.release();
      responseBody = null;
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
  public void setStatus(int status, String message) {
    this.status = new HttpResponseStatus(status, message);
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
  public void sendError(int sc) {
    this.status = HttpResponseStatus.valueOf(sc);
    commit();
  }

  @Override
  public void sendError(int sc, String msg) {
    this.status = HttpResponseStatus.valueOf(sc, msg);
    commit();
  }

  @Override
  @SuppressWarnings("unchecked")
  public final FullHttpRequest nativeRequest() {
    return request;
  }

  public HttpHeaders getNettyResponseHeaders() {
    return nettyResponseHeaders;
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

  public ChannelHandlerContext getChannelContext() {
    return channelContext;
  }

  @Override
  protected String doGetContextPath() {
    return config.getContextPath();
  }

  static final class TrailerHeaders extends io.netty.handler.codec.http.DefaultHttpHeaders {

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

    static Set<String> filterHeaderNames(String declaredHeaderNames) {
      Objects.requireNonNull(declaredHeaderNames, "declaredHeaderNames");
      Set<String> result = new HashSet<>();
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

    static final class TrailerNameValidator implements DefaultHeaders.NameValidator<CharSequence> {

      /**
       * Contains the headers names specified with {@link DefaultHttpHeaders#TRAILER}
       */
      final Set<String> declaredHeaderNames;

      TrailerNameValidator(Set<String> declaredHeaderNames) {
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
  }

}
