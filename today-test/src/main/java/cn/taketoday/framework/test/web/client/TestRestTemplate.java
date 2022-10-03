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

package cn.taketoday.framework.test.web.client;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.TypeReference;
import cn.taketoday.framework.test.context.ApplicationTest;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.RequestEntity.UriTemplateRequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.client.DefaultResponseErrorHandler;
import cn.taketoday.web.client.RequestCallback;
import cn.taketoday.web.client.ResponseExtractor;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.client.config.RestTemplateBuilder;
import cn.taketoday.web.client.config.RootUriTemplateHandler;
import cn.taketoday.web.util.DefaultUriBuilderFactory;
import cn.taketoday.web.util.UriTemplateHandler;

/**
 * Convenient alternative of {@link RestTemplate} that is suitable for integration tests.
 * {@code TestRestTemplate} is fault tolerant. This means that 4xx and 5xx do not result
 * in an exception being thrown and can instead be detected via the {@link ResponseEntity
 * response entity} and its {@link ResponseEntity#getStatusCode() status code}.
 * <p>
 * A {@code TestRestTemplate} can optionally carry Basic authentication headers. If Apache
 * Http Client 4.3.2 or better is available (recommended) it will be used as the client,
 * and by default configured to ignore cookies and redirects.
 * <p>
 * Note: To prevent injection problems this class intentionally does not extend
 * {@link RestTemplate}. If you need access to the underlying {@link RestTemplate} use
 * {@link #getRestTemplate()}.
 * <p>
 * If you are using the
 * {@link ApplicationTest @ApplicationTest} annotation
 * with an embedded server, a {@link TestRestTemplate} is automatically available and can
 * be {@code @Autowired} into your test. If you need customizations (for example to adding
 * additional message converters) use a {@link RestTemplateBuilder} {@code @Bean}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kristine Jetzke
 * @author Dmytro Nosan
 * @since 4.0
 */
public class TestRestTemplate {

  private final RestTemplateBuilder builder;

  private final HttpClientOption[] httpClientOptions;

  private final RestTemplate restTemplate;

  /**
   * Create a new {@link TestRestTemplate} instance.
   *
   * @param restTemplateBuilder builder used to configure underlying
   * {@link RestTemplate}
   */
  public TestRestTemplate(RestTemplateBuilder restTemplateBuilder) {
    this(restTemplateBuilder, null, null);
  }

  /**
   * Create a new {@link TestRestTemplate} instance.
   *
   * @param httpClientOptions client options to use if the Apache HTTP Client is used
   */
  public TestRestTemplate(HttpClientOption... httpClientOptions) {
    this(null, null, httpClientOptions);
  }

  /**
   * Create a new {@link TestRestTemplate} instance with the specified credentials.
   *
   * @param username the username to use (or {@code null})
   * @param password the password (or {@code null})
   * @param httpClientOptions client options to use if the Apache HTTP Client is used
   */
  public TestRestTemplate(String username, String password, HttpClientOption... httpClientOptions) {
    this(new RestTemplateBuilder(), username, password, httpClientOptions);
  }

  /**
   * Create a new {@link TestRestTemplate} instance with the specified credentials.
   *
   * @param builder builder used to configure underlying {@link RestTemplate}
   * @param username the username to use (or {@code null})
   * @param password the password (or {@code null})
   * @param httpClientOptions client options to use if the Apache HTTP Client is used
   */
  public TestRestTemplate(RestTemplateBuilder builder, String username, String password,
          HttpClientOption... httpClientOptions) {
    Assert.notNull(builder, "Builder must not be null");
    this.builder = builder;
    this.httpClientOptions = httpClientOptions;
    if (httpClientOptions != null) {
      ClientHttpRequestFactory requestFactory = builder.buildRequestFactory();
      if (requestFactory instanceof HttpComponentsClientHttpRequestFactory) {
        builder = builder
                .requestFactory(() -> new CustomHttpComponentsClientHttpRequestFactory(httpClientOptions));
      }
    }
    if (username != null || password != null) {
      builder = builder.basicAuthentication(username, password);
    }
    this.restTemplate = builder.build();
    this.restTemplate.setErrorHandler(new NoOpResponseErrorHandler());
  }

