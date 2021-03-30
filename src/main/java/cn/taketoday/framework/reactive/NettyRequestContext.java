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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.web.AbstractRequestContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.http.DefaultHttpHeaders;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.resolver.ParameterReadFailedException;
import cn.taketoday.web.ui.Model;
import cn.taketoday.web.ui.RedirectModel;
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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
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
        extends AbstractRequestContext implements RequestContext, Map<String, Object> {

//    private static final Logger log = LoggerFactory.getLogger(NettyRequestContext.class);

  private final String url;
  private String queryString;
  private String remoteAddress;

  private boolean committed = false;

  private Map<String, Object> attributes;
  private Map<String, String[]> parameters;

  private final FullHttpRequest request;
  private final ChannelHandlerContext handlerContext;

  private String uri;

  private final NettyRequestContextConfig config;

  public NettyRequestContext(String contextPath,
                             ChannelHandlerContext ctx,
                             FullHttpRequest request,
                             NettyRequestContextConfig config) {
    this.request = request;
    this.handlerContext = ctx;
    this.url = request.uri();
    this.uri = request.uri();//TODO
    this.config = config;
    setContextPath(contextPath);
  }

  @Override
  public String requestURI() {
    return uri;
  }

  @Override
  public String requestURL() {
    return request.uri();
  }

  @Override
  public String remoteAddress() {
    return this.remoteAddress;
  }

  @Override
  public String queryString() {
    final String queryString = this.queryString;
    if (queryString == null) {
      final String url = this.url;
      int index;
      if (url == null || (index = url.indexOf('?')) == -1) {
        return Constant.BLANK;
      }
      return this.queryString = url.substring(index + 1);
    }
    return queryString;
  }

  @Override
  public String method() {
    return this.request.method().name();
  }

  @Override
  protected InputStream getInputStreamInternal() {
    return new ByteBufInputStream(request.content());
  }

  @Override
  protected OutputStream getOutputStreamInternal() {
    return new ByteBufOutputStream(responseBody);
  }

  public Map<String, Object> getAttributes() {
    final Map<String, Object> attributes = this.attributes;
    if (attributes == null) {
      return this.attributes = new HashMap<>();
    }
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  @Override
  public Model attributes(Map<String, Object> attributes) {
    getAttributes().putAll(attributes);
    return this;
  }

  @Override
  public Enumeration<String> attributes() {
    return Collections.enumeration(getAttributes().keySet());
  }

  @Override
  public Object attribute(String name) {
    return getAttributes().get(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T attribute(String name, Class<T> targetClass) {
    return (T) ConvertUtils.convert(getAttributes().get(name), targetClass);
  }

  @Override
  public Model attribute(String name, Object value) {
    getAttributes().put(name, value);
    return this;
  }

  @Override
  public Model removeAttribute(String name) {
    getAttributes().remove(name);
    return this;
  }

  @Override
  public Map<String, Object> asMap() {
    return attributes;
  }

  @Override
  protected cn.taketoday.web.http.HttpHeaders createRequestHeaders() {
    final HttpHeaders headers = request.headers();
    final DefaultHttpHeaders ret = new DefaultHttpHeaders();
    for (final Entry<String, String> header : headers) {
      ret.add(header.getKey(), header.getValue());
    }
    return ret;
  }

  @Override
  public void applyHeaders() {
    // noop; netty auto apply headers
  }

  @Override
  public String contentType() {
    return request.headers().get(HttpHeaderNames.CONTENT_TYPE);
  }

  @Override
  public NettyHttpHeaders responseHeaders() {
    return (NettyHttpHeaders) super.responseHeaders();
  }

  @Override
  protected NettyHttpHeaders createResponseHeaders() {
    final HttpHeaders responseHeaders = config.isSingleFieldHeaders()
                                        ? new io.netty.handler.codec.http.DefaultHttpHeaders(config.isValidateHeaders())
                                        : new CombinedHttpHeaders(config.isValidateHeaders());
    return new NettyHttpHeaders(responseHeaders);
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

  private static final HttpDataFactory HTTP_DATA_FACTORY = new DefaultHttpDataFactory(true);

  private InterfaceHttpPostRequestDecoder requestDecoder;

  @Override
  public Map<String, String[]> parameters() {
    final Map<String, String[]> params = this.parameters;
    if (params != null) {
      return params;
    }
    return this.parameters = parseParameters();
  }

  protected Map<String, String[]> parseParameters() {

    final String queryString = queryString();
    final Map<String, List<String>> parameters = StringUtils.isNotEmpty(queryString)
                                                 ? StringUtils.parseParameters(queryString)
                                                 : new HashMap<>();

    final List<InterfaceHttpData> bodyHttpDatas = getRequestDecoder().getBodyHttpDatas();
    for (final InterfaceHttpData data : bodyHttpDatas) {

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
      for (final Entry<String, List<String>> entry : parameters.entrySet()) {
        final List<String> value = entry.getValue();
        params.put(entry.getKey(), value.toArray(new String[value.size()]));
      }
      return params;
    }
    return Collections.emptyMap();
  }

  protected InterfaceHttpPostRequestDecoder getRequestDecoder() {
    InterfaceHttpPostRequestDecoder requestDecoder = this.requestDecoder;
    if (requestDecoder == null) {
      requestDecoder = new HttpPostRequestDecoder(HTTP_DATA_FACTORY, this.request, DEFAULT_CHARSET);
//            requestDecoder = WebUtils.isMultipart(this)
//                    ? new HttpPostMultipartRequestDecoder(HTTP_DATA_FACTORY, request.retain(), DEFAULT_CHARSET)
//                    : new HttpPostStandardRequestDecoder(HTTP_DATA_FACTORY, request.retain(), DEFAULT_CHARSET);

      requestDecoder.setDiscardThreshold(0);
      return this.requestDecoder = requestDecoder;
    }
    return requestDecoder;
  }

  @Override
  public long contentLength() {
    return request.content().readableBytes();
  }

  @Override
  public RedirectModel redirectModel() {
    return null;
  }

  @Override
  public RedirectModel applyRedirectModel(RedirectModel redirectModel) {

    //TODO
    return redirectModel;
  }

  @Override
  public RequestContext redirect(String location) {
    assertNotCommitted();

    getResponse().setStatus(HttpResponseStatus.FOUND);
    responseHeaders().set(Constant.LOCATION, location);

    send();
    return this;
  }

  private final ByteBuf responseBody = Unpooled.buffer(16);

  public final ByteBuf responseBody() {
    return responseBody;
  }

  /**
   * Send HTTP message to the client
   */
  public void send() {
    assertNotCommitted();
    final NettyHttpHeaders responseHeaders = responseHeaders();
    if (responseHeaders.getFirst(Constant.CONTENT_LENGTH) == null) {
      responseHeaders.setContentLength(responseBody.readableBytes());
    }
    final ChannelHandlerContext handlerContext = this.handlerContext;
    if (handlerContext != null) {
      if (config.isKeepAliveWhenSending()) {
        responseHeaders.setConnection(Constant.KEEP_ALIVE);
      }
      else {
        handlerContext.write(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
      }
      handlerContext.writeAndFlush(getResponse());
    }
    final InterfaceHttpPostRequestDecoder requestDecoder = this.requestDecoder;
    if (requestDecoder != null) {
      requestDecoder.destroy();
    }
    setCommitted(true);
  }

  @Override
  public RequestContext contentLength(long length) {
    responseHeaders().setContentLength(length);
    return this;
  }

  @Override
  public boolean committed() {
    return committed;
  }

  @Override
  public RequestContext reset() {
    assertNotCommitted();

    resetResponseHeader();

    responseBody.clear();
    getResponse().setStatus(HttpResponseStatus.OK);
    return this;
  }

  protected void assertNotCommitted() {
    if (committed()) {
      throw new IllegalStateException("The response has been committed");
    }
  }

  @Override
  public RequestContext addCookie(HttpCookie cookie) {

    final Cookie c = new DefaultCookie(cookie.getName(), cookie.getValue());
    c.setPath(cookie.getPath());
    c.setDomain(cookie.getDomain());
    c.setMaxAge(cookie.getMaxAge());
    c.setSecure(cookie.getSecure());
    c.setHttpOnly(cookie.isHttpOnly());

    responseHeaders().add(
            Constant.SET_COOKIE, config.getCookieEncoder().encode(c)
    );
    return this;
  }

  @Override
  public RequestContext status(int sc) {
    getResponse().setStatus(HttpResponseStatus.valueOf(sc));
    return this;
  }

  @Override
  public RequestContext status(final int status, final String message) {
    getResponse().setStatus(new HttpResponseStatus(status, message));
    return this;
  }

  @Override
  public int status() {
    return getResponse().status().code();
  }

  @Override
  public RequestContext sendError(int sc) {
    assertNotCommitted();

    final FullHttpResponse response = getResponse();
    response.setStatus(HttpResponseStatus.valueOf(sc));

    handlerContext.writeAndFlush(response);
    setCommitted(true);
    return this;
  }

  @Override
  public RequestContext sendError(int sc, String msg) {
    assertNotCommitted();

    final FullHttpResponse response = getResponse();
    response.setStatus(HttpResponseStatus.valueOf(sc, msg));

    handlerContext.writeAndFlush(response);
    setCommitted(true);
    return this;
  }

  @Override
  public <T> T nativeSession() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T nativeSession(Class<T> sessionClass) {
    throw new UnsupportedOperationException();
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

  private FullHttpResponse response;

  @Override
  @SuppressWarnings("unchecked")
  public <T> T nativeResponse() {
    return (T) getResponse();
  }

  public final FullHttpRequest getRequest() {
    return request;
  }

  public final FullHttpResponse getResponse() {
    final FullHttpResponse response = this.response;
    if (response == null) {
      final NettyRequestContextConfig config = this.config;
      final DefaultFullHttpResponse httpResponse =
              new DefaultFullHttpResponse(config.getHttpVersion(), HttpResponseStatus.OK,
                                          responseBody, responseHeaders().getOriginal(), config.getTrailingHeaders().get());

      return this.response = httpResponse;

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
    handlerContext.flush();
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

  // Map
  // -----------------------------------------

  @Override
  public int size() {
    return getAttributes().size();
  }

  @Override
  public boolean isEmpty() {
    return getAttributes().isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return getAttributes().containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return getAttributes().containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return getAttributes().get(key);
  }

  @Override
  public Object put(String key, Object value) {
    return getAttributes().put(key, value);
  }

  @Override
  public Object remove(Object key) {
    return getAttributes().remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ?> m) {
    getAttributes().putAll(m);
  }

  @Override
  public Set<String> keySet() {
    return getAttributes().keySet();
  }

  @Override
  public Collection<Object> values() {
    return getAttributes().values();
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return getAttributes().entrySet();
  }

  @Override
  public void clear() {
    getAttributes().clear();
  }

  // -------------------------------

  public void setCommitted(boolean committed) {
    this.committed = committed;
  }

}
