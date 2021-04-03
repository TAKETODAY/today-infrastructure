/**
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
package cn.taketoday.framework.reactive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.web.AbstractRequestContext;
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
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

import static cn.taketoday.context.Constant.DEFAULT_CHARSET;

/**
 * @author TODAY <br>
 * 2019-07-04 21:24
 */
public class NettyRequestContext
        extends AbstractRequestContext implements RequestContext {
//    private static final Logger log = LoggerFactory.getLogger(NettyRequestContext.class);

  private static final HttpDataFactory HTTP_DATA_FACTORY = new DefaultHttpDataFactory(true);

  private String queryString;
  private String remoteAddress;

  private boolean committed = false;

  private Map<String, String[]> parameters;

  private final FullHttpRequest request;
  private final ChannelHandlerContext channelContext;
  private InterfaceHttpPostRequestDecoder requestDecoder;

  private final String uri;
  private String requestURI;

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

  public NettyRequestContext(ChannelHandlerContext ctx,
                             FullHttpRequest request,
                             NettyRequestContextConfig config) {
    this.config = config;
    this.request = request;
    this.channelContext = ctx;
    this.uri = request.uri();
  }

  @Override
  public String getRequestURI() {
    String requestURI = this.requestURI;
    if (requestURI == null) {
      final String uri = this.uri;
      final int index = uri.indexOf('?');
      if (index > -1) {
        requestURI = uri.substring(0, index);
      }
      else {
        requestURI = uri;
      }
      this.requestURI = requestURI;
    }
    return requestURI;
  }

  @Override
  public String getRequestURL() {
    final String host = request.headers().get(Constant.HOST);
    return "http://" + host + getRequestURI();
  }

  @Override
  public String remoteAddress() {
    if (remoteAddress == null) {
      final InetSocketAddress remote = (InetSocketAddress) channelContext.channel().remoteAddress();
      remoteAddress = remote.getAddress().getHostAddress();
    }
    return remoteAddress;
  }

  @Override
  public String getQueryString() {
    String queryString = this.queryString;
    if (queryString == null) {
      final int index;
      final String uri = this.uri;
      if (uri == null || (index = uri.indexOf('?')) == -1) {
        queryString = Constant.BLANK;
      }
      else {
        queryString = uri.substring(index + 1);
      }
      this.queryString = queryString;
    }
    return queryString;
  }

  @Override
  public String getMethod() {
    return this.request.method().name();
  }

  @Override
  protected InputStream getInputStreamInternal() {
    return new ByteBufInputStream(request.content());
  }

  @Override
  protected OutputStream getOutputStreamInternal() {
    return new ByteBufOutputStream(responseBody());
  }

  @Override
  protected cn.taketoday.web.http.HttpHeaders createRequestHeaders() {
    final HttpHeaders headers = request.headers();
    final DefaultHttpHeaders ret = new DefaultHttpHeaders();
    for (final Map.Entry<String, String> header : headers) {
      ret.add(header.getKey(), header.getValue());
    }
    return ret;
  }

  @Override
  public void applyHeaders() {
    // noop; netty auto apply headers
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
  public HttpCookie[] getCookiesInternal() {
    final String header = request.headers().get(HttpHeaderNames.COOKIE);
    if (StringUtils.isEmpty(header)) {
      return EMPTY_COOKIES;
    }

    final Set<Cookie> parsed = config.getCookieDecoder().decode(header);
    return ObjectUtils.isEmpty(parsed)
           ? EMPTY_COOKIES
           : parsed.stream().map(this::mapHttpCookie).toArray(HttpCookie[]::new);
  }

  private HttpCookie mapHttpCookie(final Cookie cookie) {
    final HttpCookie ret = new HttpCookie(cookie.name(), cookie.value());
    ret.setPath(cookie.path());
    ret.setDomain(cookie.domain());
    ret.setMaxAge(cookie.maxAge());
    ret.setSecure(cookie.isSecure());
    ret.setHttpOnly(cookie.isHttpOnly());
    return ret;
  }

  @Override
  public Map<String, String[]> getParameters() {
    Map<String, String[]> params = this.parameters;
    if (params == null) {
      params = parseParameters();
      this.parameters = params;
    }
    return params;
  }

  protected Map<String, String[]> parseParameters() {

    final String queryString = getQueryString();
    final Map<String, List<String>> parameters = StringUtils.isNotEmpty(queryString)
                                                 ? StringUtils.parseParameters(queryString)
                                                 : new HashMap<>();

    final List<InterfaceHttpData> bodyHttpData = getRequestDecoder().getBodyHttpDatas();
    for (final InterfaceHttpData data : bodyHttpData) {
      if (data instanceof Attribute) {
        try {
          final String name = data.getName();
          List<String> list = parameters.get(name);
          if (list == null) {
            parameters.put(name, list = new ArrayList<>(1));
          }
          list.add(((Attribute) data).getValue());
        }
        catch (IOException e) {
          throw new ParameterReadFailedException("Netty http-data read failed", e);
        }
      }
    }

    if (!parameters.isEmpty()) {
      final Map<String, String[]> params = new HashMap<>(parameters.size());
      for (final Map.Entry<String, List<String>> entry : parameters.entrySet()) {
        final List<String> value = entry.getValue();
        params.put(entry.getKey(), value.toArray(new String[value.size()]));
      }
      return params;
    }
    return Collections.emptyMap();
  }

  private InterfaceHttpPostRequestDecoder getRequestDecoder() {
    InterfaceHttpPostRequestDecoder requestDecoder = this.requestDecoder;
    if (requestDecoder == null) {
      requestDecoder = new HttpPostRequestDecoder(HTTP_DATA_FACTORY, this.request, DEFAULT_CHARSET);
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
    status = HttpResponseStatus.FOUND;
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
      final Supplier<ByteBuf> bufSupplier = config.getResponseBody();
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
    Boolean keepAlive = this.keepAlive;
    if (keepAlive == null) {
      return this.keepAlive = HttpUtil.isKeepAlive(request);
    }
    return keepAlive;
  }

  private int readableBytes() {
    ByteBuf responseBody = this.responseBody;
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
   * @throws IllegalStateException
   *         If the response has been committed
   */
  private void send() {
    assertNotCommitted();
    // obtain response object
    FullHttpResponse response = this.response;
    if (response == null) {
      // flush writer's content to responseBody
      if (writer != null) {
        writer.flush();
      }
      ByteBuf responseBody = this.responseBody;
      if (responseBody == null) {
        responseBody = Unpooled.EMPTY_BUFFER;
      }
      response = new DefaultFullHttpResponse(
              config.getHttpVersion(), status, responseBody,
              originalResponseHeaders(), config.getTrailingHeaders().get());
    }
    else {
      // apply HTTP status
      response.setStatus(status);
      // flush writer
      if (writer != null) {
        writer.flush();
      }
    }
    // set Content-Length header
    final HttpHeaders responseHeaders = originalResponseHeaders(); // never null
    if (responseHeaders.get(HttpHeaderNames.CONTENT_LENGTH) == null) {
      responseHeaders.setInt(HttpHeaderNames.CONTENT_LENGTH, readableBytes());
    }
    // write response
    final ChannelHandlerContext context = this.channelContext;
    if (isKeepAlive()) {
      responseHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      context.writeAndFlush(response, context.voidPromise());
    }
    else {
      context.writeAndFlush(response, context.voidPromise())
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
  public boolean committed() {
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

  private void assertNotCommitted() {
    if (committed) {
      throw new IllegalStateException("The response has been committed");
    }
  }

  @Override
  public void addCookie(HttpCookie cookie) {
    final Cookie c = new DefaultCookie(cookie.getName(), cookie.getValue());
    c.setPath(cookie.getPath());
    c.setDomain(cookie.getDomain());
    c.setMaxAge(cookie.getMaxAge());
    c.setSecure(cookie.getSecure());
    c.setHttpOnly(cookie.isHttpOnly());

    originalResponseHeaders().add(
            HttpHeaderNames.SET_COOKIE, config.getCookieEncoder().encode(c)
    );
  }

  @Override
  public void setStatus(final int sc) {
    status = HttpResponseStatus.valueOf(sc);
  }

  @Override
  public void setStatus(final int status, final String message) {
    this.status = new HttpResponseStatus(status, message);
  }

  @Override
  public int getStatus() {
    return status.code();
  }

  @Override
  public void sendError(int sc) {
    status = HttpResponseStatus.valueOf(sc);
    send();
  }

  @Override
  public void sendError(int sc, String msg) {
    status = HttpResponseStatus.valueOf(sc, msg);
    send();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T nativeRequest() {
    return (T) request;
  }

  @Override
  public <T> T nativeRequest(Class<T> requestClass) {
    if (requestClass.isInstance(request)) {
      return requestClass.cast(request);
    }
    throw new ConfigurationException("The runtime request is not a: [" + requestClass + "] object");
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T nativeResponse() {
    return (T) getResponse();
  }

  public final FullHttpRequest getRequest() {
    return request;
  }

  private HttpHeaders originalResponseHeaders() {
    HttpHeaders originalResponseHeaders = this.originalResponseHeaders;
    if (originalResponseHeaders == null) {
      final NettyRequestContextConfig config = this.config;
      originalResponseHeaders = config.isSingleFieldHeaders()
                                ? new io.netty.handler.codec.http.DefaultHttpHeaders(config.isValidateHeaders())
                                : new CombinedHttpHeaders(config.isValidateHeaders());
      this.originalResponseHeaders = originalResponseHeaders;
    }
    return originalResponseHeaders;
  }

  public final FullHttpResponse getResponse() {
    FullHttpResponse response = this.response;
    if (response == null) {
      final NettyRequestContextConfig config = this.config;
      response = new DefaultFullHttpResponse(config.getHttpVersion(),
                                             HttpResponseStatus.OK,
                                             responseBody(),
                                             originalResponseHeaders(),
                                             config.getTrailingHeaders().get());
      this.response = response;
    }
    return response;
  }

  @Override
  public <T> T nativeResponse(Class<T> responseClass) {
    final T ret = nativeResponse();
    if (responseClass.isInstance(ret)) {
      return ret;
    }
    throw new ConfigurationException("The runtime response is not a: [" + responseClass + "] object");
  }

  @Override
  public void flush() throws IOException {
    channelContext.flush();
  }

  @Override
  protected Map<String, List<MultipartFile>> parseMultipartFiles() {
    final HashMap<String, List<MultipartFile>> multipartFiles = new HashMap<>();

    for (InterfaceHttpData data : getRequestDecoder().getBodyHttpDatas()) {
      if (data instanceof FileUpload) {
        final String name = data.getName();
        List<MultipartFile> parts = multipartFiles.get(name);
        if (parts == null) {
          multipartFiles.put(name, parts = new ArrayList<>(2));
        }
        parts.add(new FileUploadMultipartFile((FileUpload) data));
      }
    }
    return multipartFiles;
  }

  public void setCommitted(boolean committed) {
    this.committed = committed;
  }

  @Override
  protected String getContextPathInternal() {
    return config.getContextPath();
  }

}
