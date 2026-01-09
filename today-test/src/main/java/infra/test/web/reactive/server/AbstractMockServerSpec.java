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

package infra.test.web.reactive.server;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for implementations of {@link WebTestClient.MockServerSpec}.
 *
 * @param <B> a self reference to the builder type
 * @author Rossen Stoyanchev
 * @since 4.0
 */
abstract class AbstractMockServerSpec<B extends WebTestClient.MockServerSpec<B>>
        implements WebTestClient.MockServerSpec<B> {

  @Nullable
  private List<MockServerConfigurer> configurers;

  AbstractMockServerSpec() {
  }

  @Override
  public <T extends B> T apply(MockServerConfigurer configurer) {
    configurer.afterConfigureAdded(this);
    this.configurers = (this.configurers != null ? this.configurers : new ArrayList<>(4));
    this.configurers.add(configurer);
    return self();
  }

  @SuppressWarnings("unchecked")
  private <T extends B> T self() {
    return (T) this;
  }

  @Override
  public WebTestClient.Builder configureClient() {
    return new DefaultWebTestClientBuilder();
  }

  @Override
  public WebTestClient build() {
    return configureClient().build();
  }

}
