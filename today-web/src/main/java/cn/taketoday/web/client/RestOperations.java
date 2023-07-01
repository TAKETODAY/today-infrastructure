/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.client;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;

/**
 * Interface specifying a basic set of RESTful operations.
 * Implemented by {@link RestTemplate}. Not often used directly, but a useful
 * option to enhance testability, as it can easily be mocked or stubbed.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see RestTemplate
 * @since 4.0
 */
public interface RestOperations {

  // GET

  /**
   * Retrieve a representation by doing a GET on the specified URL.
   * The response (if any) is converted and returned.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @param uriVariables the variables to expand the template
   * @return the converted object
   */
  @Nullable
  <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException;

  /**
   * Retrieve a representation by doing a GET on the URI template.
   * The response (if any) is converted and returned.
   * <p>URI Template variables are expanded using the given map.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @param uriVariables the map containing variables for the URI template
   * @return the converted object
   */
  @Nullable
  <T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

  /**
   * Retrieve a representation by doing a GET on the URL .
   * The response (if any) is converted and returned.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @return the converted object
   */
  @Nullable
  <T> T getForObject(URI url, Class<T> responseType) throws RestClientException;

  /**
   * Retrieve an entity by doing a GET on the specified URL.
   * The response is converted and stored in an {@link ResponseEntity}.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @param uriVariables the variables to expand the template
   * @return the entity
   */
  <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables)
          throws RestClientException;

  /**
   * Retrieve a representation by doing a GET on the URI template.
   * The response is converted and stored in an {@link ResponseEntity}.
   * <p>URI Template variables are expanded using the given map.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @param uriVariables the map containing variables for the URI template
   * @return the converted object
   */
  <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables)
          throws RestClientException;

  /**
   * Retrieve a representation by doing a GET on the URL .
   * The response is converted and stored in an {@link ResponseEntity}.
   *
   * @param url the URL
   * @param responseType the type of the return value
   * @return the converted object
   */
  <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException;

  // HEAD

  /**
   * Retrieve all headers of the resource specified by the URI template.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param uriVariables the variables to expand the template
   * @return all HTTP headers of that resource
   */
  HttpHeaders headForHeaders(String url, Object... uriVariables) throws RestClientException;

  /**
   * Retrieve all headers of the resource specified by the URI template.
   * <p>URI Template variables are expanded using the given map.
   *
   * @param url the URL
   * @param uriVariables the map containing variables for the URI template
   * @return all HTTP headers of that resource
   */
  HttpHeaders headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException;

  /**
   * Retrieve all headers of the resource specified by the URL.
   *
   * @param url the URL
   * @return all HTTP headers of that resource
   */
  HttpHeaders headForHeaders(URI url) throws RestClientException;

  // POST

  /**
   * Create a new resource by POSTing the given object to the URI template, and returns the value of
   * the {@code Location} header. This header typically indicates where the new resource is stored.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   * <p>The body of the entity, or {@code request} itself, can be a
   * {@link MultiValueMap MultiValueMap} to create a multipart request.
   * The values in the {@code MultiValueMap} can be any Object representing the body of the part,
   * or an {@link cn.taketoday.http.HttpEntity HttpEntity} representing a part with body
   * and headers.
   *
   * @param url the URL
   * @param request the Object to be POSTed (may be {@code null})
   * @param uriVariables the variables to expand the template
   * @return the value for the {@code Location} header
   * @see HttpEntity
   */
  @Nullable
  URI postForLocation(String url, @Nullable Object request, Object... uriVariables) throws RestClientException;

  /**
   * Create a new resource by POSTing the given object to the URI template, and returns the value of
   * the {@code Location} header. This header typically indicates where the new resource is stored.
   * <p>URI Template variables are expanded using the given map.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request
   * <p>The body of the entity, or {@code request} itself, can be a
   * {@link MultiValueMap MultiValueMap} to create a multipart request.
   * The values in the {@code MultiValueMap} can be any Object representing the body of the part,
   * or an {@link cn.taketoday.http.HttpEntity HttpEntity} representing a part with body
   * and headers.
   *
   * @param url the URL
   * @param request the Object to be POSTed (may be {@code null})
   * @param uriVariables the variables to expand the template
   * @return the value for the {@code Location} header
   * @see HttpEntity
   */
  @Nullable
  URI postForLocation(String url, @Nullable Object request, Map<String, ?> uriVariables)
          throws RestClientException;

  /**
   * Create a new resource by POSTing the given object to the URL, and returns the value of the
   * {@code Location} header. This header typically indicates where the new resource is stored.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   * <p>The body of the entity, or {@code request} itself, can be a
   * {@link MultiValueMap MultiValueMap} to create a multipart request.
   * The values in the {@code MultiValueMap} can be any Object representing the body of the part,
   * or an {@link cn.taketoday.http.HttpEntity HttpEntity} representing a part with body
   * and headers.
   *
   * @param url the URL
   * @param request the Object to be POSTed (may be {@code null})
   * @return the value for the {@code Location} header
   * @see HttpEntity
   */
  @Nullable
  URI postForLocation(URI url, @Nullable Object request) throws RestClientException;

  /**
   * Create a new resource by POSTing the given object to the URI template,
   * and returns the representation found in the response.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   * <p>The body of the entity, or {@code request} itself, can be a
   * {@link MultiValueMap MultiValueMap} to create a multipart request.
   * The values in the {@code MultiValueMap} can be any Object representing the body of the part,
   * or an {@link cn.taketoday.http.HttpEntity HttpEntity} representing a part with body
   * and headers.
   *
   * @param url the URL
   * @param request the Object to be POSTed (may be {@code null})
   * @param responseType the type of the return value
   * @param uriVariables the variables to expand the template
   * @return the converted object
   * @see HttpEntity
   */
  @Nullable
  <T> T postForObject(
          String url, @Nullable Object request, Class<T> responseType, Object... uriVariables)
          throws RestClientException;

  /**
   * Create a new resource by POSTing the given object to the URI template,
   * and returns the representation found in the response.
   * <p>URI Template variables are expanded using the given map.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   * <p>The body of the entity, or {@code request} itself, can be a
   * {@link MultiValueMap MultiValueMap} to create a multipart request.
   * The values in the {@code MultiValueMap} can be any Object representing the body of the part,
   * or an {@link cn.taketoday.http.HttpEntity HttpEntity} representing a part with body
   * and headers.
   *
   * @param url the URL
   * @param request the Object to be POSTed (may be {@code null})
   * @param responseType the type of the return value
   * @param uriVariables the variables to expand the template
   * @return the converted object
   * @see HttpEntity
   */
  @Nullable
  <T> T postForObject(String url, @Nullable Object request, Class<T> responseType,
                      Map<String, ?> uriVariables) throws RestClientException;

  /**
   * Create a new resource by POSTing the given object to the URL,
   * and returns the representation found in the response.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   * <p>The body of the entity, or {@code request} itself, can be a
   * {@link MultiValueMap MultiValueMap} to create a multipart request.
   * The values in the {@code MultiValueMap} can be any Object representing the body of the part,
   * or an {@link cn.taketoday.http.HttpEntity HttpEntity} representing a part with body
   * and headers.
   *
   * @param url the URL
   * @param request the Object to be POSTed (may be {@code null})
   * @param responseType the type of the return value
   * @return the converted object
   * @see HttpEntity
   */
  @Nullable
  <T> T postForObject(URI url, @Nullable Object request, Class<T> responseType) throws RestClientException;

  /**
   * Create a new resource by POSTing the given object to the URI template,
   * and returns the response as {@link ResponseEntity}.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   * <p>The body of the entity, or {@code request} itself, can be a
   * {@link MultiValueMap MultiValueMap} to create a multipart request.
   * The values in the {@code MultiValueMap} can be any Object representing the body of the part,
   * or an {@link cn.taketoday.http.HttpEntity HttpEntity} representing a part with body
   * and headers.
   *
   * @param url the URL
   * @param request the Object to be POSTed (may be {@code null})
   * @param uriVariables the variables to expand the template
   * @return the converted object
   * @see HttpEntity
   */
  <T> ResponseEntity<T> postForEntity(
          String url, @Nullable Object request, Class<T> responseType,
          Object... uriVariables) throws RestClientException;

  /**
   * Create a new resource by POSTing the given object to the URI template,
   * and returns the response as {@link HttpEntity}.
   * <p>URI Template variables are expanded using the given map.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   * <p>The body of the entity, or {@code request} itself, can be a
   * {@link MultiValueMap MultiValueMap} to create a multipart request.
   * The values in the {@code MultiValueMap} can be any Object representing the body of the part,
   * or an {@link cn.taketoday.http.HttpEntity HttpEntity} representing a part with body
   * and headers.
   *
   * @param url the URL
   * @param request the Object to be POSTed (may be {@code null})
   * @param uriVariables the variables to expand the template
   * @return the converted object
   * @see HttpEntity
   */
  <T> ResponseEntity<T> postForEntity(
          String url, @Nullable Object request, Class<T> responseType,
          Map<String, ?> uriVariables) throws RestClientException;

  /**
   * Create a new resource by POSTing the given object to the URL,
   * and returns the response as {@link ResponseEntity}.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   * <p>The body of the entity, or {@code request} itself, can be a
   * {@link MultiValueMap MultiValueMap} to create a multipart request.
   * The values in the {@code MultiValueMap} can be any Object representing the body of the part,
   * or an {@link cn.taketoday.http.HttpEntity HttpEntity} representing a part with body
   * and headers.
   *
   * @param url the URL
   * @param request the Object to be POSTed (may be {@code null})
   * @return the converted object
   * @see HttpEntity
   */
  <T> ResponseEntity<T> postForEntity(URI url, @Nullable Object request, Class<T> responseType)
          throws RestClientException;

  // PUT

  /**
   * Create or update a resource by PUTting the given object to the URI.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be PUT (may be {@code null})
   * @param uriVariables the variables to expand the template
   * @see HttpEntity
   */
  void put(String url, @Nullable Object request, Object... uriVariables) throws RestClientException;

  /**
   * Creates a new resource by PUTting the given object to URI template.
   * <p>URI Template variables are expanded using the given map.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be PUT (may be {@code null})
   * @param uriVariables the variables to expand the template
   * @see HttpEntity
   */
  void put(String url, @Nullable Object request, Map<String, ?> uriVariables) throws RestClientException;

  /**
   * Creates a new resource by PUTting the given object to URL.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   *
   * @param url the URL
   * @param request the Object to be PUT (may be {@code null})
   * @see HttpEntity
   */
  void put(URI url, @Nullable Object request) throws RestClientException;

  // PATCH

  /**
   * Update a resource by PATCHing the given object to the URI template,
   * and return the representation found in the response.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   * <p><b>NOTE: The standard JDK HTTP library does not support HTTP PATCH.
   * You need to use the Apache HttpComponents or OkHttp request factory.</b>
   *
   * @param url the URL
   * @param request the object to be PATCHed (may be {@code null})
   * @param responseType the type of the return value
   * @param uriVariables the variables to expand the template
   * @return the converted object
   * @see HttpEntity
   * @see RestTemplate#setRequestFactory
   * @see cn.taketoday.http.client.OkHttp3ClientHttpRequestFactory
   */
  @Nullable
  <T> T patchForObject(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables)
          throws RestClientException;

  /**
   * Update a resource by PATCHing the given object to the URI template,
   * and return the representation found in the response.
   * <p>URI Template variables are expanded using the given map.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   * <p><b>NOTE: The standard JDK HTTP library does not support HTTP PATCH.
   * You need to use the Apache HttpComponents or OkHttp request factory.</b>
   *
   * @param url the URL
   * @param request the object to be PATCHed (may be {@code null})
   * @param responseType the type of the return value
   * @param uriVariables the variables to expand the template
   * @return the converted object
   * @see HttpEntity
   * @see RestTemplate#setRequestFactory
   * @see cn.taketoday.http.client.OkHttp3ClientHttpRequestFactory
   */
  @Nullable
  <T> T patchForObject(
          String url, @Nullable Object request, Class<T> responseType,
          Map<String, ?> uriVariables) throws RestClientException;

  /**
   * Update a resource by PATCHing the given object to the URL,
   * and return the representation found in the response.
   * <p>The {@code request} parameter can be a {@link HttpEntity} in order to
   * add additional HTTP headers to the request.
   * <p><b>NOTE: The standard JDK HTTP library does not support HTTP PATCH.
   * You need to use the Apache HttpComponents or OkHttp request factory.</b>
   *
   * @param url the URL
   * @param request the object to be PATCHed (may be {@code null})
   * @param responseType the type of the return value
   * @return the converted object
   * @see HttpEntity
   * @see RestTemplate#setRequestFactory
   * @see cn.taketoday.http.client.OkHttp3ClientHttpRequestFactory
   */
  @Nullable
  <T> T patchForObject(URI url, @Nullable Object request, Class<T> responseType)
          throws RestClientException;

  // DELETE

  /**
   * Delete the resources at the specified URI.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param uriVariables the variables to expand in the template
   */
  void delete(String url, Object... uriVariables) throws RestClientException;

  /**
   * Delete the resources at the specified URI.
   * <p>URI Template variables are expanded using the given map.
   *
   * @param url the URL
   * @param uriVariables the variables to expand the template
   */
  void delete(String url, Map<String, ?> uriVariables) throws RestClientException;

  /**
   * Delete the resources at the specified URL.
   *
   * @param url the URL
   */
  void delete(URI url) throws RestClientException;

  // OPTIONS

  /**
   * Return the value of the Allow header for the given URI.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param uriVariables the variables to expand in the template
   * @return the value of the allow header
   */
  Set<HttpMethod> optionsForAllow(String url, Object... uriVariables) throws RestClientException;

  /**
   * Return the value of the Allow header for the given URI.
   * <p>URI Template variables are expanded using the given map.
   *
   * @param url the URL
   * @param uriVariables the variables to expand in the template
   * @return the value of the allow header
   */
  Set<HttpMethod> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException;

  /**
   * Return the value of the Allow header for the given URL.
   *
   * @param url the URL
   * @return the value of the allow header
   */
  Set<HttpMethod> optionsForAllow(URI url) throws RestClientException;

  // exchange

  /**
   * Execute the HTTP method to the given URI template, writing the given request entity to the request, and
   * returns the response as {@link ResponseEntity}.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc)
   * @param requestEntity the entity (headers and/or body) to write to the request
   * may be {@code null})
   * @param responseType the type to convert the response to, or {@code Void.class} for no body
   * @param uriVariables the variables to expand in the template
   * @return the response as entity
   */
  <T> ResponseEntity<T> exchange(
          String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
          Class<T> responseType, Object... uriVariables)
          throws RestClientException;

  /**
   * Execute the HTTP method to the given URI template, writing the given
   * request entity to the request, and returns the response as {@link ResponseEntity}.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc)
   * @param requestEntity the entity (headers and/or body) to write to the request
   * (may be {@code null})
   * @param responseType the type to convert the response to, or {@code Void.class} for no body
   * @param uriVariables the variables to expand in the template
   * @return the response as entity
   */
  <T> ResponseEntity<T> exchange(
          String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
          Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

  /**
   * Execute the HTTP method to the given URI template, writing the given request entity to the request, and
   * returns the response as {@link ResponseEntity}.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc)
   * @param requestEntity the entity (headers and/or body) to write to the request
   * (may be {@code null})
   * @param responseType the type to convert the response to, or {@code Void.class} for no body
   * @return the response as entity
   */
  <T> ResponseEntity<T> exchange(
          URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
          Class<T> responseType) throws RestClientException;

  /**
   * Execute the HTTP method to the given URI template, writing the given
   * request entity to the request, and returns the response as {@link ResponseEntity}.
   * The given {@link ParameterizedTypeReference} is used to pass generic type information:
   * <pre class="code">
   * TypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
   *     new TypeReference&lt;List&lt;MyBean&gt;&gt;() {};
   *
   * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
   *     template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
   * </pre>
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc)
   * @param requestEntity the entity (headers and/or body) to write to the
   * request (may be {@code null})
   * @param responseType the type to convert the response to, or {@code Void.class} for no body
   * @param uriVariables the variables to expand in the template
   * @return the response as entity
   */
  <T> ResponseEntity<T> exchange(
          String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
          ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException;

  /**
   * Execute the HTTP method to the given URI template, writing the given
   * request entity to the request, and returns the response as {@link ResponseEntity}.
   * The given {@link ParameterizedTypeReference} is used to pass generic type information:
   * <pre class="code">
   * TypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
   *     new TypeReference&lt;List&lt;MyBean&gt;&gt;() {};
   *
   * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
   *     template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
   * </pre>
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc)
   * @param requestEntity the entity (headers and/or body) to write to the request
   * (may be {@code null})
   * @param responseType the type to convert the response to, or {@code Void.class} for no body
   * @param uriVariables the variables to expand in the template
   * @return the response as entity
   */
  <T> ResponseEntity<T> exchange(
          String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
          ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

  /**
   * Execute the HTTP method to the given URI template, writing the given
   * request entity to the request, and returns the response as {@link ResponseEntity}.
   * The given {@link ParameterizedTypeReference} is used to pass generic type information:
   * <pre class="code">
   * TypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
   *     new TypeReference&lt;List&lt;MyBean&gt;&gt;() {};
   *
   * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
   *     template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
   * </pre>
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc)
   * @param requestEntity the entity (headers and/or body) to write to the request
   * (may be {@code null})
   * @param responseType the type to convert the response to, or {@code Void.class} for no body
   * @return the response as entity
   */
  <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
                                 ParameterizedTypeReference<T> responseType) throws RestClientException;

  /**
   * Execute the request specified in the given {@link RequestEntity} and return
   * the response as {@link ResponseEntity}. Typically used in combination
   * with the static builder methods on {@code RequestEntity}, for instance:
   * <pre class="code">
   * MyRequest body = ...
   * RequestEntity request = RequestEntity
   *     .post(new URI(&quot;https://example.com/foo&quot;))
   *     .accept(MediaType.APPLICATION_JSON)
   *     .body(body);
   * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
   * </pre>
   *
   * @param requestEntity the entity to write to the request
   * @param responseType the type to convert the response to, or {@code Void.class} for no body
   * @return the response as entity
   */
  <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, Class<T> responseType)
          throws RestClientException;

  /**
   * Execute the request specified in the given {@link RequestEntity} and return
   * the response as {@link ResponseEntity}. The given
   * {@link ParameterizedTypeReference} is used to pass generic type information:
   * <pre class="code">
   * MyRequest body = ...
   * RequestEntity request = RequestEntity
   *     .post(new URI(&quot;https://example.com/foo&quot;))
   *     .accept(MediaType.APPLICATION_JSON)
   *     .body(body);
   * TypeReference&lt;List&lt;MyResponse&gt;&gt; myBean =
   *     new TypeReference&lt;List&lt;MyResponse&gt;&gt;() {};
   * ResponseEntity&lt;List&lt;MyResponse&gt;&gt; response = template.exchange(request, myBean);
   * </pre>
   *
   * @param requestEntity the entity to write to the request
   * @param responseType the type to convert the response to, or {@code Void.class} for no body
   * @return the response as entity
   */
  <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
          throws RestClientException;

  // General execution

  /**
   * Execute the HTTP method to the given URI template, preparing the request with the
   * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
   * <p>URI Template variables are expanded using the given URI variables, if any.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc)
   * @param requestCallback object that prepares the request
   * @param responseExtractor object that extracts the return value from the response
   * @param uriVariables the variables to expand in the template
   * @return an arbitrary object, as returned by the {@link ResponseExtractor}
   */
  @Nullable
  <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
                @Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables)
          throws RestClientException;

  /**
   * Execute the HTTP method to the given URI template, preparing the request with the
   * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
   * <p>URI Template variables are expanded using the given URI variables map.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc)
   * @param requestCallback object that prepares the request
   * @param responseExtractor object that extracts the return value from the response
   * @param uriVariables the variables to expand in the template
   * @return an arbitrary object, as returned by the {@link ResponseExtractor}
   */
  @Nullable
  <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
                @Nullable ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables)
          throws RestClientException;

  /**
   * Execute the HTTP method to the given URL, preparing the request with the
   * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
   *
   * @param url the URL
   * @param method the HTTP method (GET, POST, etc)
   * @param requestCallback object that prepares the request
   * @param responseExtractor object that extracts the return value from the response
   * @return an arbitrary object, as returned by the {@link ResponseExtractor}
   */
  @Nullable
  <T> T execute(URI url, HttpMethod method, @Nullable RequestCallback requestCallback,
                @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException;

}
