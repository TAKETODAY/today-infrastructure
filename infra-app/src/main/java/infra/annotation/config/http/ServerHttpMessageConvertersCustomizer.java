/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.annotation.config.http;

import infra.http.converter.HttpMessageConverters;
import infra.http.converter.HttpMessageConverters.ServerBuilder;

/**
 * Callback interface that can be used to customize a {@link HttpMessageConverters} for
 * server usage.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface ServerHttpMessageConvertersCustomizer {

  /**
   * Callback to customize a {@link ServerBuilder} instance.
   *
   * @param builder the builder to customize
   */
  void customize(ServerBuilder builder);

}
