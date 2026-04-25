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

package infra.app.restclient.test.config;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;

import infra.app.restclient.test.MockServerRestClientCustomizer;
import infra.app.restclient.test.MockServerRestTemplateCustomizer;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnBooleanProperty;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpResponse;
import infra.lang.Assert;
import infra.stereotype.Component;
import infra.test.web.client.ExpectedCount;
import infra.test.web.client.MockRestServiceServer;
import infra.test.web.client.RequestExpectationManager;
import infra.test.web.client.RequestMatcher;
import infra.test.web.client.ResponseActions;
import infra.web.client.RestClient;
import infra.web.client.RestTemplate;

/**
 * Auto-configuration for {@link MockRestServiceServer} support.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @see AutoConfigureMockRestServiceServer
 * @since 5.0
 */
@AutoConfiguration
@ConditionalOnClass(MockServerRestTemplateCustomizer.class)
@ConditionalOnBooleanProperty("infra.test.restclient.mockrestserviceserver.enabled")
public final class MockRestServiceServerAutoConfiguration {

  @Component
  @ConditionalOnMissingBean
  static MockServerRestTemplateCustomizer mockServerRestTemplateCustomizer() {
    return new MockServerRestTemplateCustomizer();
  }

  @Component
  @ConditionalOnMissingBean
  static MockServerRestClientCustomizer mockServerRestClientCustomizer() {
    return new MockServerRestClientCustomizer();
  }

  @Component
  @ConditionalOnMissingBean
  static MockRestServiceServer mockRestServiceServer(MockServerRestTemplateCustomizer restTemplateCustomizer,
          MockServerRestClientCustomizer restClientCustomizer) {
    try {
      return createDeferredMockRestServiceServer(restTemplateCustomizer, restClientCustomizer);
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static MockRestServiceServer createDeferredMockRestServiceServer(
          MockServerRestTemplateCustomizer restTemplateCustomizer,
          MockServerRestClientCustomizer restClientCustomizer) throws Exception {
    Constructor<MockRestServiceServer> constructor = MockRestServiceServer.class
            .getDeclaredConstructor(RequestExpectationManager.class);
    constructor.setAccessible(true);
    return constructor
            .newInstance(new DeferredRequestExpectationManager(restTemplateCustomizer, restClientCustomizer));
  }

  /**
   * {@link RequestExpectationManager} with the injected {@link MockRestServiceServer}
   * so that the bean can be created before the
   * {@link MockServerRestTemplateCustomizer#customize(RestTemplate)
   * MockServerRestTemplateCustomizer} has been called.
   */
  private static class DeferredRequestExpectationManager implements RequestExpectationManager {

    private final MockServerRestTemplateCustomizer restTemplateCustomizer;

    private final MockServerRestClientCustomizer restClientCustomizer;

    DeferredRequestExpectationManager(MockServerRestTemplateCustomizer restTemplateCustomizer,
            MockServerRestClientCustomizer restClientCustomizer) {
      this.restTemplateCustomizer = restTemplateCustomizer;
      this.restClientCustomizer = restClientCustomizer;
    }

    @Override
    public ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher) {
      return getDelegate().expectRequest(count, requestMatcher);
    }

    @Override
    public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
      return getDelegate().validateRequest(request);
    }

    @Override
    public void verify() {
      getDelegate().verify();
    }

    @Override
    public void verify(Duration timeout) {
      getDelegate().verify(timeout);
    }

    @Override
    public void reset() {
      resetExpectations(this.restTemplateCustomizer.getExpectationManagers().values());
      resetExpectations(this.restClientCustomizer.getExpectationManagers().values());
    }

    private void resetExpectations(Collection<RequestExpectationManager> expectationManagers) {
      if (expectationManagers.size() == 1) {
        expectationManagers.iterator().next().reset();
      }
    }

    private RequestExpectationManager getDelegate() {
      Map<RestTemplate, RequestExpectationManager> restTemplateExpectationManagers = this.restTemplateCustomizer
              .getExpectationManagers();
      Map<RestClient.Builder, RequestExpectationManager> restClientExpectationManagers = this.restClientCustomizer
              .getExpectationManagers();
      boolean neitherBound = restTemplateExpectationManagers.isEmpty() && restClientExpectationManagers.isEmpty();
      boolean bothBound = !restTemplateExpectationManagers.isEmpty() && !restClientExpectationManagers.isEmpty();
      Assert.state(!neitherBound, "Unable to use auto-configured MockRestServiceServer since "
              + "a mock server customizer has not been bound to a RestTemplate or RestClient");
      Assert.state(!bothBound, "Unable to use auto-configured MockRestServiceServer since "
              + "mock server customizers have been bound to both a RestTemplate and a RestClient");
      if (!restTemplateExpectationManagers.isEmpty()) {
        Assert.state(restTemplateExpectationManagers.size() == 1,
                "Unable to use auto-configured MockRestServiceServer since "
                        + "MockServerRestTemplateCustomizer has been bound to more than one RestTemplate");
        return restTemplateExpectationManagers.values().iterator().next();
      }
      Assert.state(restClientExpectationManagers.size() == 1,
              "Unable to use auto-configured MockRestServiceServer since "
                      + "MockServerRestClientCustomizer has been bound to more than one RestClient");
      return restClientExpectationManagers.values().iterator().next();
    }

  }

}
