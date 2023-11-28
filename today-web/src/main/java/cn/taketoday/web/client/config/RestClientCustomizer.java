/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.client.config;

import cn.taketoday.web.client.RestClient;

/**
 * Callback interface that can be used to customize a
 * {@link cn.taketoday.web.client.RestClient.Builder RestClient.Builder}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface RestClientCustomizer {

  /**
   * Callback to customize a {@link cn.taketoday.web.client.RestClient.Builder
   * RestClient.Builder} instance.
   *
   * @param restClientBuilder the client builder to customize
   */
  void customize(RestClient.Builder restClientBuilder);

}
