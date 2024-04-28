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

/**
 * Abstractions for reactive HTTP server support including a
 * {@link cn.taketoday.http.server.reactive.ServerHttpRequest} and
 * {@link cn.taketoday.http.server.reactive.ServerHttpResponse} along with an
 * {@link cn.taketoday.http.server.reactive.HttpHandler} for processing.
 *
 * <p>Also provides implementations adapting to different runtimes
 * including Netty + Reactor IO.
 */
@NonNullApi
@NonNullFields
package cn.taketoday.http.server.reactive;

import cn.taketoday.lang.NonNullApi;
import cn.taketoday.lang.NonNullFields;
