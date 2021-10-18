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
package cn.taketoday.web.framework.reactive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.http.DefaultHttpHeaders;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.resolver.ParameterReadFailedException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.CombinedHttpHeaders;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

/**
 * @author TODAY 2019-07-04 21:24
 */
public class NettyRequestContext extends RequestContext {

  private String remoteAddress;

  private boolean committed = false;

  private final FullHttpRequest request;
  private final ChannelHandlerContext channelContext;
  private InterfaceHttpPostRequestDecoder requestDecoder;

  private final String uri; // none null

  private final NettyRequestContextConfig config;

  // response
  private Boolean keepAlive;

  private HttpResponseStatus status = HttpResponseStatus.OK;

  private ByteBuf responseBody;
  /**
   * response headers
   */
  private HttpHeaders originalResponseHeaders;

  private FullHttpResponse response;

  private final int queryStringIndex; // for optimize

  public NettyRequestContext(
          ChannelHandlerContext ctx, FullHttpRequest request, NettyRequestContextConfig config) {
    this.config = config;
    this.request = request;
    this.channelContext = ctx;
    String uri = request.uri();
    this.uri = uri;
    this.queryStringIndex = uri.indexOf('?');
  }

  @Override
  public String getScheme() {
    SocketAddress socketAddress = channelContext.channel().localAddress();
    if (socketAddress instanceof InetSocketAddress) {
      int port = ((InetSocketAddress) socketAddress).getPort();
      if (port == 443) {
        return Constant.HTTPS;
      }
    }
    return Constant.HTTP;
  }

  @Override
  protected final String doGetRequestPath() {
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
    String host = request.headers().get(HttpHeaderNames.HOST);
    return getScheme() + "://" + host + getRequestPath();
  }

