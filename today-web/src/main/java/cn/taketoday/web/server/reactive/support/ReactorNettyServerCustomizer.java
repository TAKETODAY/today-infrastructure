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

package cn.taketoday.web.server.reactive.support;

import java.util.function.Function;

import reactor.netty.http.server.HttpServer;

/**
 * Mapping function that can be used to customize a Reactor Netty server instance.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ReactorNettyReactiveWebServerFactory
 * @since 4.0
 */
@FunctionalInterface
public interface ReactorNettyServerCustomizer extends Function<HttpServer, HttpServer> {

}
