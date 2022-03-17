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

import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Contract that frameworks or applications can use to pre-package a set of
 * customizations to a {@link WebTestClient.Builder} and expose that
 * as a shortcut.
 *
 * @author Rossen Stoyanchev
 * @see MockServerConfigurer
 * @since 4.0
 */
public interface WebTestClientConfigurer {

  /**
   * Invoked once only, immediately (i.e. before this method returns).
   *
   * @param builder the WebTestClient builder to make changes to
   * @param httpHandlerBuilder the builder for the "mock server" HttpHandler
   * this client was configured for "mock server" testing
   * @param connector the connector for "live" integration tests if this
   * server was configured for live integration testing
   */
  void afterConfigurerAdded(WebTestClient.Builder builder,
          @Nullable WebHttpHandlerBuilder httpHandlerBuilder,
          @Nullable ClientHttpConnector connector);

}