  /**
   * Configure the {@link UriTemplateHandler} to use to expand URI templates. By default
   * the {@link DefaultUriBuilderFactory} is used which relies on Spring's URI template
   * support and exposes several useful properties that customize its behavior for
   * encoding and for prepending a common base URL. An alternative implementation may be
   * used to plug an external URI template library.
   *
   * @param handler the URI template handler to use
   */
  public void setUriTemplateHandler(UriTemplateHandler handler) {
    this.restTemplate.setUriTemplateHandler(handler);
  }

  /**
   * Returns the root URI applied by a {@link RootUriTemplateHandler} or {@code ""} if
   * the root URI is not available.
   *
   * @return the root URI
   */
  public String getRootUri() {
    UriTemplateHandler uriTemplateHandler = this.restTemplate.getUriTemplateHandler();
    if (uriTemplateHandler instanceof RootUriTemplateHandler) {
      return ((RootUriTemplateHandler) uriTemplateHandler).getRootUri();
    }
    return "";
  }

  /**
   * Retrieve a representation by doing a GET on the specified URL. The response (if
   * any) is converted and returned.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @param urlVariables the variables to expand the template
   * @param <T> the type of the return value
   * @return the converted object
   * @see RestTemplate#getForObject(String, Class, Object...)
   */
  public <T> T getForObject(String url, Class<T> responseType, Object... urlVariables) {
    return this.restTemplate.getForObject(url, responseType, urlVariables);
  }

  /**
   * Retrieve a representation by doing a GET on the URI template. The response (if any)
   * is converted and returned.
   * <p>
   * URI Template variables are expanded using the given map.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @param urlVariables the map containing variables for the URI template
   * @param <T> the type of the return value
   * @return the converted object
   * @see RestTemplate#getForObject(String, Class, Object...)
   */
  public <T> T getForObject(String url, Class<T> responseType, Map<String, ?> urlVariables) {
    return this.restTemplate.getForObject(url, responseType, urlVariables);
  }

  /**
   * Retrieve a representation by doing a GET on the URL . The response (if any) is
   * converted and returned.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @param <T> the type of the return value
   * @return the converted object
   * @see RestTemplate#getForObject(URI, Class)
   */
  public <T> T getForObject(URI url, Class<T> responseType) {
    return this.restTemplate.getForObject(applyRootUriIfNecessary(url), responseType);
  }

