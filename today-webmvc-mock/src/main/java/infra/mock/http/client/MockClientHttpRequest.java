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

package infra.mock.http.client;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executor;

import infra.http.HttpMethod;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpResponse;
import infra.lang.Assert;
import infra.mock.http.MockHttpOutputMessage;
import infra.util.concurrent.Future;

/**
 * Mock implementation of {@link ClientHttpRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class MockClientHttpRequest extends MockHttpOutputMessage implements ClientHttpRequest {

  private HttpMethod httpMethod;

  private URI uri;

  @Nullable
  private ClientHttpResponse clientHttpResponse;

  private boolean executed = false;

  /**
   * Default constructor.
   */
  public MockClientHttpRequest() {
    this.httpMethod = HttpMethod.GET;
    try {
      this.uri = new URI("/");
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Create an instance with the given HttpMethod and URI.
   */
  public MockClientHttpRequest(HttpMethod httpMethod, URI uri) {
    this.httpMethod = httpMethod;
    this.uri = uri;
  }

  public void setMethod(HttpMethod httpMethod) {
    this.httpMethod = httpMethod;
  }

  @Override
  public HttpMethod getMethod() {
    return this.httpMethod;
  }

  @Override
  @Deprecated
  public String getMethodAsString() {
    return this.httpMethod.name();
  }

  public void setURI(URI uri) {
    this.uri = uri;
  }

  @Override
  public URI getURI() {
    return this.uri;
  }

  public void setResponse(ClientHttpResponse clientHttpResponse) {
    this.clientHttpResponse = clientHttpResponse;
  }

  public boolean isExecuted() {
    return this.executed;
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

  @Override
  public Future<ClientHttpResponse> async(@Nullable Executor executor) {
    return Future.run(this::execute, executor);
  }

  /**
   * The default implementation returns the configured
   * {@link #setResponse(ClientHttpResponse) response}.
   * <p>Override this method to execute the request and provide a response,
   * potentially different than the configured response.
   */
  protected ClientHttpResponse executeInternal() throws IOException {
    Assert.state(this.clientHttpResponse != null, "No ClientHttpResponse");
    return this.clientHttpResponse;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.httpMethod);
    sb.append(' ').append(this.uri);
    if (!getHeaders().isEmpty()) {
      sb.append(", headers: ").append(getHeaders());
    }
    if (sb.isEmpty()) {
      sb.append("Not yet initialized");
    }
    return sb.toString();
  }

}
