/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.handler.function;

import java.io.IOException;
import java.io.Serial;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRange;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.GenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextDecorator;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.multipart.Multipart;
import cn.taketoday.web.util.UriBuilder;
import cn.taketoday.web.util.UriComponentsBuilder;

/**
 * {@code ServerRequest} implementation based on a {@link RequestContext}.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultServerRequest implements ServerRequest {

  private final Headers headers;
  private final RequestPath requestPath;
  private final RequestContext requestContext;
  private final Map<String, Object> attributes;
  private final MultiValueMap<String, String> params;
  private final List<HttpMessageConverter<?>> messageConverters;

  @Nullable
  private MultiValueMap<String, Multipart> parts;

  DefaultServerRequest(RequestContext requestContext, List<HttpMessageConverter<?>> messageConverters) {
    this.requestContext = requestContext;
    this.requestPath = requestContext.getRequestPath();
    this.messageConverters = List.copyOf(messageConverters);
    this.attributes = new ServletAttributesMap(requestContext);
    this.headers = new DefaultRequestHeaders(requestContext.getHeaders());
    this.params = MultiValueMap.from(new ServletParametersMap(requestContext));
  }

  @Override
  public HttpMethod method() {
    return requestContext.getMethod();
  }

  @Override
  @Deprecated
  public String methodName() {
    return requestContext.getMethodValue();
  }

  @Override
  public URI uri() {
    return requestContext.getURI();
  }

  @Override
  public UriBuilder uriBuilder() {
    return UriComponentsBuilder.fromHttpRequest(requestContext);
  }

  @Override
  public RequestPath requestPath() {
    return this.requestPath;
  }

  @Override
  public Headers headers() {
    return this.headers;
  }

  @Override
  public MultiValueMap<String, HttpCookie> cookies() {
    HttpCookie[] cookies = requestContext.getCookies();
    if (cookies == null) {
      cookies = new HttpCookie[0];
    }
    MultiValueMap<String, HttpCookie> result = new LinkedMultiValueMap<>(cookies.length);
    for (HttpCookie cookie : cookies) {
      result.add(cookie.getName(), cookie);
    }
    return result;
  }

  @Override
  public RequestContext requestContext() {
    return requestContext;
  }

  @Override
  public Optional<InetSocketAddress> remoteAddress() {
    return Optional.of(new InetSocketAddress(
            requestContext.getRemoteAddress(), requestContext.getServerPort()));
  }

  @Override
  public List<HttpMessageConverter<?>> messageConverters() {
    return this.messageConverters;
  }

  @Override
  public <T> T body(Class<T> bodyType) throws IOException {
    return bodyInternal(bodyType, bodyType);
  }

  @Override
  public <T> T body(ParameterizedTypeReference<T> bodyType) throws IOException {
    Type type = bodyType.getType();
    return bodyInternal(type, bodyClass(type));
  }

  static Class<?> bodyClass(Type type) {
    if (type instanceof Class<?> clazz) {
      return clazz;
    }
    if (type instanceof ParameterizedType parameterizedType &&
            parameterizedType.getRawType() instanceof Class<?> rawType) {
      return rawType;
    }
    return Object.class;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <T> T bodyInternal(Type bodyType, Class bodyClass) throws IOException {
    MediaType contentType = this.headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);

    for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
      if (messageConverter instanceof GenericHttpMessageConverter<?> genericMessageConverter) {
        if (genericMessageConverter.canRead(bodyType, bodyClass, contentType)) {
          return (T) genericMessageConverter.read(bodyType, bodyClass, requestContext);
        }
      }
      if (messageConverter.canRead(bodyClass, contentType)) {
        return (T) messageConverter.read(bodyClass, requestContext);
      }
    }
    throw new HttpMediaTypeNotSupportedException(contentType, getSupportedMediaTypes(bodyClass), method());
  }

  private List<MediaType> getSupportedMediaTypes(Class<?> bodyClass) {
    List<MediaType> result = new ArrayList<>(this.messageConverters.size());
    for (HttpMessageConverter<?> converter : this.messageConverters) {
      result.addAll(converter.getSupportedMediaTypes(bodyClass));
    }
    MimeTypeUtils.sortBySpecificity(result);
    return result;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T bind(Class<T> bindType, Consumer<WebDataBinder> dataBinderCustomizer) throws BindException {
    Assert.notNull(bindType, "BindType must not be null");
    Assert.notNull(dataBinderCustomizer, "DataBinderCustomizer must not be null");

    WebDataBinder dataBinder = new WebDataBinder(null);
    dataBinder.setTargetType(ResolvableType.forClass(bindType));
    dataBinderCustomizer.accept(dataBinder);

    RequestContext context = requestContext();
    dataBinder.construct(context);
    dataBinder.bind(context);

    BindingResult bindingResult = dataBinder.getBindingResult();
    if (bindingResult.hasErrors()) {
      throw new BindException(bindingResult);
    }
    else {
      T result = (T) bindingResult.getTarget();
      if (result != null) {
        return result;
      }
      else {
        throw new IllegalStateException("Binding result has neither target nor errors");
      }
    }
  }

  @Override
  public Optional<Object> attribute(String name) {
    return Optional.ofNullable(requestContext.getAttribute(name));
  }

  @Override
  public Map<String, Object> attributes() {
    return this.attributes;
  }

  @Override
  public Optional<String> param(String name) {
    return Optional.ofNullable(requestContext.getParameter(name));
  }

  @Override
  public List<String> params(String name) {
    List<String> paramValues = params.get(name);
    if (CollectionUtils.isEmpty(paramValues)) {
      return Collections.emptyList();
    }
    return paramValues;
  }

  @Override
  public MultiValueMap<String, String> params() {
    return this.params;
  }

  @Override
  public MultiValueMap<String, Multipart> multipartData() throws IOException {
    MultiValueMap<String, Multipart> result = this.parts;
    if (result == null) {
      result = requestContext.getMultipartRequest().multipartData();
      this.parts = result;
    }
    return result;
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Map<String, String> pathVariables() {
    if (requestContext.getAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE)
            instanceof Map pathVariables) {
      return pathVariables;
    }
    return Collections.emptyMap();
  }

  @Override
  public String toString() {
    return String.format("HTTP %s %s", method(), path());
  }

  static Optional<ServerResponse> checkNotModified(
          RequestContext context, @Nullable Instant lastModified, @Nullable String etag) {

    long lastModifiedTimestamp = -1;
    if (lastModified != null && lastModified.isAfter(Instant.EPOCH)) {
      lastModifiedTimestamp = lastModified.toEpochMilli();
    }
    var response = new CheckNotModifiedResponse(context);
    if (response.checkNotModified(etag, lastModifiedTimestamp)) {
      return Optional.of(
              ServerResponse.status(response.status)
                      .headers(headers -> headers.addAll(response.headers))
                      .build()
      );
    }
    else {
      return Optional.empty();
    }
  }

  /**
   * Default implementation of {@link Headers}.
   */
  static class DefaultRequestHeaders implements Headers {

    private final HttpHeaders httpHeaders;

    public DefaultRequestHeaders(HttpHeaders httpHeaders) {
      this.httpHeaders = HttpHeaders.readOnlyHttpHeaders(httpHeaders);
    }

    @Override
    public List<MediaType> accept() {
      return this.httpHeaders.getAccept();
    }

    @Override
    public List<Charset> acceptCharset() {
      return this.httpHeaders.getAcceptCharset();
    }

    @Override
    public List<Locale.LanguageRange> acceptLanguage() {
      return this.httpHeaders.getAcceptLanguage();
    }

    @Override
    public OptionalLong contentLength() {
      long value = this.httpHeaders.getContentLength();
      return (value != -1 ? OptionalLong.of(value) : OptionalLong.empty());
    }

    @Override
    public Optional<MediaType> contentType() {
      return Optional.ofNullable(this.httpHeaders.getContentType());
    }

    @Override
    public InetSocketAddress host() {
      return this.httpHeaders.getHost();
    }

    @Override
    public List<HttpRange> range() {
      return this.httpHeaders.getRange();
    }

    @Override
    public List<String> header(String headerName) {
      List<String> headerValues = this.httpHeaders.get(headerName);
      return (headerValues != null ? headerValues : Collections.emptyList());
    }

    @Override
    public HttpHeaders asHttpHeaders() {
      return this.httpHeaders;
    }

    @Override
    public String toString() {
      return this.httpHeaders.toString();
    }
  }

  private static final class ServletParametersMap extends AbstractMap<String, List<String>> {

    private final RequestContext requestContext;

    private ServletParametersMap(RequestContext requestContext) {
      this.requestContext = requestContext;
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
      return this.requestContext.getParameters().entrySet().stream()
              .map(entry -> {
                List<String> value = Arrays.asList(entry.getValue());
                return new SimpleImmutableEntry<>(entry.getKey(), value);
              })
              .collect(Collectors.toSet());
    }

    @Override
    public int size() {
      return this.requestContext.getParameters().size();
    }

    @Override
    public List<String> get(Object key) {
      String[] parameterValues = requestContext.getParameters((String) key);
      if (ObjectUtils.isEmpty(parameterValues)) {
        return Collections.emptyList();
      }
      else {
        return Arrays.asList(parameterValues);
      }
    }

    @Override
    public List<String> put(String key, List<String> value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<String> remove(Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }

  private static final class ServletAttributesMap extends AbstractMap<String, Object> {

    private final RequestContext requestContext;

    private ServletAttributesMap(RequestContext requestContext) {
      this.requestContext = requestContext;
    }

    @Override
    public boolean containsKey(Object key) {
      String name = (String) key;
      return this.requestContext.getAttribute(name) != null;
    }

    @Override
    public void clear() {
      for (String attributeName : requestContext.getAttributeNames()) {
        requestContext.removeAttribute(attributeName);
      }
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      HashSet<Entry<String, Object>> ret = new HashSet<>();
      for (String attributeName : requestContext.getAttributeNames()) {
        Object value = requestContext.getAttribute(attributeName);
        ret.add(new SimpleImmutableEntry<>(attributeName, value));
      }
      return ret;
    }

    @Override
    @Nullable
    public Object get(Object key) {
      String name = (String) key;
      return this.requestContext.getAttribute(name);
    }

    @Override
    public Object put(String key, Object value) {
      Object oldValue = requestContext.getAttribute(key);
      this.requestContext.setAttribute(key, value);
      return oldValue;
    }

    @Override
    @Nullable
    public Object remove(Object key) {
      String name = (String) key;
      return this.requestContext.removeAttribute(name);
    }
  }

  static class CheckNotModifiedResponse extends RequestContextDecorator {

    @Serial
    private static final long serialVersionUID = 1L;

    private int status = 200;

    private final HttpHeaders headers = HttpHeaders.create();

    protected CheckNotModifiedResponse(RequestContext context) {
      super(context);
    }

    @Override
    public HttpHeaders responseHeaders() {
      return headers;
    }

    @Override
    public void setStatus(int sc) {
      this.status = sc;
    }

    @Override
    public int getStatus() {
      return this.status;
    }

  }

}
