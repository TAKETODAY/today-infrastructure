/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.test.web.servlet.client;

import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.DispatcherServletCustomizer;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.RequestBuilder;
import cn.taketoday.test.web.servlet.ResultMatcher;
import cn.taketoday.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import cn.taketoday.test.web.servlet.setup.MockMvcConfigurer;
import jakarta.servlet.Filter;

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
  public <T extends B> T dispatcherServletCustomizer(DispatcherServletCustomizer customizer) {
    getMockMvcBuilder().addDispatcherServletCustomizer(customizer);
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
