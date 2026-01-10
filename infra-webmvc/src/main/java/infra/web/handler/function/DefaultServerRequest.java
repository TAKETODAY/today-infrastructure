/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

import infra.core.ParameterizedTypeReference;
import infra.core.ResolvableType;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRange;
import infra.http.MediaType;
import infra.http.converter.GenericHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.SmartHttpMessageConverter;
import infra.http.server.RequestPath;
import infra.lang.Assert;
import infra.util.CollectionUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MimeTypeUtils;
import infra.util.MultiValueMap;
import infra.validation.BindException;
import infra.validation.BindingResult;
import infra.web.HttpMediaTypeNotSupportedException;
import infra.web.RequestContext;
import infra.web.accept.ApiVersionStrategy;
import infra.web.async.AsyncWebRequest;
import infra.web.bind.WebDataBinder;
import infra.web.multipart.MultipartRequest;
import infra.web.multipart.Part;
import infra.web.util.UriBuilder;
import infra.web.util.UriComponentsBuilder;

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

  private final MultiValueMap<String, String> params;

  private final List<HttpMessageConverter<?>> messageConverters;

  @Nullable
  private final ApiVersionStrategy versionStrategy;

  @Nullable
  private MultiValueMap<String, Part> parts;

  public DefaultServerRequest(RequestContext servletRequest, List<HttpMessageConverter<?>> messageConverters) {
    this(servletRequest, messageConverters, null);
  }

  public DefaultServerRequest(RequestContext requestContext,
          List<HttpMessageConverter<?>> messageConverters, @Nullable ApiVersionStrategy versionStrategy) {

    this.requestContext = requestContext;
    this.versionStrategy = versionStrategy;
    this.params = requestContext.getParameters();
    this.requestPath = requestContext.getRequestPath();
    this.messageConverters = List.copyOf(messageConverters);
    this.headers = new DefaultRequestHeaders(requestContext.getHeaders());
  }

  @Override
  public HttpMethod method() {
    return requestContext.getMethod();
  }

  @Override
  public String methodName() {
    return requestContext.getMethodAsString();
  }

  @Override
  public URI uri() {
    return requestContext.getURI();
  }

  @Override
  public UriBuilder uriBuilder() {
    return UriComponentsBuilder.forHttpRequest(requestContext);
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
    var result = new LinkedMultiValueMap<String, HttpCookie>(cookies.length);
    for (HttpCookie cookie : cookies) {
      result.add(cookie.getName(), cookie);
    }
    return result;
  }

  @Override
  public RequestContext exchange() {
    return requestContext;
  }

  @Override
  public InetSocketAddress remoteAddress() {
    return requestContext.remoteAddress();
  }

  @Override
  public List<HttpMessageConverter<?>> messageConverters() {
    return this.messageConverters;
  }

  @Nullable
  @Override
  public ApiVersionStrategy apiVersionStrategy() {
    return this.versionStrategy;
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
    if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> rawType) {
      return rawType;
    }
    return Object.class;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <T> T bodyInternal(Type bodyType, Class bodyClass) throws IOException {
    MediaType contentType = headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);

    ResolvableType resolvableType = null;
    for (HttpMessageConverter converter : messageConverters) {
      if (converter instanceof GenericHttpMessageConverter generic) {
        if (generic.canRead(bodyType, bodyClass, contentType)) {
          return (T) generic.read(bodyType, bodyClass, requestContext);
        }
      }

      if (converter instanceof SmartHttpMessageConverter smart) {
        if (resolvableType == null) {
          resolvableType = ResolvableType.forType(bodyType);
        }
        if (smart.canRead(resolvableType, contentType)) {
          return (T) smart.read(resolvableType, requestContext, null);
        }
      }

      if (converter.canRead(bodyClass, contentType)) {
        return (T) converter.read(bodyClass, requestContext);
      }
    }
    throw new HttpMediaTypeNotSupportedException(contentType, getSupportedMediaTypes(bodyClass), method());
  }

  private List<MediaType> getSupportedMediaTypes(Class<?> bodyClass) {
    var result = new ArrayList<MediaType>(messageConverters.size());
    for (HttpMessageConverter<?> converter : messageConverters) {
      result.addAll(converter.getSupportedMediaTypes(bodyClass));
    }
    MimeTypeUtils.sortBySpecificity(result);
    return result;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T bind(Class<T> bindType, Consumer<WebDataBinder> dataBinderCustomizer) throws BindException {
    Assert.notNull(bindType, "BindType is required");
    Assert.notNull(dataBinderCustomizer, "DataBinderCustomizer is required");

    WebDataBinder dataBinder = new WebDataBinder(null);
    dataBinder.setTargetType(ResolvableType.forClass(bindType));
    dataBinderCustomizer.accept(dataBinder);

    RequestContext context = exchange();
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
  @Nullable
  public Object attribute(String name) {
    return requestContext.getAttribute(name);
  }

  @Override
  public Map<String, Object> attributes() {
    return requestContext.getAttributes();
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
  public MultiValueMap<String, Part> multipartData() throws IOException {
    MultiValueMap<String, Part> result = this.parts;
    if (result == null) {
      result = requestContext.asMultipartRequest().getParts();
      this.parts = result;
    }
    return result;
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Map<String, String> pathVariables() {
    if (requestContext.getAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map pathVariables) {
      return pathVariables;
    }
    return Collections.emptyMap();
  }

  @Override
  public String toString() {
    return String.format("HTTP %s %s", method(), path());
  }

  static Optional<ServerResponse> checkNotModified(RequestContext context,
          @Nullable Instant lastModified, @Nullable String etag) {

    long lastModifiedTimestamp = -1;
    if (lastModified != null && lastModified.isAfter(Instant.EPOCH)) {
      lastModifiedTimestamp = lastModified.toEpochMilli();
    }
    var response = new CheckNotModifiedResponse(context);
    if (response.checkNotModified(etag, lastModifiedTimestamp)) {
      return Optional.of(
              ServerResponse.status(response.status)
                      .headers(response.headers)
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
      this.httpHeaders = httpHeaders.asReadOnly();
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

    @Nullable
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

  static class CheckNotModifiedResponse extends RequestContext {

    private int status = 200;

    private final HttpHeaders headers = HttpHeaders.forWritable();

    private final RequestContext context;

    @SuppressWarnings("NullAway")
    protected CheckNotModifiedResponse(RequestContext context) {
      super(null, null);
      this.context = context;
    }

    @Override
    public HttpHeaders responseHeaders() {
      return headers;
    }

    @Override
    public HttpMethod getMethod() {
      return context.getMethod();
    }

    @Override
    public String getMethodAsString() {
      return context.getMethodAsString();
    }

    @Nullable
    @Override
    public HttpCookie getCookie(String name) {
      return context.getCookie(name);
    }

    @Override
    protected MultiValueMap<String, String> readParameters() {
      throw new UnsupportedOperationException();
    }

    @Override
    public MultiValueMap<String, String> getParameters() {
      return context.getParameters();
    }

    @Override
    public HttpHeaders requestHeaders() {
      return context.requestHeaders();
    }

    @Override
    public <T> T nativeRequest() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setStatus(int sc) {
      this.status = sc;
    }

    @Override
    public int getStatus() {
      return this.status;
    }

    @Override
    public void sendError(int sc) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void sendError(int sc, @Nullable String msg) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    protected OutputStream createOutputStream() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getRequestTimeMillis() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getScheme() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getServerName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getServerPort() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected String readRequestURI() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getRequestURL() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected String readQueryString() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected HttpCookie[] readCookies() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected String readMethod() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteAddress() {
      throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress localAddress() {
      return context.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
      return context.remoteAddress();
    }

    @Override
    public long getContentLength() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected InputStream createInputStream() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    protected MultipartRequest createMultipartRequest() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected AsyncWebRequest createAsyncWebRequest() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getContentTypeAsString() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected HttpHeaders createRequestHeaders() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCommitted() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void sendRedirect(String location) throws IOException {

    }

  }

}
