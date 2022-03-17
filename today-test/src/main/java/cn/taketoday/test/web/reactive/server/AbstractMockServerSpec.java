/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.web.reactive.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.server.WebFilter;
import cn.taketoday.web.server.adapter.WebHttpHandlerBuilder;
import cn.taketoday.web.server.session.DefaultWebSessionManager;
import cn.taketoday.web.server.session.WebSessionManager;

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
  private List<WebFilter> filters;

  @Nullable
  private WebSessionManager sessionManager;

  @Nullable
  private List<MockServerConfigurer> configurers;

  AbstractMockServerSpec() {
    // Default instance to be re-used across requests, unless one is configured explicitly
    this.sessionManager = new DefaultWebSessionManager();
  }

  @Override
  public <T extends B> T webFilter(WebFilter... filters) {
    if (filters.length > 0) {
      this.filters = (this.filters != null ? this.filters : new ArrayList<>(4));
      this.filters.addAll(Arrays.asList(filters));
    }
    return self();
  }

  @Override
  public <T extends B> T webSessionManager(WebSessionManager sessionManager) {
    this.sessionManager = sessionManager;
    return self();
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
    WebHttpHandlerBuilder builder = initHttpHandlerBuilder();
    if (!CollectionUtils.isEmpty(this.filters)) {
      builder.filters(theFilters -> theFilters.addAll(0, this.filters));
    }
    if (!builder.hasSessionManager() && this.sessionManager != null) {
      builder.sessionManager(this.sessionManager);
    }
    if (!CollectionUtils.isEmpty(this.configurers)) {
      this.configurers.forEach(configurer -> configurer.beforeServerCreated(builder));
    }
    return new DefaultWebTestClientBuilder(builder);
  }

  /**
   * Sub-classes must create an {@code WebHttpHandlerBuilder} that will then
   * be used to create the HttpHandler for the mock server.
   */
  protected abstract WebHttpHandlerBuilder initHttpHandlerBuilder();

  @Override
  public WebTestClient build() {
    return configureClient().build();
  }

}