  @Override
  public final String remoteAddress() {
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
  protected OutputStream doGetOutputStream() {
    return new ByteBufOutputStream(responseBody());
  }

  @Override
  protected cn.taketoday.web.http.HttpHeaders createRequestHeaders() {
    HttpHeaders headers = request.headers();
    DefaultHttpHeaders ret = new DefaultHttpHeaders();
    for (Map.Entry<String, String> header : headers) {
      ret.add(header.getKey(), header.getValue());
    }
    return ret;
  }

  @Override
  public String getContentType() {
    return request.headers().get(HttpHeaderNames.CONTENT_TYPE);
  }

  @Override
  protected NettyHttpHeaders createResponseHeaders() {
    return new NettyHttpHeaders(originalResponseHeaders());
  }

  @Override
  public HttpCookie[] doGetCookies() {
    List<String> allCookie = request.headers().getAll(HttpHeaderNames.COOKIE);
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
        ret[i++] = mapHttpCookie(cookie);
      }
      return ret;
    }
  }

  private HttpCookie mapHttpCookie(Cookie cookie) {
    HttpCookie ret = new HttpCookie(cookie.name(), cookie.value());
    ret.setPath(cookie.path());
    ret.setDomain(cookie.domain());
    ret.setMaxAge(cookie.maxAge());
    ret.setSecure(cookie.isSecure());
    ret.setHttpOnly(cookie.isHttpOnly());
    return ret;
  }

  @Override
  protected void postGetParameters(MultiValueMap<String, String> parameters) {
    super.postGetParameters(parameters);

    List<InterfaceHttpData> bodyHttpData = requestDecoder().getBodyHttpDatas();
    for (InterfaceHttpData data : bodyHttpData) {
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

  private InterfaceHttpPostRequestDecoder requestDecoder() {
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
    originalResponseHeaders().set(HttpHeaderNames.LOCATION, location);
    send();
  }

  /**
   * HTTP response body
   *
   * @return HTTP response body
   */
  public final ByteBuf responseBody() {
    ByteBuf responseBody = this.responseBody;
    if (responseBody == null) {
      Supplier<ByteBuf> bufSupplier = config.getResponseBody();
      if (bufSupplier != null) {
        responseBody = bufSupplier.get(); // may null
      }
      if (responseBody == null) {
        responseBody = Unpooled.buffer(config.getBodyInitialSize());
      }
      this.responseBody = responseBody;
    }
    return responseBody;
  }

  public void setKeepAlive(boolean keepAlive) {
    this.keepAlive = keepAlive;
  }

  private boolean isKeepAlive() {
    if (keepAlive == null) {
      this.keepAlive = HttpUtil.isKeepAlive(request);
    }
    return keepAlive;
  }

  private int readableBytes() {
    if (responseBody == null) {
      return 0;
    }
    return responseBody.readableBytes();
  }

  /**
   * Send HTTP message to the client
   */
  public void sendIfNotCommitted() {
    if (!committed) {
      send();
    }
  }

  /**
   * Send HTTP message to the client
   *
   * @throws IllegalStateException If the response has been committed
   */
  private void send() {
    assertNotCommitted();
    // obtain response object
    if (response == null) {
      ByteBuf responseBody = this.responseBody;
      if (responseBody == null) {
        responseBody = Unpooled.EMPTY_BUFFER;
      }
      this.response = new DefaultFullHttpResponse(
              config.getHttpVersion(),
              status,
              responseBody,
              originalResponseHeaders(),
              config.getTrailingHeaders().get()
      );
    }
    else {
      // apply HTTP status
      response.setStatus(status);
    }
    // set Content-Length header
    HttpHeaders responseHeaders = originalResponseHeaders(); // never null
    if (responseHeaders.get(HttpHeaderNames.CONTENT_LENGTH) == null) {
      responseHeaders.setInt(HttpHeaderNames.CONTENT_LENGTH, readableBytes());
    }
    // write response
    if (isKeepAlive()) {
      responseHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      channelContext.writeAndFlush(response);
    }
    else {
      channelContext.writeAndFlush(response)
              .addListener(ChannelFutureListener.CLOSE);
    }

    if (requestDecoder != null) {
      requestDecoder.destroy();
    }
    setCommitted(true);
  }

  @Override
  public void setContentLength(long length) {
    originalResponseHeaders().set(HttpHeaderNames.CONTENT_LENGTH, length);
  }

  @Override
  public boolean isCommitted() {
    return committed;
  }

  @Override
  protected void resetResponseHeader() {
    super.resetResponseHeader();
    if (originalResponseHeaders != null) {
      originalResponseHeaders.clear();
    }
  }

  @Override
  public void reset() {
    assertNotCommitted();
    resetResponseHeader();

    if (responseBody != null) {
      if (writer != null) {
        writer.flush(); // flush to responseBody
      }
      responseBody.clear();
    }
    status = HttpResponseStatus.OK;
  }

  /**
   * assert that response is committed?
   *
   * @throws IllegalStateException if response is committed
   */
  private void assertNotCommitted() {
    if (committed) {
      throw new IllegalStateException("The response has been committed");
    }
  }

  @Override
  public void addCookie(HttpCookie cookie) {
    super.addCookie(cookie);

    DefaultCookie nettyCookie = new DefaultCookie(cookie.getName(), cookie.getValue());
    nettyCookie.setPath(cookie.getPath());
    nettyCookie.setDomain(cookie.getDomain());
    nettyCookie.setMaxAge(cookie.getMaxAge());
    nettyCookie.setSecure(cookie.getSecure());
    nettyCookie.setHttpOnly(cookie.isHttpOnly());

    originalResponseHeaders().add(
            HttpHeaderNames.SET_COOKIE, config.getCookieEncoder().encode(nettyCookie));
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
  public int getStatus() {
    return status.code();
  }

  @Override
  public void sendError(int sc) {
    this.status = HttpResponseStatus.valueOf(sc);
    send();
  }

  @Override
  public void sendError(int sc, String msg) {
    this.status = HttpResponseStatus.valueOf(sc, msg);
    send();
  }

  @Override
  @SuppressWarnings("unchecked")
  public final FullHttpRequest nativeRequest() {
    return request;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final FullHttpResponse nativeResponse() {
    if (response == null) {
      this.response = new DefaultFullHttpResponse(
              config.getHttpVersion(),
              HttpResponseStatus.OK,
              responseBody(),
              originalResponseHeaders(),
              config.getTrailingHeaders().get()
      );
    }
    return response;
  }

  public HttpHeaders originalResponseHeaders() {
    if (originalResponseHeaders == null) {
      this.originalResponseHeaders = config.isSingleFieldHeaders()
                                     ? new io.netty.handler.codec.http.DefaultHttpHeaders(config.isValidateHeaders())
                                     : new CombinedHttpHeaders(config.isValidateHeaders());
    }
    return originalResponseHeaders;
  }

  @Override
  @Nullable
  public <T> T unwrapRequest(Class<T> requestClass) {
    if (requestClass.isInstance(request)) {
      return requestClass.cast(request);
    }
    return null;
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrapResponse(Class<T> responseClass) {
    Object ret = nativeResponse();
    if (responseClass.isInstance(ret)) {
      return (T) ret;
    }
    return null;
  }

  @Override
  public void flush() {
    channelContext.flush();
  }

  @Override
  protected MultiValueMap<String, MultipartFile> parseMultipartFiles() {
    DefaultMultiValueMap<String, MultipartFile> multipartFiles = new DefaultMultiValueMap<>();
    for (InterfaceHttpData data : requestDecoder().getBodyHttpDatas()) {
      if (data instanceof FileUpload) {
        String name = data.getName();
        multipartFiles.add(name, new FileUploadMultipartFile((FileUpload) data));
      }
    }
    return multipartFiles;
  }

  public void setCommitted(boolean committed) {
    this.committed = committed;
  }

  public ChannelHandlerContext getChannelContext() {
    return channelContext;
  }

  @Override
  protected String doGetContextPath() {
    return config.getContextPath();
  }

  @Override
  public String toString() {
    return "Netty " + super.toString();
  }

}
