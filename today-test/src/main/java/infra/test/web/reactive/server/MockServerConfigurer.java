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

}
