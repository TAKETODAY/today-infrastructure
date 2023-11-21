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

package cn.taketoday.web.client;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.client.support.InterceptingHttpAccessor;
import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.GenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.ResourceHttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import cn.taketoday.http.converter.feed.AtomFeedHttpMessageConverter;
import cn.taketoday.http.converter.feed.RssChannelHttpMessageConverter;
import cn.taketoday.http.converter.json.GsonHttpMessageConverter;
import cn.taketoday.http.converter.json.JsonbHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import cn.taketoday.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.SmartList;
import cn.taketoday.web.util.AbstractUriTemplateHandler;
import cn.taketoday.web.util.DefaultUriBuilderFactory;
import cn.taketoday.web.util.DefaultUriBuilderFactory.EncodingMode;
import cn.taketoday.web.util.UriTemplateHandler;

/**
 * Synchronous client to perform HTTP requests, exposing a simple, template
 * method API over underlying HTTP client libraries such as the JDK
 * {@code HttpURLConnection}, Apache HttpComponents, and others. RestTemplate
 * offers templates for common scenarios by HTTP method, in addition to the
 * generalized {@code exchange} and {@code execute} methods that support of
 * less frequent cases.
 *
 * <p>RestTemplate is typically used as a shared component. However, its
 * configuration does not support concurrent modification, and as such its
 * configuration is typically prepared on startup. If necessary, you can create
 * multiple, differently configured RestTemplate instances on startup. Such
 * instances may use the same the underlying {@link ClientHttpRequestFactory}
 * if they need to share HTTP client resources.
 *
 * <p><strong>NOTE:</strong> {@link RestClient} offers a more modern
 * API for synchronous HTTP access. For asynchronous and streaming scenarios,
 * consider the reactive
 * {@link cn.taketoday.web.reactive.function.client.WebClient}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HttpMessageConverter
 * @see RequestCallback
 * @see ResponseExtractor
 * @see ResponseErrorHandler
 * @since 4.0
 */
public class RestTemplate extends InterceptingHttpAccessor implements RestOperations {

  private static final boolean gsonPresent = isPresent("com.google.gson.Gson");
  private static final boolean jsonbPresent = isPresent("jakarta.json.bind.Jsonb");
  private static final boolean romePresent = isPresent("com.rometools.rome.feed.WireFeed");

  private static final boolean jackson2Present = isPresent("com.fasterxml.jackson.databind.ObjectMapper")
          && isPresent("com.fasterxml.jackson.core.JsonGenerator");

  private static final boolean jackson2SmilePresent = isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory");
  private static final boolean jackson2CborPresent = isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory");
  private static final boolean jackson2XmlPresent = isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper");

  private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

  private ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();

  private UriTemplateHandler uriTemplateHandler;

  private final ResponseExtractor<HttpHeaders> headersExtractor = new HeadersExtractor();

  /**
   * Create a new instance of the {@link RestTemplate} using default settings.
   * Default {@link HttpMessageConverter HttpMessageConverters} are initialized.
   */
  public RestTemplate() {
    this.messageConverters.add(new ByteArrayHttpMessageConverter());
    this.messageConverters.add(new StringHttpMessageConverter());
    this.messageConverters.add(new ResourceHttpMessageConverter(false));

    this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());

    if (romePresent) {
      this.messageConverters.add(new AtomFeedHttpMessageConverter());
      this.messageConverters.add(new RssChannelHttpMessageConverter());
    }

    if (jackson2XmlPresent) {
      this.messageConverters.add(new MappingJackson2XmlHttpMessageConverter());
    }

    if (jackson2Present) {
      this.messageConverters.add(new MappingJackson2HttpMessageConverter());
    }
    else if (gsonPresent) {
      this.messageConverters.add(new GsonHttpMessageConverter());
    }
    else if (jsonbPresent) {
      this.messageConverters.add(new JsonbHttpMessageConverter());
    }

    if (jackson2SmilePresent) {
      this.messageConverters.add(new MappingJackson2SmileHttpMessageConverter());
    }
    if (jackson2CborPresent) {
      this.messageConverters.add(new MappingJackson2CborHttpMessageConverter());
    }

