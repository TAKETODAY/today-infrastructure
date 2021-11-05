/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.http.client;

import cn.taketoday.web.http.client.support.HttpAccessor;

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
 * @since 4.0
 * @see HttpAccessor#getClientHttpRequestInitializers()
 */
@FunctionalInterface
public interface ClientHttpRequestInitializer {

	/**
	 * Initialize the given client HTTP request.
	 * @param request the request to configure
	 */
	void initialize(ClientHttpRequest request);

}
