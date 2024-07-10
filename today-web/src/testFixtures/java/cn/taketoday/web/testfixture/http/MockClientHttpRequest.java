/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.testfixture.http;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MockHttpOutputMessage;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.util.UriComponentsBuilder;

/**
 * Mock implementation of {@link ClientHttpRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class MockClientHttpRequest extends MockHttpOutputMessage implements ClientHttpRequest {

  private HttpMethod httpMethod;

  private URI uri;

  @Nullable
  private ClientHttpResponse clientHttpResponse;

  private boolean executed = false;

  @Nullable
  Map<String, Object> attributes;

  /**
   * Create a {@code MockClientHttpRequest} with {@link HttpMethod#GET GET} as
   * the HTTP request method and {@code "/"} as the {@link URI}.
   */
  public MockClientHttpRequest() {
    this(HttpMethod.GET, URI.create("/"));
  }

  /**
   * Create a {@code MockClientHttpRequest} with the given {@link HttpMethod},
   * URI template, and URI template variable values.
   */
  public MockClientHttpRequest(HttpMethod httpMethod, String uriTemplate, Object... vars) {
    this(httpMethod, UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(vars).encode().toUri());
  }

  /**
   * Create a {@code MockClientHttpRequest} with the given {@link HttpMethod}
   * and {@link URI}.
   */
  public MockClientHttpRequest(HttpMethod httpMethod, URI uri) {
    this.httpMethod = httpMethod;
    this.uri = uri;
  }

  /**
   * Set the HTTP method of the request.
   */
  public void setMethod(HttpMethod httpMethod) {
    this.httpMethod = httpMethod;
  }

  @Override
  public HttpMethod getMethod() {
    return this.httpMethod;
  }

  /**
   * Set the URI of the request.
   */
  public void setURI(URI uri) {
    this.uri = uri;
  }

  @Override
  public URI getURI() {
    return this.uri;
  }

  /**
   * Set the {@link ClientHttpResponse} to be used as the result of executing
   * the this request.
   *
   * @see #execute()
   */
  public void setResponse(ClientHttpResponse clientHttpResponse) {
    this.clientHttpResponse = clientHttpResponse;
  }

  /**
   * Get the {@link #isExecuted() executed} flag.
   *
   * @see #execute()
   */
  public boolean isExecuted() {
    return this.executed;
  }

  @Override
  public Map<String, Object> getAttributes() {
    Map<String, Object> attributes = this.attributes;
    if (attributes == null) {
      attributes = new ConcurrentHashMap<>();
      this.attributes = attributes;
    }
    return attributes;
  }

  /**
   * Set the {@link #isExecuted() executed} flag to {@code true} and return the
   * configured {@link #setResponse(ClientHttpResponse) response}.
   *
   * @see #executeInternal()
   */
  @Override
  public final ClientHttpResponse execute() throws IOException {
    this.executed = true;
    return executeInternal();
  }

  /**
   * The default implementation returns the configured
   * {@link #setResponse(ClientHttpResponse) response}.
   * <p>Override this method to execute the request and provide a response,
   * potentially different from the configured response.
   */
  protected ClientHttpResponse executeInternal() throws IOException {
    Assert.state(this.clientHttpResponse != null, "No ClientHttpResponse");
    return this.clientHttpResponse;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.httpMethod).append(' ').append(this.uri);
    if (!getHeaders().isEmpty()) {
      sb.append(", headers: ").append(getHeaders());
    }
    return sb.toString();
  }

}