    updateErrorHandlerConverters();
    this.uriTemplateHandler = initUriTemplateHandler();
  }

  /**
   * Create a new instance of the {@link RestTemplate} based on the given {@link ClientHttpRequestFactory}.
   *
   * @param requestFactory the HTTP request factory to use
   * @see cn.taketoday.http.client.SimpleClientHttpRequestFactory
   * @see cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory
   */
  public RestTemplate(ClientHttpRequestFactory requestFactory) {
    this();
    setRequestFactory(requestFactory);
  }

  /**
   * Create a new instance of the {@link RestTemplate} using the given list of
   * {@link HttpMessageConverter} to use.
   *
   * @param messageConverters the list of {@link HttpMessageConverter} to use
   */
  public RestTemplate(List<HttpMessageConverter<?>> messageConverters) {
    validateConverters(messageConverters);
    this.messageConverters.addAll(messageConverters);
    this.uriTemplateHandler = initUriTemplateHandler();
    updateErrorHandlerConverters();
  }

  private void updateErrorHandlerConverters() {
    if (this.errorHandler instanceof DefaultResponseErrorHandler handler) {
      handler.setMessageConverters(this.messageConverters);
    }
  }

  private static DefaultUriBuilderFactory initUriTemplateHandler() {
    DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
    uriFactory.setEncodingMode(EncodingMode.URI_COMPONENT);  // for backwards compatibility..
    return uriFactory;
  }

  /**
   * Set the message body converters to use.
   * <p>These converters are used to convert from and to HTTP requests and responses.
   */
  public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    validateConverters(messageConverters);
    // Take getMessageConverters() List as-is when passed in here
    if (this.messageConverters != messageConverters) {
      this.messageConverters.clear();
      this.messageConverters.addAll(messageConverters);
    }
    updateErrorHandlerConverters();
  }

  private void validateConverters(List<HttpMessageConverter<?>> messageConverters) {
    Assert.notEmpty(messageConverters, "At least one HttpMessageConverter is required");
    Assert.noNullElements(messageConverters, "The HttpMessageConverter list must not contain null elements");
  }

  /**
   * Return the list of message body converters.
   * <p>The returned {@link List} is active and may get appended to.
   */
  public List<HttpMessageConverter<?>> getMessageConverters() {
    return this.messageConverters;
  }

  /**
   * Set the error handler.
   * <p>By default, RestTemplate uses a {@link DefaultResponseErrorHandler}.
   */
  public void setErrorHandler(ResponseErrorHandler errorHandler) {
    Assert.notNull(errorHandler, "ResponseErrorHandler is required");
    this.errorHandler = errorHandler;
    updateErrorHandlerConverters();
  }

  /**
   * Return the error handler.
   */
  public ResponseErrorHandler getErrorHandler() {
    return this.errorHandler;
  }

  /**
   * Configure default URI variable values. This is a shortcut for:
   * <pre class="code">
   * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
   * handler.setDefaultUriVariables(...);
   *
   * RestTemplate restTemplate = new RestTemplate();
   * restTemplate.setUriTemplateHandler(handler);
   * </pre>
   *
   * @param uriVars the default URI variable values
   */
  public void setDefaultUriVariables(Map<String, ?> uriVars) {
    if (this.uriTemplateHandler instanceof DefaultUriBuilderFactory) {
      ((DefaultUriBuilderFactory) this.uriTemplateHandler).setDefaultUriVariables(uriVars);
    }
    else if (this.uriTemplateHandler instanceof AbstractUriTemplateHandler handler) {
      handler.setDefaultUriVariables(uriVars);
    }
    else {
      throw new IllegalArgumentException(
              "This property is not supported with the configured UriTemplateHandler.");
    }
  }

  /**
   * Configure a strategy for expanding URI templates.
   * <p>By default, {@link DefaultUriBuilderFactory} is used. prefer
   * using {@link EncodingMode#TEMPLATE_AND_VALUES TEMPLATE_AND_VALUES}.
   *
   * @param handler the URI template handler to use
   * @see DefaultUriBuilderFactory
   * @see cn.taketoday.web.util.DefaultUriTemplateHandler
   */
  public void setUriTemplateHandler(UriTemplateHandler handler) {
    Assert.notNull(handler, "UriTemplateHandler is required");
    this.uriTemplateHandler = handler;
  }

  /**
   * Return the configured URI template handler.
   */
  public UriTemplateHandler getUriTemplateHandler() {
    return this.uriTemplateHandler;
  }

  // GET

  @Override
  @Nullable
  public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException {
    RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
    HttpMessageConverterExtractor<T> responseExtractor =
            new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
    return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
  }

  @Override
  @Nullable
  public <T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
    RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
    HttpMessageConverterExtractor<T> responseExtractor =
            new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
    return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
  }

  @Override
  @Nullable
  public <T> T getForObject(URI url, Class<T> responseType) throws RestClientException {
    RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
    HttpMessageConverterExtractor<T> responseExtractor =
            new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
    return execute(url, HttpMethod.GET, requestCallback, responseExtractor);
  }

  @Override
  public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables)
          throws RestClientException {

    RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
    return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables));
  }

  @Override
  public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables)
          throws RestClientException {

    RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
    return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables));
  }

  @Override
  public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException {
    RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
    return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor));
  }

  // HEAD

  @Override
  public HttpHeaders headForHeaders(String url, Object... uriVariables) throws RestClientException {
    return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor(), uriVariables));
  }

  @Override
  public HttpHeaders headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException {
    return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor(), uriVariables));
  }

  @Override
  public HttpHeaders headForHeaders(URI url) throws RestClientException {
    return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor()));
  }

  // POST

  @Override
  @Nullable
  public URI postForLocation(String url, @Nullable Object request, Object... uriVariables) throws RestClientException {
    RequestCallback requestCallback = httpEntityCallback(request);
    HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor(), uriVariables);
    return (headers != null ? headers.getLocation() : null);
  }

  @Override
  @Nullable
  public URI postForLocation(String url, @Nullable Object request, Map<String, ?> uriVariables) throws RestClientException {

    RequestCallback requestCallback = httpEntityCallback(request);
    HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor(), uriVariables);
    return (headers != null ? headers.getLocation() : null);
  }

  @Override
  @Nullable
  public URI postForLocation(URI url, @Nullable Object request) throws RestClientException {
    RequestCallback requestCallback = httpEntityCallback(request);
    HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor());
    return (headers != null ? headers.getLocation() : null);
  }

  @Override
  @Nullable
  public <T> T postForObject(String url, @Nullable Object request,
          Class<T> responseType, Object... uriVariables) throws RestClientException {
    RequestCallback requestCallback = httpEntityCallback(request, responseType);
    HttpMessageConverterExtractor<T> responseExtractor =
            new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
    return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
  }

  @Override
  @Nullable
  public <T> T postForObject(String url, @Nullable Object request,
          Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

    RequestCallback requestCallback = httpEntityCallback(request, responseType);
    HttpMessageConverterExtractor<T> responseExtractor =
            new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
    return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
  }

  @Override
  @Nullable
  public <T> T postForObject(URI url, @Nullable Object request, Class<T> responseType)
          throws RestClientException {
    RequestCallback requestCallback = httpEntityCallback(request, responseType);
    HttpMessageConverterExtractor<T> responseExtractor =
            new HttpMessageConverterExtractor<>(responseType, getMessageConverters());
    return execute(url, HttpMethod.POST, requestCallback, responseExtractor);
  }

  @Override
  public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request,
          Class<T> responseType, Object... uriVariables) throws RestClientException {

    RequestCallback requestCallback = httpEntityCallback(request, responseType);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
    return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables));
  }

  @Override
  public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request,
          Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

    RequestCallback requestCallback = httpEntityCallback(request, responseType);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
    return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables));
  }

  @Override
  public <T> ResponseEntity<T> postForEntity(URI url, @Nullable Object request, Class<T> responseType)
          throws RestClientException {

    RequestCallback requestCallback = httpEntityCallback(request, responseType);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
    return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor));
  }

  // PUT

  @Override
  public void put(String url, @Nullable Object request, Object... uriVariables) throws RestClientException {
    RequestCallback requestCallback = httpEntityCallback(request);
    execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
  }

  @Override
  public void put(String url, @Nullable Object request, Map<String, ?> uriVariables) throws RestClientException {
    RequestCallback requestCallback = httpEntityCallback(request);
    execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
  }

  @Override
  public void put(URI url, @Nullable Object request) throws RestClientException {
    RequestCallback requestCallback = httpEntityCallback(request);
    execute(url, HttpMethod.PUT, requestCallback, null);
  }

  // PATCH

  @Override
  @Nullable
  public <T> T patchForObject(String url, @Nullable Object request,
          Class<T> responseType, Object... uriVariables) throws RestClientException {

    RequestCallback requestCallback = httpEntityCallback(request, responseType);
    HttpMessageConverterExtractor<T> responseExtractor =
            new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
    return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor, uriVariables);
  }

  @Override
  @Nullable
  public <T> T patchForObject(String url, @Nullable Object request,
          Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
    RequestCallback requestCallback = httpEntityCallback(request, responseType);
    HttpMessageConverterExtractor<T> responseExtractor =
            new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
    return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor, uriVariables);
  }

  @Override
  @Nullable
  public <T> T patchForObject(URI url, @Nullable Object request, Class<T> responseType) throws RestClientException {
    RequestCallback requestCallback = httpEntityCallback(request, responseType);
    HttpMessageConverterExtractor<T> responseExtractor =
            new HttpMessageConverterExtractor<>(responseType, getMessageConverters());
    return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor);
  }

  // DELETE

  @Override
  public void delete(String url, Object... uriVariables) throws RestClientException {
    execute(url, HttpMethod.DELETE, null, null, uriVariables);
  }

  @Override
  public void delete(String url, Map<String, ?> uriVariables) throws RestClientException {
    execute(url, HttpMethod.DELETE, null, null, uriVariables);
  }

  @Override
  public void delete(URI url) throws RestClientException {
    execute(url, HttpMethod.DELETE, null, null);
  }

  // OPTIONS

  @Override
  public Set<HttpMethod> optionsForAllow(String url, Object... uriVariables) throws RestClientException {
    ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
    HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
    return (headers != null ? headers.getAllow() : Collections.emptySet());
  }

  @Override
  public Set<HttpMethod> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException {
    ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
    HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
    return (headers != null ? headers.getAllow() : Collections.emptySet());
  }

  @Override
  public Set<HttpMethod> optionsForAllow(URI url) throws RestClientException {
    ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
    HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor);
    return (headers != null ? headers.getAllow() : Collections.emptySet());
  }

  // exchange

  @Override
  public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
          @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables) throws RestClientException {

    RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
    return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
  }

  @Override
  public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
          @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

    RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
    return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
  }

  @Override
  public <T> ResponseEntity<T> exchange(URI url, HttpMethod method,
          @Nullable HttpEntity<?> requestEntity, Class<T> responseType) throws RestClientException {

    RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
    return nonNull(execute(url, method, requestCallback, responseExtractor));
  }

  @Override
  public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
          ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException {

    Type type = responseType.getType();
    RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
    return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
  }

  @Override
  public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
          ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

    Type type = responseType.getType();
    RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
    return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
  }

  @Override
  public <T> ResponseEntity<T> exchange(URI url, HttpMethod method,
          @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType) throws RestClientException {

    Type type = responseType.getType();
    RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
    return nonNull(execute(url, method, requestCallback, responseExtractor));
  }

  @Override
  public <T> ResponseEntity<T> exchange(RequestEntity<?> entity, Class<T> responseType) throws RestClientException {
    RequestCallback requestCallback = httpEntityCallback(entity, responseType);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
    return nonNull(doExecute(resolveUrl(entity), entity.getMethod(), requestCallback, responseExtractor));
  }

  @Override
  public <T> ResponseEntity<T> exchange(RequestEntity<?> entity, ParameterizedTypeReference<T> responseType) throws RestClientException {
    Type type = responseType.getType();
    RequestCallback requestCallback = httpEntityCallback(entity, type);
    ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
    return nonNull(doExecute(resolveUrl(entity), entity.getMethod(), requestCallback, responseExtractor));
  }

  private URI resolveUrl(RequestEntity<?> entity) {
    if (entity instanceof RequestEntity.UriTemplateRequestEntity<?> ext) {
      if (ext.getVars() != null) {
        return this.uriTemplateHandler.expand(ext.getUriTemplate(), ext.getVars());
      }
      else if (ext.getVarsMap() != null) {
        return this.uriTemplateHandler.expand(ext.getUriTemplate(), ext.getVarsMap());
      }
      else {
        throw new IllegalStateException("No variables specified for URI template: " + ext.getUriTemplate());
      }
    }
    else {
      return entity.getUrl();
    }
  }

  // General execution

  /**
   * {@inheritDoc}
   * <p>To provide a {@code RequestCallback} or {@code ResponseExtractor} only,
   * but not both, consider using:
   * <ul>
   * <li>{@link #acceptHeaderRequestCallback(Class)}
   * <li>{@link #httpEntityCallback(Object)}
   * <li>{@link #httpEntityCallback(Object, Type)}
   * <li>{@link #responseEntityExtractor(Type)}
   * </ul>
   */
  @Override
  @Nullable
  public <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
          @Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException {

    URI expanded = getUriTemplateHandler().expand(url, uriVariables);
    return doExecute(expanded, method, requestCallback, responseExtractor);
  }

  /**
   * {@inheritDoc}
   * <p>To provide a {@code RequestCallback} or {@code ResponseExtractor} only,
   * but not both, consider using:
   * <ul>
   * <li>{@link #acceptHeaderRequestCallback(Class)}
   * <li>{@link #httpEntityCallback(Object)}
   * <li>{@link #httpEntityCallback(Object, Type)}
   * <li>{@link #responseEntityExtractor(Type)}
   * </ul>
   */
  @Override
  @Nullable
  public <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
          @Nullable ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables) throws RestClientException {

    URI expanded = getUriTemplateHandler().expand(url, uriVariables);
    return doExecute(expanded, method, requestCallback, responseExtractor);
  }

  /**
   * {@inheritDoc}
   * <p>To provide a {@code RequestCallback} or {@code ResponseExtractor} only,
   * but not both, consider using:
   * <ul>
   * <li>{@link #acceptHeaderRequestCallback(Class)}
   * <li>{@link #httpEntityCallback(Object)}
   * <li>{@link #httpEntityCallback(Object, Type)}
   * <li>{@link #responseEntityExtractor(Type)}
   * </ul>
   */
  @Override
  @Nullable
  public <T> T execute(URI url, HttpMethod method, @Nullable RequestCallback requestCallback,
          @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {
    return doExecute(url, method, requestCallback, responseExtractor);
  }

  /**
   * Execute the given method on the provided URI.
   * <p>The {@link ClientHttpRequest} is processed using the {@link RequestCallback};
   * the response with the {@link ResponseExtractor}.
   *
   * @param url the fully-expanded URL to connect to
   * @param method the HTTP method to execute (GET, POST, etc.)
   * @param requestCallback object that prepares the request (can be {@code null})
   * @param responseExtractor object that extracts the return value from the response (can be {@code null})
   * @return an arbitrary object, as returned by the {@link ResponseExtractor}
   */
  @Nullable
  protected <T> T doExecute(URI url, @Nullable HttpMethod method,
          @Nullable RequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

    Assert.notNull(url, "URI is required");
    Assert.notNull(method, "HttpMethod is required");
    ClientHttpResponse response = null;
    try {
      ClientHttpRequest request = createRequest(url, method);
      if (requestCallback != null) {
        requestCallback.doWithRequest(request);
      }
      response = request.execute();
      handleResponse(url, method, response);
      return responseExtractor != null ? responseExtractor.extractData(response) : null;
    }
    catch (IOException ex) {
      String resource = url.toString();
      String query = url.getRawQuery();
      resource = query != null ? resource.substring(0, resource.indexOf('?')) : resource;
      throw new ResourceAccessException(
              "I/O error on " + method.name() +
                      " request for \"" + resource + "\": " + ex.getMessage(), ex);
    }
    finally {
      if (response != null) {
        response.close();
      }
    }
  }

  /**
   * Handle the given response, performing appropriate logging and
   * invoking the {@link ResponseErrorHandler} if necessary.
   * <p>Can be overridden in subclasses.
   *
   * @param url the fully-expanded URL to connect to
   * @param method the HTTP method to execute (GET, POST, etc.)
   * @param response the resulting {@link ClientHttpResponse}
   * @throws IOException if propagated from {@link ResponseErrorHandler}
   * @see #setErrorHandler
   */
  protected void handleResponse(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
    ResponseErrorHandler errorHandler = getErrorHandler();
    boolean hasError = errorHandler.hasError(response);
    if (logger.isDebugEnabled()) {
      try {
        HttpStatusCode status = response.getStatusCode();
        logger.debug("{} Response {}", url, status);
      }
      catch (IOException ex) {
        logger.debug("Failed to get response status code", ex);
      }
    }
    if (hasError) {
      errorHandler.handleError(url, method, response);
    }
  }

  /**
   * Return a {@code RequestCallback} that sets the request {@code Accept}
   * header based on the given response type, cross-checked against the
   * configured message converters.
   */
  public <T> RequestCallback acceptHeaderRequestCallback(Class<T> responseType) {
    return new AcceptHeaderRequestCallback(responseType);
  }

  /**
   * Return a {@code RequestCallback} implementation that writes the given
   * object to the request stream.
   */
  public RequestCallback httpEntityCallback(@Nullable Object requestBody) {
    return new HttpEntityRequestCallback(requestBody);
  }

  /**
   * Return a {@code RequestCallback} implementation that:
   * <ol>
   * <li>Sets the request {@code Accept} header based on the given response
   * type, cross-checked against the configured message converters.
   * <li>Writes the given object to the request stream.
   * </ol>
   */
  public RequestCallback httpEntityCallback(@Nullable Object requestBody, Type responseType) {
    return new HttpEntityRequestCallback(requestBody, responseType);
  }

  /**
   * Return a {@code ResponseExtractor} that prepares a {@link ResponseEntity}.
   */
  public <T> ResponseExtractor<ResponseEntity<T>> responseEntityExtractor(Type responseType) {
    return new ResponseEntityResponseExtractor<>(responseType);
  }

  /**
   * Return a response extractor for {@link HttpHeaders}.
   */
  protected ResponseExtractor<HttpHeaders> headersExtractor() {
    return this.headersExtractor;
  }

  private static <T> T nonNull(@Nullable T result) {
    Assert.state(result != null, "No result");
    return result;
  }

  static boolean isPresent(String name) {
    ClassLoader classLoader = RestTemplate.class.getClassLoader();
    return ClassUtils.isPresent(name, classLoader);
  }

  /**
   * Request callback implementation that prepares the request's accept headers.
   */
  private class AcceptHeaderRequestCallback implements RequestCallback {

    @Nullable
    private final Type responseType;

    public AcceptHeaderRequestCallback(@Nullable Type responseType) {
      this.responseType = responseType;
    }

    @Override
    public void doWithRequest(ClientHttpRequest request) throws IOException {
      if (this.responseType != null) {
        HashSet<MediaType> allSupportedMediaTypes = new HashSet<>();
        for (HttpMessageConverter<?> converter : getMessageConverters()) {
          if (canReadResponse(this.responseType, converter)) {
            putSupportedMediaTypes(this.responseType, converter, allSupportedMediaTypes);
          }
        }

        List<MediaType> supportedMediaTypes = new ArrayList<>(allSupportedMediaTypes);
        MimeTypeUtils.sortBySpecificity(supportedMediaTypes);
        if (logger.isDebugEnabled()) {
          logger.debug("Accept={}", allSupportedMediaTypes);
        }
        request.getHeaders().setAccept(supportedMediaTypes);
      }
    }

    private boolean canReadResponse(Type responseType, HttpMessageConverter<?> converter) {
      Class<?> responseClass = (responseType instanceof Class ? (Class<?>) responseType : null);
      if (responseClass != null) {
        return converter.canRead(responseClass, null);
      }
      else if (converter instanceof GenericHttpMessageConverter<?> genericConverter) {
        return genericConverter.canRead(responseType, null, null);
      }
      return false;
    }

    private void putSupportedMediaTypes(Type type, HttpMessageConverter<?> converter, HashSet<MediaType> allSupportedMediaTypes) {
      Type rawType = type instanceof ParameterizedType parameterized ? parameterized.getRawType() : type;
      Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;
      List<MediaType> mediaTypes = clazz != null ? converter.getSupportedMediaTypes(clazz) : converter.getSupportedMediaTypes();
      if (!mediaTypes.isEmpty()) {
        for (MediaType mediaType : mediaTypes) {
          if (mediaType.getCharset() != null) {
            mediaType = new MediaType(mediaType.getType(), mediaType.getSubtype());
          }
          allSupportedMediaTypes.add(mediaType);
        }
      }
    }
  }

  /**
   * Request callback implementation that writes the given object to the request stream.
   */
  private class HttpEntityRequestCallback extends AcceptHeaderRequestCallback {

    private final HttpEntity<?> requestEntity;

    public HttpEntityRequestCallback(@Nullable Object requestBody) {
      this(requestBody, null);
    }

    public HttpEntityRequestCallback(@Nullable Object requestBody, @Nullable Type responseType) {
      super(responseType);
      if (requestBody instanceof HttpEntity) {
        this.requestEntity = (HttpEntity<?>) requestBody;
      }
      else if (requestBody != null) {
        this.requestEntity = new HttpEntity<>(requestBody);
      }
      else {
        this.requestEntity = HttpEntity.EMPTY;
      }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void doWithRequest(ClientHttpRequest httpRequest) throws IOException {
      super.doWithRequest(httpRequest);
      Object requestBody = this.requestEntity.getBody();
      if (requestBody == null) {
        HttpHeaders httpHeaders = httpRequest.getHeaders();
        copyHttpHeaders(httpHeaders, requestEntity.getHeaders());
        if (httpHeaders.getContentLength() < 0) {
          httpHeaders.setContentLength(0L);
        }
      }
      else {
        Class<?> requestBodyClass = requestBody.getClass();
        Type requestBodyType = this.requestEntity instanceof RequestEntity entity
                               ? entity.getType() : requestBodyClass;
        HttpHeaders httpHeaders = httpRequest.getHeaders();
        HttpHeaders requestHeaders = this.requestEntity.getHeaders();
        MediaType requestContentType = requestHeaders.getContentType();

        // iterate messageConverter
        for (HttpMessageConverter messageConverter : getMessageConverters()) {
          if (messageConverter instanceof GenericHttpMessageConverter genericConverter) {
            if (genericConverter.canWrite(requestBodyType, requestBodyClass, requestContentType)) {
              copyHttpHeaders(httpHeaders, requestHeaders);
              if (logger.isDebugEnabled()) {
                logBody(requestBody, requestContentType, genericConverter);
              }
              genericConverter.write(requestBody, requestBodyType, requestContentType, httpRequest);
              return;
            }
          }
          else if (messageConverter.canWrite(requestBodyClass, requestContentType)) {
            copyHttpHeaders(httpHeaders, requestHeaders);
            if (logger.isDebugEnabled()) {
              logBody(requestBody, requestContentType, messageConverter);
            }
            messageConverter.write(requestBody, requestContentType, httpRequest);
            return;
          }
        }

        String message = "No HttpMessageConverter for " + requestBodyClass.getName();
        if (requestContentType != null) {
          message += " and content type \"" + requestContentType + "\"";
        }
        throw new RestClientException(message);
      }
    }

    private void logBody(Object body, @Nullable MediaType mediaType, HttpMessageConverter<?> converter) {
      if (mediaType != null) {
        logger.debug("Writing [{}] as \"{}\"", body, mediaType);
      }
      else {
        logger.debug("Writing [{}] with {}", body, converter.getClass().getName());
      }
    }
  }

  private static void copyHttpHeaders(HttpHeaders httpHeaders, HttpHeaders requestHeaders) {
    if (!requestHeaders.isEmpty()) {
      for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
        httpHeaders.put(entry.getKey(), new SmartList<>(entry.getValue()));
      }
    }
  }

  /**
   * Response extractor for {@link HttpEntity}.
   */
  private class ResponseEntityResponseExtractor<T> implements ResponseExtractor<ResponseEntity<T>> {
    @Nullable
    private final HttpMessageConverterExtractor<T> delegate;

    public ResponseEntityResponseExtractor(@Nullable Type responseType) {
      if (responseType != null && Void.class != responseType) {
        this.delegate = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
      }
      else {
        this.delegate = null;
      }
    }

    @Override
    public ResponseEntity<T> extractData(ClientHttpResponse response) throws IOException {
      if (this.delegate != null) {
        T body = this.delegate.extractData(response);
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(body);
      }
      else {
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .build();
      }
    }
  }

  /**
   * Response extractor that extracts the response {@link HttpHeaders}.
   */
  private static class HeadersExtractor implements ResponseExtractor<HttpHeaders> {

    @Override
    public HttpHeaders extractData(ClientHttpResponse response) {
      return response.getHeaders();
    }
  }

}
