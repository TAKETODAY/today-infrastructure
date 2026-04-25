/*
 * Copyright 2012-present the original author or authors.
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

package infra.restclient.test;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import infra.beans.BeanUtils;
import infra.http.client.BufferingClientHttpRequestFactory;
import infra.lang.Assert;
import infra.test.web.client.MockRestServiceServer;
import infra.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder;
import infra.test.web.client.RequestExpectationManager;
import infra.test.web.client.SimpleRequestExpectationManager;
import infra.web.client.RestClient;
import infra.web.client.RestClientCustomizer;

/**
 * {@link RestClientCustomizer} that can be applied to {@link RestClient.Builder RestClient.Builder}
 * instances to add {@link MockRestServiceServer} support.
 * <p>
 * Typically applied to an existing builder before it is used, for example:
 * <pre class="code">
 * MockServerRestClientCustomizer customizer = new MockServerRestClientCustomizer();
 * RestClient.Builder builder = RestClient.builder();
 * customizer.customize(builder);
 * MyBean bean = new MyBean(client.build());
 * customizer.getServer().expect(requestTo("/hello")).andRespond(withSuccess());
 * bean.makeRestCall();
 * </pre>
 * <p>
 * If the customizer is only used once, the {@link #getServer()} method can be used to
 * obtain the mock server. If the customizer has been used more than once the
 * {@link #getServer(RestClient.Builder)} or {@link #getServers()} method must be used to
 * access the related server.
 * <p>
 * If a mock server is used in more than one test case in a test class, it might be
 * necessary to reset the expectations on the server between tests using
 * {@code getServer().reset()} or {@code getServer(restClientBuilder).reset()}.
 *
 * @author Scott Frederick
 * @see #getServer()
 * @see #getServer(RestClient.Builder)
 * @since 5.0
 */
public class MockServerRestClientCustomizer implements RestClientCustomizer {

  private final Map<RestClient.Builder, RequestExpectationManager> expectationManagers = new ConcurrentHashMap<>();

  private final Map<RestClient.Builder, MockRestServiceServer> servers = new ConcurrentHashMap<>();

  private final Supplier<? extends RequestExpectationManager> expectationManagerSupplier;

  private boolean bufferContent;

  public MockServerRestClientCustomizer() {
    this(SimpleRequestExpectationManager::new);
  }

  /**
   * Create a new {@link MockServerRestClientCustomizer} instance.
   *
   * @param expectationManager the expectation manager class to use
   */
  public MockServerRestClientCustomizer(Class<? extends RequestExpectationManager> expectationManager) {
    this(() -> BeanUtils.newInstance(expectationManager));
    Assert.notNull(expectationManager, "'expectationManager' is required");
  }

  /**
   * Create a new {@link MockServerRestClientCustomizer} instance.
   *
   * @param expectationManagerSupplier a supplier that provides the
   * {@link RequestExpectationManager} to use
   */
  public MockServerRestClientCustomizer(Supplier<? extends RequestExpectationManager> expectationManagerSupplier) {
    Assert.notNull(expectationManagerSupplier, "'expectationManagerSupplier' is required");
    this.expectationManagerSupplier = expectationManagerSupplier;
  }

  /**
   * Set if the {@link BufferingClientHttpRequestFactory} wrapper should be used to
   * buffer the input and output streams, and for example, allow multiple reads of the
   * response body.
   *
   * @param bufferContent if request and response content should be buffered
   */
  public void setBufferContent(boolean bufferContent) {
    this.bufferContent = bufferContent;
  }

  @Override
  public void customize(RestClient.Builder restClientBuilder) {
    RequestExpectationManager expectationManager = createExpectationManager();
    MockRestServiceServerBuilder serverBuilder = MockRestServiceServer.bindTo(restClientBuilder);
    if (this.bufferContent) {
      serverBuilder.bufferContent();
    }
    MockRestServiceServer server = serverBuilder.build(expectationManager);
    this.expectationManagers.put(restClientBuilder, expectationManager);
    this.servers.put(restClientBuilder, server);
  }

  protected RequestExpectationManager createExpectationManager() {
    return this.expectationManagerSupplier.get();
  }

  public MockRestServiceServer getServer() {
    Assert.state(!this.servers.isEmpty(), "Unable to return a single MockRestServiceServer since "
            + "MockServerRestClientCustomizer has not been bound to a RestClient");
    Assert.state(this.servers.size() == 1, "Unable to return a single MockRestServiceServer since "
            + "MockServerRestClientCustomizer has been bound to more than one RestClient");
    return this.servers.values().iterator().next();
  }

  public Map<RestClient.Builder, RequestExpectationManager> getExpectationManagers() {
    return this.expectationManagers;
  }

  public @Nullable MockRestServiceServer getServer(RestClient.Builder restClientBuilder) {
    return this.servers.get(restClientBuilder);
  }

  public Map<RestClient.Builder, MockRestServiceServer> getServers() {
    return Collections.unmodifiableMap(this.servers);
  }

}
