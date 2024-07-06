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

package cn.taketoday.http.client;

import cn.taketoday.http.client.support.HttpAccessor;

/**
 * Callback interface for initializing a {@link ClientHttpRequest} prior to it
 * being used.
 *
 * <p>Typically used with {@link HttpAccessor} and subclasses such as
 * {@link cn.taketoday.web.client.RestTemplate RestTemplate} to apply
 * consistent settings or headers to each request.
 *
 * <p>Unlike {@link ClientHttpRequestInterceptor}, this interface can apply
 * customizations without needing to read the entire request body into memory.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HttpAccessor#getHttpRequestInitializers()
 * @since 4.0
 */
@FunctionalInterface
public interface ClientHttpRequestInitializer {

  /**
   * Initialize the given client HTTP request.
   *
   * @param request the request to configure
   */
  void initialize(ClientHttpRequest request);

}
