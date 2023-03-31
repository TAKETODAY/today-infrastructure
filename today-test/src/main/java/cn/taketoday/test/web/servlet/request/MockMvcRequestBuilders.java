/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.request;

import java.net.URI;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.test.web.servlet.RequestBuilder;
import jakarta.servlet.DispatcherType;

/**
 * Static factory methods for {@link RequestBuilder RequestBuilders}.
 *
 * <h3>Integration with the Spring TestContext Framework</h3>
 * <p>Methods in this class will reuse a
 * {@link cn.taketoday.mock.web.MockServletContext MockServletContext}
 * that was created by the Spring TestContext Framework.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to
 * this setting, open the Preferences and type "favorites".
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Greg Turnquist
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Kamill Sokol
 * @since 4.0
 */
public abstract class MockMvcRequestBuilders {

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a GET request.
   *
   * @param urlTemplate a URL template; the resulting URL will be encoded
   * @param uriVariables zero or more URI variables
   */
  public static MockHttpServletRequestBuilder get(String urlTemplate, Object... uriVariables) {
    return new MockHttpServletRequestBuilder(HttpMethod.GET, urlTemplate, uriVariables);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a GET request.
   *
   * @param uri the URL
   */
  public static MockHttpServletRequestBuilder get(URI uri) {
    return new MockHttpServletRequestBuilder(HttpMethod.GET, uri);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a POST request.
   *
   * @param urlTemplate a URL template; the resulting URL will be encoded
   * @param uriVariables zero or more URI variables
   */
  public static MockHttpServletRequestBuilder post(String urlTemplate, Object... uriVariables) {
    return new MockHttpServletRequestBuilder(HttpMethod.POST, urlTemplate, uriVariables);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a POST request.
   *
   * @param uri the URL
   */
  public static MockHttpServletRequestBuilder post(URI uri) {
    return new MockHttpServletRequestBuilder(HttpMethod.POST, uri);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a PUT request.
   *
   * @param urlTemplate a URL template; the resulting URL will be encoded
   * @param uriVariables zero or more URI variables
   */
  public static MockHttpServletRequestBuilder put(String urlTemplate, Object... uriVariables) {
    return new MockHttpServletRequestBuilder(HttpMethod.PUT, urlTemplate, uriVariables);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a PUT request.
   *
   * @param uri the URL
   */
  public static MockHttpServletRequestBuilder put(URI uri) {
    return new MockHttpServletRequestBuilder(HttpMethod.PUT, uri);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a PATCH request.
   *
   * @param urlTemplate a URL template; the resulting URL will be encoded
   * @param uriVariables zero or more URI variables
   */
  public static MockHttpServletRequestBuilder patch(String urlTemplate, Object... uriVariables) {
    return new MockHttpServletRequestBuilder(HttpMethod.PATCH, urlTemplate, uriVariables);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a PATCH request.
   *
   * @param uri the URL
   */
  public static MockHttpServletRequestBuilder patch(URI uri) {
    return new MockHttpServletRequestBuilder(HttpMethod.PATCH, uri);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a DELETE request.
   *
   * @param urlTemplate a URL template; the resulting URL will be encoded
   * @param uriVariables zero or more URI variables
   */
  public static MockHttpServletRequestBuilder delete(String urlTemplate, Object... uriVariables) {
    return new MockHttpServletRequestBuilder(HttpMethod.DELETE, urlTemplate, uriVariables);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a DELETE request.
   *
   * @param uri the URL
   */
  public static MockHttpServletRequestBuilder delete(URI uri) {
    return new MockHttpServletRequestBuilder(HttpMethod.DELETE, uri);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for an OPTIONS request.
   *
   * @param urlTemplate a URL template; the resulting URL will be encoded
   * @param uriVariables zero or more URI variables
   */
  public static MockHttpServletRequestBuilder options(String urlTemplate, Object... uriVariables) {
    return new MockHttpServletRequestBuilder(HttpMethod.OPTIONS, urlTemplate, uriVariables);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for an OPTIONS request.
   *
   * @param uri the URL
   */
  public static MockHttpServletRequestBuilder options(URI uri) {
    return new MockHttpServletRequestBuilder(HttpMethod.OPTIONS, uri);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a HEAD request.
   *
   * @param urlTemplate a URL template; the resulting URL will be encoded
   * @param uriVariables zero or more URI variables
   */
  public static MockHttpServletRequestBuilder head(String urlTemplate, Object... uriVariables) {
    return new MockHttpServletRequestBuilder(HttpMethod.HEAD, urlTemplate, uriVariables);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a HEAD request.
   *
   * @param uri the URL
   */
  public static MockHttpServletRequestBuilder head(URI uri) {
    return new MockHttpServletRequestBuilder(HttpMethod.HEAD, uri);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a request with the given HTTP method.
   *
   * @param method the HTTP method (GET, POST, etc)
   * @param urlTemplate a URL template; the resulting URL will be encoded
   * @param uriVariables zero or more URI variables
   */
  public static MockHttpServletRequestBuilder request(HttpMethod method, String urlTemplate, Object... uriVariables) {
    return new MockHttpServletRequestBuilder(method, urlTemplate, uriVariables);
  }

  /**
   * Create a {@link MockHttpServletRequestBuilder} for a request with the given HTTP method.
   *
   * @param httpMethod the HTTP method (GET, POST, etc)
   * @param uri the URL
   */
  public static MockHttpServletRequestBuilder request(HttpMethod httpMethod, URI uri) {
    return new MockHttpServletRequestBuilder(httpMethod, uri);
  }

  /**
   * Alternative factory method that allows for custom HTTP verbs (e.g. WebDAV).
   *
   * @param httpMethod the HTTP method
   * @param uri the URL
   */
  public static MockHttpServletRequestBuilder request(String httpMethod, URI uri) {
    return new MockHttpServletRequestBuilder(httpMethod, uri);
  }

  /**
   * Create a {@link MockMultipartHttpServletRequestBuilder} for a multipart request,
   * using POST as the HTTP method.
   *
   * @param urlTemplate a URL template; the resulting URL will be encoded
   * @param uriVariables zero or more URI variables
   */
  public static MockMultipartHttpServletRequestBuilder multipart(String urlTemplate, Object... uriVariables) {
    return new MockMultipartHttpServletRequestBuilder(urlTemplate, uriVariables);
  }

  /**
   * Variant of {@link #multipart(String, Object...)} that also accepts an
   * {@link HttpMethod}.
   *
   * @param httpMethod the HTTP method to use
   * @param urlTemplate a URL template; the resulting URL will be encoded
   * @param uriVariables zero or more URI variables
   */
  public static MockMultipartHttpServletRequestBuilder multipart(HttpMethod httpMethod, String urlTemplate, Object... uriVariables) {
    return new MockMultipartHttpServletRequestBuilder(httpMethod, urlTemplate, uriVariables);
  }

  /**
   * Variant of {@link #multipart(String, Object...)} with a {@link URI}.
   *
   * @param uri the URL
   */
  public static MockMultipartHttpServletRequestBuilder multipart(URI uri) {
    return new MockMultipartHttpServletRequestBuilder(uri);
  }

  /**
   * Variant of {@link #multipart(String, Object...)} with a {@link URI} and
   * an {@link HttpMethod}.
   *
   * @param httpMethod the HTTP method to use
   * @param uri the URL
   */
  public static MockMultipartHttpServletRequestBuilder multipart(HttpMethod httpMethod, URI uri) {
    return new MockMultipartHttpServletRequestBuilder(httpMethod, uri);
  }

  /**
   * Create a {@link RequestBuilder} for an async dispatch from the
   * {@link MvcResult} of the request that started async processing.
   * <p>Usage involves performing a request that starts async processing first:
   * <pre class="code">
   * MvcResult mvcResult = this.mockMvc.perform(get("/1"))
   * 	.andExpect(request().asyncStarted())
   * 	.andReturn();
   *  </pre>
   * <p>And then performing the async dispatch re-using the {@code MvcResult}:
   * <pre class="code">
   * this.mockMvc.perform(asyncDispatch(mvcResult))
   * 	.andExpect(status().isOk())
   * 	.andExpect(content().contentType(MediaType.APPLICATION_JSON))
   * 	.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
   * </pre>
   *
   * @param mvcResult the result from the request that started async processing
   */
  public static RequestBuilder asyncDispatch(MvcResult mvcResult) {

    // There must be an async result before dispatching
    mvcResult.getAsyncResult();

    return servletContext -> {
      MockHttpServletRequest request = mvcResult.getRequest();
      request.setDispatcherType(DispatcherType.ASYNC);
      request.setAsyncStarted(false);
      return request;
    };
  }

}
