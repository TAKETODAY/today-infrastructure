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

package infra.app.test.web.client;

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
import infra.web.client.RestTemplate;
import infra.web.client.RestTemplateBuilder;
import infra.web.client.RestTemplateCustomizer;

/**
 * {@link RestTemplateCustomizer} that can be applied to a {@link RestTemplateBuilder}
 * instances to add {@link MockRestServiceServer} support.
 * <p>
 * Typically applied to an existing builder before it is used, for example:
 * <pre class="code">
 * MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
 * MyBean bean = new MyBean(new RestTemplateBuilder(customizer));
 * customizer.getServer().expect(requestTo("/hello")).andRespond(withSuccess());
 * bean.makeRestCall();
 * </pre>
 * <p>
 * If the customizer is only used once, the {@link #getServer()} method can be used to
 * obtain the mock server. If the customizer has been used more than once the
 * {@link #getServer(RestTemplate)} or {@link #getServers()} method must be used to access
 * the related server.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getServer()
 * @see #getServer(RestTemplate)
 * @since 4.0
 */
public class MockServerRestTemplateCustomizer implements RestTemplateCustomizer {

  private final Map<RestTemplate, RequestExpectationManager> expectationManagers = new ConcurrentHashMap<>();

  private final Map<RestTemplate, MockRestServiceServer> servers = new ConcurrentHashMap<>();

  private final Supplier<? extends RequestExpectationManager> expectationManagerSupplier;

  private boolean detectRootUri = true;

  private boolean bufferContent = false;

  public MockServerRestTemplateCustomizer() {
    this(SimpleRequestExpectationManager::new);
  }

  /**
   * Crate a new {@link MockServerRestTemplateCustomizer} instance.
   *
   * @param expectationManager the expectation manager class to use
   */
  public MockServerRestTemplateCustomizer(Class<? extends RequestExpectationManager> expectationManager) {
    this(() -> BeanUtils.newInstance(expectationManager));
    Assert.notNull(expectationManager, "ExpectationManager is required");
  }

  /**
   * Crate a new {@link MockServerRestTemplateCustomizer} instance.
   *
   * @param expectationManagerSupplier a supplier that provides the
   * {@link RequestExpectationManager} to use
   */
  public MockServerRestTemplateCustomizer(Supplier<? extends RequestExpectationManager> expectationManagerSupplier) {
    Assert.notNull(expectationManagerSupplier, "ExpectationManagerSupplier is required");
    this.expectationManagerSupplier = expectationManagerSupplier;
  }

  /**
   * Set if root URIs from {@link RootUriRequestExpectationManager} should be detected
   * and applied to the {@link MockRestServiceServer}.
   *
   * @param detectRootUri if root URIs should be detected
   */
  public void setDetectRootUri(boolean detectRootUri) {
    this.detectRootUri = detectRootUri;
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
  public void customize(RestTemplate restTemplate) {
    RequestExpectationManager expectationManager = createExpectationManager();
    if (this.detectRootUri) {
      expectationManager = RootUriRequestExpectationManager.forRestTemplate(restTemplate, expectationManager);
    }
    MockRestServiceServerBuilder serverBuilder = MockRestServiceServer.bindTo(restTemplate);
    if (this.bufferContent) {
      serverBuilder.bufferContent();
    }
    MockRestServiceServer server = serverBuilder.build(expectationManager);
    this.expectationManagers.put(restTemplate, expectationManager);
    this.servers.put(restTemplate, server);
  }

  protected RequestExpectationManager createExpectationManager() {
    return this.expectationManagerSupplier.get();
  }

  public MockRestServiceServer getServer() {
    Assert.state(!this.servers.isEmpty(), "Unable to return a single MockRestServiceServer since "
            + "MockServerRestTemplateCustomizer has not been bound to a RestTemplate");
    Assert.state(this.servers.size() == 1, "Unable to return a single MockRestServiceServer since "
            + "MockServerRestTemplateCustomizer has been bound to more than one RestTemplate");
    return this.servers.values().iterator().next();
  }

  public Map<RestTemplate, RequestExpectationManager> getExpectationManagers() {
    return this.expectationManagers;
  }

  public MockRestServiceServer getServer(RestTemplate restTemplate) {
    return this.servers.get(restTemplate);
  }

  public Map<RestTemplate, MockRestServiceServer> getServers() {
    return Collections.unmodifiableMap(this.servers);
  }

}