  /**
   * Retrieve an entity by doing a GET on the specified URL. The response is converted
   * and stored in an {@link ResponseEntity}.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @param urlVariables the variables to expand the template
   * @param <T> the type of the return value
   * @return the entity
   * @see RestTemplate#getForEntity(String, Class,
   * Object[])
   */
  public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... urlVariables) {
    return this.restTemplate.getForEntity(url, responseType, urlVariables);
  }

  /**
   * Retrieve a representation by doing a GET on the URI template. The response is
   * converted and stored in an {@link ResponseEntity}.
   * <p>
   * URI Template variables are expanded using the given map.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @param urlVariables the map containing variables for the URI template
   * @param <T> the type of the return value
   * @return the converted object
   * @see RestTemplate#getForEntity(String, Class, Map)
   */
  public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> urlVariables) {
    return this.restTemplate.getForEntity(url, responseType, urlVariables);
  }

  /**
   * Retrieve a representation by doing a GET on the URL . The response is converted and
   * stored in an {@link ResponseEntity}.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @param <T> the type of the return value
   * @return the converted object
   * @see RestTemplate#getForEntity(URI, Class)
   */
  public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) {
    return this.restTemplate.getForEntity(applyRootUriIfNecessary(url), responseType);
  }

  /**
   * Retrieve all headers of the resource specified by the URI template.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param urlVariables the variables to expand the template
   * @return all HTTP headers of that resource
   * @see RestTemplate#headForHeaders(String, Object[])
   */
  public HttpHeaders headForHeaders(String url, Object... urlVariables) {
    return this.restTemplate.headForHeaders(url, urlVariables);
  }

  /**
   * Retrieve all headers of the resource specified by the URI template.
   * <p>
   * URI Template variables are expanded using the given map.
   *
   * @param url the URL
   * @param urlVariables the map containing variables for the URI template
   * @return all HTTP headers of that resource
   * @see RestTemplate#headForHeaders(String, Map)
   */
  public HttpHeaders headForHeaders(String url, Map<String, ?> urlVariables) {
    return this.restTemplate.headForHeaders(url, urlVariables);
  }

  /**
   * Retrieve all headers of the resource specified by the URL.
   *
   * @param url the URL
   * @return all HTTP headers of that resource
   * @see RestTemplate#headForHeaders(URI)
   */
  public HttpHeaders headForHeaders(URI url) {
    return this.restTemplate.headForHeaders(applyRootUriIfNecessary(url));
  }

  /**
   * Create a new resource by POSTing the given object to the URI template, and returns
   * the value of the {@code Location} header. This header typically indicates where the
   * new resource is stored.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be POSTed, may be {@code null}
   * @param urlVariables the variables to expand the template
   * @return the value for the {@code Location} header
   * @see HttpEntity
   * @see RestTemplate#postForLocation(String, Object,
   * Object[])
   */
  public URI postForLocation(String url, Object request, Object... urlVariables) {
    return this.restTemplate.postForLocation(url, request, urlVariables);
  }

  /**
   * Create a new resource by POSTing the given object to the URI template, and returns
   * the value of the {@code Location} header. This header typically indicates where the
   * new resource is stored.
   * <p>
   * URI Template variables are expanded using the given map.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be POSTed, may be {@code null}
   * @param urlVariables the variables to expand the template
   * @return the value for the {@code Location} header
   * @see HttpEntity
   * @see RestTemplate#postForLocation(String, Object,
   * Map)
   */
  public URI postForLocation(String url, Object request, Map<String, ?> urlVariables) {
    return this.restTemplate.postForLocation(url, request, urlVariables);
  }

  /**
   * Create a new resource by POSTing the given object to the URL, and returns the value
   * of the {@code Location} header. This header typically indicates where the new
   * resource is stored.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be POSTed, may be {@code null}
   * @return the value for the {@code Location} header
   * @see HttpEntity
   * @see RestTemplate#postForLocation(URI, Object)
   */
  public URI postForLocation(URI url, Object request) {
    return this.restTemplate.postForLocation(applyRootUriIfNecessary(url), request);
  }

  /**
   * Create a new resource by POSTing the given object to the URI template, and returns
   * the representation found in the response.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be POSTed, may be {@code null}
   * @param responseType the type of the return value
   * @param urlVariables the variables to expand the template
   * @param <T> the type of the return value
   * @return the converted object
   * @see HttpEntity
   * @see RestTemplate#postForObject(String, Object,
   * Class, Object[])
   */
  public <T> T postForObject(String url, Object request, Class<T> responseType, Object... urlVariables) {
    return this.restTemplate.postForObject(url, request, responseType, urlVariables);
  }

  /**
   * Create a new resource by POSTing the given object to the URI template, and returns
   * the representation found in the response.
   * <p>
   * URI Template variables are expanded using the given map.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be POSTed, may be {@code null}
   * @param responseType the type of the return value
   * @param urlVariables the variables to expand the template
   * @param <T> the type of the return value
   * @return the converted object
   * @see HttpEntity
   * @see RestTemplate#postForObject(String, Object,
   * Class, Map)
   */
  public <T> T postForObject(String url, Object request, Class<T> responseType, Map<String, ?> urlVariables) {
    return this.restTemplate.postForObject(url, request, responseType, urlVariables);
  }

  /**
   * Create a new resource by POSTing the given object to the URL, and returns the
   * representation found in the response.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be POSTed, may be {@code null}
   * @param responseType the type of the return value
   * @param <T> the type of the return value
   * @return the converted object
   * @see HttpEntity
   * @see RestTemplate#postForObject(URI, Object, Class)
   */
  public <T> T postForObject(URI url, Object request, Class<T> responseType) {
    return this.restTemplate.postForObject(applyRootUriIfNecessary(url), request, responseType);
  }

  /**
   * Create a new resource by POSTing the given object to the URI template, and returns
   * the response as {@link ResponseEntity}.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be POSTed, may be {@code null}
   * @param responseType the response type to return
   * @param urlVariables the variables to expand the template
   * @param <T> the type of the return value
   * @return the converted object
   * @see HttpEntity
   * @see RestTemplate#postForEntity(String, Object,
   * Class, Object[])
   */
  public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType,
          Object... urlVariables) {
    return this.restTemplate.postForEntity(url, request, responseType, urlVariables);
  }

  /**
   * Create a new resource by POSTing the given object to the URI template, and returns
   * the response as {@link HttpEntity}.
   * <p>
   * URI Template variables are expanded using the given map.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be POSTed, may be {@code null}
   * @param responseType the response type to return
   * @param urlVariables the variables to expand the template
   * @param <T> the type of the return value
   * @return the converted object
   * @see HttpEntity
   * @see RestTemplate#postForEntity(String, Object,
   * Class, Map)
   */
  public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType,
          Map<String, ?> urlVariables) {
    return this.restTemplate.postForEntity(url, request, responseType, urlVariables);
  }

  /**
   * Create a new resource by POSTing the given object to the URL, and returns the
   * response as {@link ResponseEntity}.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be POSTed, may be {@code null}
   * @param responseType the response type to return
   * @param <T> the type of the return value
   * @return the converted object
   * @see HttpEntity
   * @see RestTemplate#postForEntity(URI, Object, Class)
   */
  public <T> ResponseEntity<T> postForEntity(URI url, Object request, Class<T> responseType) {
    return this.restTemplate.postForEntity(applyRootUriIfNecessary(url), request, responseType);
  }

  /**
   * Create or update a resource by PUTting the given object to the URI.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   * <p>
   * If you need to assert the request result consider using the
   * {@link TestRestTemplate#exchange exchange} method.
   *
   * @param url the URL
   * @param request the Object to be PUT, may be {@code null}
   * @param urlVariables the variables to expand the template
   * @see HttpEntity
   * @see RestTemplate#put(String, Object, Object[])
   */
  public void put(String url, Object request, Object... urlVariables) {
    this.restTemplate.put(url, request, urlVariables);
  }

  /**
   * Creates a new resource by PUTting the given object to URI template.
   * <p>
   * URI Template variables are expanded using the given map.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   * <p>
   * If you need to assert the request result consider using the
   * {@link TestRestTemplate#exchange exchange} method.
   *
   * @param url the URL
   * @param request the Object to be PUT, may be {@code null}
   * @param urlVariables the variables to expand the template
   * @see HttpEntity
   * @see RestTemplate#put(String, Object, Map)
   */
  public void put(String url, Object request, Map<String, ?> urlVariables) {
    this.restTemplate.put(url, request, urlVariables);
  }

  /**
   * Creates a new resource by PUTting the given object to URL.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   * <p>
   * If you need to assert the request result consider using the
   * {@link TestRestTemplate#exchange exchange} method.
   *
   * @param url the URL
   * @param request the Object to be PUT, may be {@code null}
   * @see HttpEntity
   * @see RestTemplate#put(URI, Object)
   */
  public void put(URI url, Object request) {
    this.restTemplate.put(applyRootUriIfNecessary(url), request);
  }

  /**
   * Update a resource by PATCHing the given object to the URI template, and returns the
   * representation found in the response.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be PATCHed, may be {@code null}
   * @param responseType the type of the return value
   * @param uriVariables the variables to expand the template
   * @param <T> the type of the return value
   * @return the converted object
   * @see HttpEntity
   */
  public <T> T patchForObject(String url, Object request, Class<T> responseType, Object... uriVariables) {
    return this.restTemplate.patchForObject(url, request, responseType, uriVariables);
  }

  /**
   * Update a resource by PATCHing the given object to the URI template, and returns the
   * representation found in the response.
   * <p>
   * URI Template variables are expanded using the given map.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be PATCHed, may be {@code null}
   * @param responseType the type of the return value
   * @param uriVariables the variables to expand the template
   * @param <T> the type of the return value
   * @return the converted object
   * @see HttpEntity
   */
  public <T> T patchForObject(String url, Object request, Class<T> responseType, Map<String, ?> uriVariables) {
    return this.restTemplate.patchForObject(url, request, responseType, uriVariables);
  }

  /**
   * Update a resource by PATCHing the given object to the URL, and returns the
   * representation found in the response.
   * <p>
   * The {@code request} parameter can be a {@link HttpEntity} in order to add
   * additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be POSTed, may be {@code null}
   * @param responseType the type of the return value
   * @param <T> the type of the return value
   * @return the converted object
   * @see HttpEntity
   */
  public <T> T patchForObject(URI url, Object request, Class<T> responseType) {
    return this.restTemplate.patchForObject(applyRootUriIfNecessary(url), request, responseType);
  }

  /**
   * Delete the resources at the specified URI.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   * <p>
   * If you need to assert the request result consider using the
   * {@link TestRestTemplate#exchange exchange} method.
   *
   * @param url the URL
   * @param urlVariables the variables to expand in the template
   * @see RestTemplate#delete(String, Object[])
   */
  public void delete(String url, Object... urlVariables) {
    this.restTemplate.delete(url, urlVariables);
  }

  /**
   * Delete the resources at the specified URI.
   * <p>
   * URI Template variables are expanded using the given map.
   * <p>
   * If you need to assert the request result consider using the
   * {@link TestRestTemplate#exchange exchange} method.
   *
   * @param url the URL
   * @param urlVariables the variables to expand the template
   * @see RestTemplate#delete(String, Map)
   */
  public void delete(String url, Map<String, ?> urlVariables) {
    this.restTemplate.delete(url, urlVariables);
  }

  /**
   * Delete the resources at the specified URL.
   * <p>
   * If you need to assert the request result consider using the
   * {@link TestRestTemplate#exchange exchange} method.
   *
   * @param url the URL
   * @see RestTemplate#delete(URI)
   */
  public void delete(URI url) {
    this.restTemplate.delete(applyRootUriIfNecessary(url));
  }

  /**
   * Return the value of the Allow header for the given URI.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param urlVariables the variables to expand in the template
   * @return the value of the allow header
   * @see RestTemplate#optionsForAllow(String, Object[])
   */
  public Set<HttpMethod> optionsForAllow(String url, Object... urlVariables) {
    return this.restTemplate.optionsForAllow(url, urlVariables);
  }

  /**
   * Return the value of the Allow header for the given URI.
   * <p>
   * URI Template variables are expanded using the given map.
   *
   * @param url the URL
   * @param urlVariables the variables to expand in the template
   * @return the value of the allow header
   * @see RestTemplate#optionsForAllow(String, Map)
   */
  public Set<HttpMethod> optionsForAllow(String url, Map<String, ?> urlVariables) {
    return this.restTemplate.optionsForAllow(url, urlVariables);
  }

  /**
   * Return the value of the Allow header for the given URL.
   *
   * @param url the URL
   * @return the value of the allow header
   * @see RestTemplate#optionsForAllow(URI)
   */
  public Set<HttpMethod> optionsForAllow(URI url) {
    return this.restTemplate.optionsForAllow(applyRootUriIfNecessary(url));
  }

  /**
   * Execute the HTTP method to the given URI template, writing the given request entity
   * to the request, and returns the response as {@link ResponseEntity}.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc.)
   * @param requestEntity the entity (headers and/or body) to write to the request, may
   * be {@code null}
   * @param responseType the type of the return value
   * @param urlVariables the variables to expand in the template
   * @param <T> the type of the return value
   * @return the response as entity
   * @see RestTemplate#exchange(String, cn.taketoday.http.HttpMethod,
   * cn.taketoday.http.HttpEntity, Class, Object[])
   */
  public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
          Class<T> responseType, Object... urlVariables) {
    return this.restTemplate.exchange(url, method, requestEntity, responseType, urlVariables);
  }

  /**
   * Execute the HTTP method to the given URI template, writing the given request entity
   * to the request, and returns the response as {@link ResponseEntity}.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc.)
   * @param requestEntity the entity (headers and/or body) to write to the request, may
   * be {@code null}
   * @param responseType the type of the return value
   * @param urlVariables the variables to expand in the template
   * @param <T> the type of the return value
   * @return the response as entity
   * @see RestTemplate#exchange(String, cn.taketoday.http.HttpMethod,
   * cn.taketoday.http.HttpEntity, Class, Map)
   */
  public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
          Class<T> responseType, Map<String, ?> urlVariables) {
    return this.restTemplate.exchange(url, method, requestEntity, responseType, urlVariables);
  }

  /**
   * Execute the HTTP method to the given URI template, writing the given request entity
   * to the request, and returns the response as {@link ResponseEntity}.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc.)
   * @param requestEntity the entity (headers and/or body) to write to the request, may
   * be {@code null}
   * @param responseType the type of the return value
   * @param <T> the type of the return value
   * @return the response as entity
   * @see RestTemplate#exchange(URI, cn.taketoday.http.HttpMethod,
   * cn.taketoday.http.HttpEntity, Class)
   */
  public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity,
          Class<T> responseType) {
    return this.restTemplate.exchange(applyRootUriIfNecessary(url), method, requestEntity, responseType);
  }

  /**
   * Execute the HTTP method to the given URI template, writing the given request entity
   * to the request, and returns the response as {@link ResponseEntity}. The given
   * {@link TypeReference} is used to pass generic type information:
   * <pre class="code">
   * TypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new TypeReference&lt;List&lt;MyBean&gt;&gt;() {};
   * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
   * </pre>
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc.)
   * @param requestEntity the entity (headers and/or body) to write to the request, may
   * be {@code null}
   * @param responseType the type of the return value
   * @param urlVariables the variables to expand in the template
   * @param <T> the type of the return value
   * @return the response as entity
   * @see RestTemplate#exchange(String, cn.taketoday.http.HttpMethod,
   * cn.taketoday.http.HttpEntity,
   * cn.taketoday.core.TypeReference, Object[])
   */
  public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
          TypeReference<T> responseType, Object... urlVariables) {
    return this.restTemplate.exchange(url, method, requestEntity, responseType, urlVariables);
  }

  /**
   * Execute the HTTP method to the given URI template, writing the given request entity
   * to the request, and returns the response as {@link ResponseEntity}. The given
   * {@link TypeReference} is used to pass generic type information:
   * <pre class="code">
   * TypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new TypeReference&lt;List&lt;MyBean&gt;&gt;() {};
   * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
   * </pre>
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc.)
   * @param requestEntity the entity (headers and/or body) to write to the request, may
   * be {@code null}
   * @param responseType the type of the return value
   * @param urlVariables the variables to expand in the template
   * @param <T> the type of the return value
   * @return the response as entity
   * @see RestTemplate#exchange(String, cn.taketoday.http.HttpMethod,
   * cn.taketoday.http.HttpEntity,
   * cn.taketoday.core.TypeReference, Map)
   */
  public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
          TypeReference<T> responseType, Map<String, ?> urlVariables) {
    return this.restTemplate.exchange(url, method, requestEntity, responseType, urlVariables);
  }

  /**
   * Execute the HTTP method to the given URI template, writing the given request entity
   * to the request, and returns the response as {@link ResponseEntity}. The given
   * {@link TypeReference} is used to pass generic type information:
   * <pre class="code">
   * TypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new TypeReference&lt;List&lt;MyBean&gt;&gt;() {};
   * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
   * </pre>
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc.)
   * @param requestEntity the entity (headers and/or body) to write to the request, may
   * be {@code null}
   * @param responseType the type of the return value
   * @param <T> the type of the return value
   * @return the response as entity
   * @see RestTemplate#exchange(URI, cn.taketoday.http.HttpMethod,
   * cn.taketoday.http.HttpEntity,
   * cn.taketoday.core.TypeReference)
   */
  public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity,
          TypeReference<T> responseType) {
    return this.restTemplate.exchange(applyRootUriIfNecessary(url), method, requestEntity, responseType);
  }

  /**
   * Execute the request specified in the given {@link RequestEntity} and return the
   * response as {@link ResponseEntity}. Typically used in combination with the static
   * builder methods on {@code RequestEntity}, for instance: <pre class="code">
   * MyRequest body = ...
   * RequestEntity request = RequestEntity.post(new URI(&quot;https://example.com/foo&quot;)).accept(MediaType.APPLICATION_JSON).body(body);
   * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
   * </pre>
   *
   * @param requestEntity the entity to write to the request
   * @param responseType the type of the return value
   * @param <T> the type of the return value
   * @return the response as entity
   * @see RestTemplate#exchange(cn.taketoday.http.RequestEntity, Class)
   */
  public <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, Class<T> responseType) {
    return this.restTemplate.exchange(createRequestEntityWithRootAppliedUri(requestEntity), responseType);
  }

  /**
   * Execute the request specified in the given {@link RequestEntity} and return the
   * response as {@link ResponseEntity}. The given {@link TypeReference} is
   * used to pass generic type information: <pre class="code">
   * MyRequest body = ...
   * RequestEntity request = RequestEntity.post(new URI(&quot;https://example.com/foo&quot;)).accept(MediaType.APPLICATION_JSON).body(body);
   * TypeReference&lt;List&lt;MyResponse&gt;&gt; myBean = new TypeReference&lt;List&lt;MyResponse&gt;&gt;() {};
   * ResponseEntity&lt;List&lt;MyResponse&gt;&gt; response = template.exchange(request, myBean);
   * </pre>
   *
   * @param requestEntity the entity to write to the request
   * @param responseType the type of the return value
   * @param <T> the type of the return value
   * @return the response as entity
   * @see RestTemplate#exchange(cn.taketoday.http.RequestEntity,
   * cn.taketoday.core.TypeReference)
   */
  public <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, TypeReference<T> responseType) {
    return this.restTemplate.exchange(createRequestEntityWithRootAppliedUri(requestEntity), responseType);
  }

  /**
   * Execute the HTTP method to the given URI template, preparing the request with the
   * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
   * <p>
   * URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc.)
   * @param requestCallback object that prepares the request
   * @param responseExtractor object that extracts the return value from the response
   * @param urlVariables the variables to expand in the template
   * @param <T> the type of the return value
   * @return an arbitrary object, as returned by the {@link ResponseExtractor}
   * @see RestTemplate#execute(String, cn.taketoday.http.HttpMethod,
   * cn.taketoday.web.client.RequestCallback,
   * cn.taketoday.web.client.ResponseExtractor, Object[])
   */
  public <T> T execute(String url, HttpMethod method, RequestCallback requestCallback,
          ResponseExtractor<T> responseExtractor, Object... urlVariables) {
    return this.restTemplate.execute(url, method, requestCallback, responseExtractor, urlVariables);
  }

  /**
   * Execute the HTTP method to the given URI template, preparing the request with the
   * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
   * <p>
   * URI Template variables are expanded using the given URI variables map.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc.)
   * @param requestCallback object that prepares the request
   * @param responseExtractor object that extracts the return value from the response
   * @param urlVariables the variables to expand in the template
   * @param <T> the type of the return value
   * @return an arbitrary object, as returned by the {@link ResponseExtractor}
   * @see RestTemplate#execute(String, cn.taketoday.http.HttpMethod,
   * cn.taketoday.web.client.RequestCallback,
   * cn.taketoday.web.client.ResponseExtractor, Map)
   */
  public <T> T execute(String url, HttpMethod method, RequestCallback requestCallback,
          ResponseExtractor<T> responseExtractor, Map<String, ?> urlVariables) {
    return this.restTemplate.execute(url, method, requestCallback, responseExtractor, urlVariables);
  }

  /**
   * Execute the HTTP method to the given URL, preparing the request with the
   * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc.)
   * @param requestCallback object that prepares the request
   * @param responseExtractor object that extracts the return value from the response
   * @param <T> the type of the return value
   * @return an arbitrary object, as returned by the {@link ResponseExtractor}
   * @see RestTemplate#execute(URI, cn.taketoday.http.HttpMethod,
   * cn.taketoday.web.client.RequestCallback,
   * cn.taketoday.web.client.ResponseExtractor)
   */
  public <T> T execute(URI url, HttpMethod method, RequestCallback requestCallback,
          ResponseExtractor<T> responseExtractor) {
    return this.restTemplate.execute(applyRootUriIfNecessary(url), method, requestCallback, responseExtractor);
  }

  /**
   * Returns the underlying {@link RestTemplate} that is actually used to perform the
   * REST operations.
   *
   * @return the restTemplate
   */
  public RestTemplate getRestTemplate() {
    return this.restTemplate;
  }

  /**
   * Creates a new {@code TestRestTemplate} with the same configuration as this one,
   * except that it will send basic authorization headers using the given
   * {@code username} and {@code password}. The request factory used is a new instance
   * of the underlying {@link RestTemplate}'s request factory type (when possible).
   *
   * @param username the username
   * @param password the password
   * @return the new template
   */
  public TestRestTemplate withBasicAuth(String username, String password) {
    TestRestTemplate template = new TestRestTemplate(this.builder, username, password, this.httpClientOptions);
    template.setUriTemplateHandler(getRestTemplate().getUriTemplateHandler());
    return template;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private RequestEntity<?> createRequestEntityWithRootAppliedUri(RequestEntity<?> requestEntity) {
    return new RequestEntity(requestEntity.getBody(), requestEntity.getHeaders(), requestEntity.getMethod(),
            applyRootUriIfNecessary(resolveUri(requestEntity)), requestEntity.getType());
  }

  private URI applyRootUriIfNecessary(URI uri) {
    UriTemplateHandler uriTemplateHandler = this.restTemplate.getUriTemplateHandler();
    if ((uriTemplateHandler instanceof RootUriTemplateHandler) && uri.toString().startsWith("/")) {
      return URI.create(((RootUriTemplateHandler) uriTemplateHandler).getRootUri() + uri.toString());
    }
    return uri;
  }

  private URI resolveUri(RequestEntity<?> entity) {
    if (entity instanceof UriTemplateRequestEntity<?> templatedUriEntity) {
      if (templatedUriEntity.getVars() != null) {
        return this.restTemplate.getUriTemplateHandler().expand(templatedUriEntity.getUriTemplate(),
                templatedUriEntity.getVars());
      }
      else if (templatedUriEntity.getVarsMap() != null) {
        return this.restTemplate.getUriTemplateHandler().expand(templatedUriEntity.getUriTemplate(),
                templatedUriEntity.getVarsMap());
      }
      throw new IllegalStateException(
              "No variables specified for URI template: " + templatedUriEntity.getUriTemplate());
    }
    return entity.getUrl();
  }

  /**
   * Options used to customize the Apache HTTP Client.
   */
  public enum HttpClientOption {

    /**
     * Enable cookies.
     */
    ENABLE_COOKIES,

    /**
     * Enable redirects.
     */
    ENABLE_REDIRECTS,

    /**
     * Use a {@link SSLConnectionSocketFactory} with {@link TrustSelfSignedStrategy}.
     */
    SSL

  }

  /**
   * {@link HttpComponentsClientHttpRequestFactory} to apply customizations.
   */
  protected static class CustomHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

    private final String cookieSpec;

    private final boolean enableRedirects;

    public CustomHttpComponentsClientHttpRequestFactory(HttpClientOption[] httpClientOptions) {
      Set<HttpClientOption> options = new HashSet<>(Arrays.asList(httpClientOptions));
      this.cookieSpec = (options.contains(HttpClientOption.ENABLE_COOKIES) ? CookieSpecs.STANDARD
                                                                           : CookieSpecs.IGNORE_COOKIES);
      this.enableRedirects = options.contains(HttpClientOption.ENABLE_REDIRECTS);
      if (options.contains(HttpClientOption.SSL)) {
        setHttpClient(createSslHttpClient());
      }
    }

    private HttpClient createSslHttpClient() {
      try {
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
        return HttpClients.custom().setSSLSocketFactory(socketFactory).build();
      }
      catch (Exception ex) {
        throw new IllegalStateException("Unable to create SSL HttpClient", ex);
      }
    }

    @Override
    protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
      HttpClientContext context = HttpClientContext.create();
      context.setRequestConfig(getRequestConfig());
      return context;
    }

    protected RequestConfig getRequestConfig() {
      Builder builder = RequestConfig.custom().setCookieSpec(this.cookieSpec).setAuthenticationEnabled(false)
              .setRedirectsEnabled(this.enableRedirects);
      return builder.build();
    }

  }

  private static class NoOpResponseErrorHandler extends DefaultResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
    }

  }

}
