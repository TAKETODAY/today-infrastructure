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

package infra.test.web.reactive.server;

import java.util.ArrayList;
import java.util.List;

import infra.lang.Nullable;

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
