/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.test.web.reactive.server;

import infra.test.web.reactive.server.WebTestClient.Builder;

/**
 * A customizer for a {@link Builder}. Any {@code WebTestClientBuilderCustomizer} beans
 * found in the application context will be {@link #customize called} to customize the
 * auto-configured {@link Builder}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2.2
 */
@FunctionalInterface
public interface WebTestClientBuilderCustomizer {

  /**
   * Customize the given {@code builder}.
   *
   * @param builder the builder
   */
  void customize(Builder builder);

}
