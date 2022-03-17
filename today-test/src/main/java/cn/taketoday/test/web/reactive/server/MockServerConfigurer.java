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

import cn.taketoday.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Contract that frameworks or applications can use to pre-package a set of
 * customizations to a {@link WebTestClient.MockServerSpec} and expose that
 * as a shortcut.
 *
 * <p>An implementation of this interface can be plugged in via
 * {@link WebTestClient.MockServerSpec#apply} where instances are likely obtained
 * via static methods, e.g.:
 *
 * <pre class="code">
 * import static org.example.ExampleSetup.securitySetup;
 *
 * // ...
 *
 * WebTestClient.bindToController(new TestController())
 *     .apply(securitySetup("foo","bar"))
 *     .build();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @see WebTestClientConfigurer
 * @since 4.0
 */
public interface MockServerConfigurer {

  /**
   * Invoked immediately, i.e. before this method returns.
   *
   * @param serverSpec the serverSpec to which the configurer is added
   */
  default void afterConfigureAdded(WebTestClient.MockServerSpec<?> serverSpec) {
  }

  /**
   * Invoked just before the mock server is built. Use this hook to inspect
   * and/or modify application-declared filters and exception handlers.
   *
   * @param builder the builder for the {@code HttpHandler} that will handle
   * requests (i.e. the mock server)
   */
  default void beforeServerCreated(WebHttpHandlerBuilder builder) {
  }

}
