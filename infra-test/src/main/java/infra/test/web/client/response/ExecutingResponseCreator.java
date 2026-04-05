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

package infra.test.web.client.response;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpResponse;
import infra.lang.Assert;
import infra.mock.http.client.MockClientHttpRequest;
import infra.test.web.client.ResponseCreator;
import infra.util.StreamUtils;

/**
 * {@code ResponseCreator} that obtains the response by executing the request
 * through a {@link ClientHttpRequestFactory}. This is useful in scenarios with
 * multiple remote services where some need to be called rather than mocked.
 * <p>The {@code ClientHttpRequestFactory} is typically used for building the
 * {@code RestClient} and is passed to {@code MockRestServiceServer},
 * in effect using the original factory rather than the test factory:
 * <pre><code>
 * RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);
 * ResponseCreator withActualResponse = new ExecutingResponseCreator(requestFactory);
 * MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
 * //...
 * server.expect(requestTo("/foo")).andRespond(withSuccess());
 * server.expect(requestTo("/bar")).andRespond(withActualResponse);
 * </code></pre>
 *
 * @author Simon Baslé
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class ExecutingResponseCreator implements ResponseCreator {

  private final ClientHttpRequestFactory requestFactory;

  /**
   * Create an instance with the given {@code ClientHttpRequestFactory}.
   *
   * @param requestFactory the request factory to delegate to
   */
  public ExecutingResponseCreator(ClientHttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public ClientHttpResponse createResponse(@Nullable ClientHttpRequest request) throws IOException {
    Assert.state(request instanceof MockClientHttpRequest, "Expected a MockClientHttpRequest");
    MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
    ClientHttpRequest newRequest = this.requestFactory.createRequest(mockRequest.getURI(), mockRequest.getMethod());
    newRequest.getHeaders().setAll(mockRequest.getHeaders());
    StreamUtils.copy(mockRequest.getBodyAsBytes(), newRequest.getBody());
    return newRequest.execute();
  }
}
