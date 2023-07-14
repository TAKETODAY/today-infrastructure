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

package cn.taketoday.test.web.client;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.BufferingClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.mock.http.client.MockClientHttpRequest;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.client.RestClient;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.client.support.RestGatewaySupport;

/**
 * <strong>Main entry point for client-side REST testing</strong>. Used for tests
 * that involve direct or indirect use of the {@link RestTemplate}. Provides a
 * way to set up expected requests that will be performed through the
 * {@code RestTemplate} as well as mock responses to send back thus removing the
 * need for an actual server.
 *
 * <p>Below is an example that assumes static imports from
 * {@code MockRestRequestMatchers}, {@code MockRestResponseCreators},
 * and {@code ExpectedCount}:
 *
 * <pre class="code">
 * RestTemplate restTemplate = new RestTemplate()
 * MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
 *
 * server.expect(manyTimes(), requestTo("/hotels/42")).andExpect(method(HttpMethod.GET))
 *     .andRespond(withSuccess("{ \"id\" : \"42\", \"name\" : \"Holiday Inn\"}", MediaType.APPLICATION_JSON));
 *
 * Hotel hotel = restTemplate.getForObject("/hotels/{id}", Hotel.class, 42);
 * &#47;&#47; Use the hotel instance...
 *
 * // Verify all expectations met
 * server.verify();
 * </pre>
 *
 * <p>Note that as an alternative to the above you can also set the
 * {@link MockMvcClientHttpRequestFactory} on a {@code RestTemplate} which
 * allows executing requests against an instance of
 * {@link MockMvc MockMvc}.
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class MockRestServiceServer {

  private final RequestExpectationManager expectationManager;

  /**
   * Private constructor with {@code RequestExpectationManager}.
   * See static builder methods and {@code createServer} shortcut methods.
   */
  private MockRestServiceServer(RequestExpectationManager expectationManager) {
    this.expectationManager = expectationManager;
  }

  /**
   * Set up an expectation for a single HTTP request. The returned
   * {@link ResponseActions} can be used to set up further expectations as
   * well as to define the response.
   * <p>This method may be invoked any number times before starting to make
   * request through the underlying {@code RestTemplate} in order to set up
   * all expected requests.
   *
   * @param matcher request matcher
   * @return a representation of the expectation
   */
  public ResponseActions expect(RequestMatcher matcher) {
    return expect(ExpectedCount.once(), matcher);
  }

  /**
   * An alternative to {@link #expect(RequestMatcher)} that also indicates how
   * many times the request is expected to be executed.
   * <p>When request expectations have an expected count greater than one, only
   * the first execution is expected to match the order of declaration. Subsequent
   * request executions may be inserted anywhere thereafter.
   *
   * @param count the expected count
   * @param matcher request matcher
   * @return a representation of the expectation
   */
  public ResponseActions expect(ExpectedCount count, RequestMatcher matcher) {
    return this.expectationManager.expectRequest(count, matcher);
  }

  /**
   * Verify that all expected requests set up via
   * {@link #expect(RequestMatcher)} were indeed performed.
   *
   * @throws AssertionError if not all expectations are met
   */
  public void verify() {
    this.expectationManager.verify();
  }

  /**
   * Variant of {@link #verify()} that waits for up to the specified time for
   * all expectations to be fulfilled. This can be useful for tests that
   * involve asynchronous requests.
   *
   * @param timeout how long to wait for all expectations to be met
   * @throws AssertionError if not all expectations are met by the specified
   * timeout, or if any expectation fails at any time before that.
   */
  public void verify(Duration timeout) {
    this.expectationManager.verify(timeout);
  }

  /**
   * Reset the internal state removing all expectations and recorded requests.
   */
  public void reset() {
    this.expectationManager.reset();
  }

  /**
   * Return a builder for a {@code MockRestServiceServer} that should be used
   * to reply to the given {@code RestTemplate}.
   */
  public static MockRestServiceServerBuilder bindTo(RestClient.Builder restClientBuilder) {
    return new RestClientMockRestServiceServerBuilder(restClientBuilder);
  }

  /**
   * Return a builder for a {@code MockRestServiceServer} that should be used
   * to reply to the given {@code RestTemplate}.
   */
  public static MockRestServiceServerBuilder bindTo(RestTemplate restTemplate) {
    return new RestTemplateMockRestServiceServerBuilder(restTemplate);
  }

  /**
   * Return a builder for a {@code MockRestServiceServer} that should be used
   * to reply to the given {@code RestGatewaySupport}.
   */
  public static MockRestServiceServerBuilder bindTo(RestGatewaySupport restGatewaySupport) {
    Assert.notNull(restGatewaySupport, "'restGatewaySupport' must not be null");
    return new RestTemplateMockRestServiceServerBuilder(restGatewaySupport.getRestTemplate());
  }

  /**
   * A shortcut for {@code bindTo(restTemplate).build()}.
   *
   * @param restTemplate the RestTemplate to set up for mock testing
   * @return the mock server
   */
  public static MockRestServiceServer createServer(RestTemplate restTemplate) {
    return bindTo(restTemplate).build();
  }

  /**
   * A shortcut for {@code bindTo(restGateway).build()}.
   *
   * @param restGateway the REST gateway to set up for mock testing
   * @return the created mock server
   */
  public static MockRestServiceServer createServer(RestGatewaySupport restGateway) {
    return bindTo(restGateway).build();
  }

  /**
   * Builder to create a {@code MockRestServiceServer}.
   */
  public interface MockRestServiceServerBuilder {

    /**
     * Whether to allow expected requests to be executed in any order not
     * necessarily matching the order of declaration.
     * <p>Effectively a shortcut for:<br>
     * {@code builder.build(new UnorderedRequestExpectationManager)}.
     * <p>By default this is set to {@code false}
     *
     * @param ignoreExpectOrder whether to ignore the order of expectations
     */
    MockRestServiceServerBuilder ignoreExpectOrder(boolean ignoreExpectOrder);

    /**
     * Use the {@link BufferingClientHttpRequestFactory} wrapper to buffer
     * the input and output streams, and for example, allow multiple reads
     * of the response body.
     */
    MockRestServiceServerBuilder bufferContent();

    /**
     * Build the {@code MockRestServiceServer} and set up the underlying
     * {@code RestTemplate} with a {@link ClientHttpRequestFactory} that
     * creates mock requests.
     */
    MockRestServiceServer build();

    /**
     * An overloaded build alternative that accepts a custom
     * {@link RequestExpectationManager}.
     */
    MockRestServiceServer build(RequestExpectationManager manager);
  }

  private abstract static class AbstractMockRestServiceServerBuilder implements MockRestServiceServerBuilder {

    private boolean ignoreExpectOrder;

    private boolean bufferContent;

    @Override
    public MockRestServiceServerBuilder ignoreExpectOrder(boolean ignoreExpectOrder) {
      this.ignoreExpectOrder = ignoreExpectOrder;
      return this;
    }

    @Override
    public MockRestServiceServerBuilder bufferContent() {
      this.bufferContent = true;
      return this;
    }

    @Override
    public MockRestServiceServer build() {
      if (this.ignoreExpectOrder) {
        return build(new UnorderedRequestExpectationManager());
      }
      else {
        return build(new SimpleRequestExpectationManager());
      }
    }

    @Override
    public MockRestServiceServer build(RequestExpectationManager manager) {
      MockRestServiceServer server = new MockRestServiceServer(manager);
      ClientHttpRequestFactory factory = server.new MockClientHttpRequestFactory();
      if (this.bufferContent) {
        factory = new BufferingClientHttpRequestFactory(factory);
      }
      injectRequestFactory(factory);
      return server;
    }

    protected abstract void injectRequestFactory(ClientHttpRequestFactory requestFactory);

  }

  private static class RestClientMockRestServiceServerBuilder extends AbstractMockRestServiceServerBuilder {

    private final RestClient.Builder restClientBuilder;

    RestClientMockRestServiceServerBuilder(RestClient.Builder restClientBuilder) {
      Assert.notNull(restClientBuilder, "RestClient.Builder must not be null");
      this.restClientBuilder = restClientBuilder;
    }

    @Override
    protected void injectRequestFactory(ClientHttpRequestFactory requestFactory) {
      this.restClientBuilder.requestFactory(requestFactory);
    }
  }

  private static class RestTemplateMockRestServiceServerBuilder extends AbstractMockRestServiceServerBuilder {

    private final RestTemplate restTemplate;

    RestTemplateMockRestServiceServerBuilder(RestTemplate restTemplate) {
      Assert.notNull(restTemplate, "RestTemplate must not be null");
      this.restTemplate = restTemplate;
    }

    @Override
    protected void injectRequestFactory(ClientHttpRequestFactory requestFactory) {
      this.restTemplate.setRequestFactory(requestFactory);
    }
  }

  /**
   * Mock ClientHttpRequestFactory that creates requests by iterating
   * over the list of expected {@link DefaultRequestExpectation}'s.
   */
  private class MockClientHttpRequestFactory implements ClientHttpRequestFactory {

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
      return createRequestInternal(uri, httpMethod);
    }

    private MockClientHttpRequest createRequestInternal(URI uri, HttpMethod httpMethod) {
      Assert.notNull(uri, "'uri' must not be null");
      Assert.notNull(httpMethod, "'httpMethod' must not be null");

      return new MockClientHttpRequest(httpMethod, uri) {

        @Override
        protected ClientHttpResponse executeInternal() throws IOException {
          ClientHttpResponse response = expectationManager.validateRequest(this);
          setResponse(response);
          return response;
        }
      };
    }
  }

}
