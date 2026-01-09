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

package infra.test.web.mock.client;

import infra.http.client.reactive.ClientHttpConnector;
import infra.mock.api.Filter;
import infra.test.web.mock.DispatcherCustomizer;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.RequestBuilder;
import infra.test.web.mock.ResultMatcher;
import infra.test.web.mock.setup.ConfigurableMockMvcBuilder;
import infra.test.web.mock.setup.MockMvcConfigurer;
import infra.test.web.reactive.server.WebTestClient;

/**
 * Base class for implementations of {@link MockMvcWebTestClient.MockMvcServerSpec}
 * that simply delegates to a {@link ConfigurableMockMvcBuilder} supplied by
 * the concrete subclasses.
 *
 * @param <B> the type of the concrete subclass spec
 * @author Rossen Stoyanchev
 * @since 4.0
 */
abstract class AbstractMockMvcServerSpec<B extends MockMvcWebTestClient.MockMvcServerSpec<B>>
        implements MockMvcWebTestClient.MockMvcServerSpec<B> {

  @Override
  public <T extends B> T filters(Filter... filters) {
    getMockMvcBuilder().addFilters(filters);
    return self();
  }

  @Override
  public final <T extends B> T filter(Filter filter, String... urlPatterns) {
    getMockMvcBuilder().addFilter(filter, urlPatterns);
    return self();
  }

  @Override
  public <T extends B> T defaultRequest(RequestBuilder requestBuilder) {
    getMockMvcBuilder().defaultRequest(requestBuilder);
    return self();
  }

  @Override
  public <T extends B> T alwaysExpect(ResultMatcher resultMatcher) {
    getMockMvcBuilder().alwaysExpect(resultMatcher);
    return self();
  }

  @Override
  public <T extends B> T dispatcherCustomizer(DispatcherCustomizer customizer) {
    getMockMvcBuilder().addDispatcherCustomizer(customizer);
    return self();
  }

  @Override
  public <T extends B> T apply(MockMvcConfigurer configurer) {
    getMockMvcBuilder().apply(configurer);
    return self();
  }

  @SuppressWarnings("unchecked")
  private <T extends B> T self() {
    return (T) this;
  }

  /**
   * Return the concrete {@link ConfigurableMockMvcBuilder} to delegate
   * configuration methods and to use to create the {@link MockMvc}.
   */
  protected abstract ConfigurableMockMvcBuilder<?> getMockMvcBuilder();

  @Override
  public WebTestClient.Builder configureClient() {
    MockMvc mockMvc = getMockMvcBuilder().build();
    ClientHttpConnector connector = new MockMvcHttpConnector(mockMvc);
    return WebTestClient.bindToServer(connector);
  }

  @Override
  public WebTestClient build() {
    return configureClient().build();
  }

}
