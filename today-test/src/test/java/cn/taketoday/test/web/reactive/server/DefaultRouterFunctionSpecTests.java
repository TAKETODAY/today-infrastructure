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

import org.junit.jupiter.api.Test;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.reactive.function.server.HandlerStrategies;
import cn.taketoday.web.reactive.function.server.RouterFunction;
import cn.taketoday.web.reactive.function.server.RouterFunctions;
import cn.taketoday.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link DefaultRouterFunctionSpec}.
 * @author Rossen Stoyanchev
 */
public class DefaultRouterFunctionSpecTests {

	@Test
	public void webFilter() {

		RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
				.GET("/", request -> ServerResponse.ok().build())
				.build();

		new DefaultRouterFunctionSpec(routerFunction)
				.handlerStrategies(HandlerStrategies.builder()
						.webFilter((exchange, chain) -> {
							exchange.getResponse().getHeaders().set("foo", "123");
							return chain.filter(exchange);
						})
						.build())
				.build()
				.get()
				.uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("foo", "123");
	}

	@Test
	public void exceptionHandler() {

		RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
				.GET("/error", request -> Mono.error(new IllegalStateException("boo")))
				.build();

		new DefaultRouterFunctionSpec(routerFunction)
				.handlerStrategies(HandlerStrategies.builder()
						.exceptionHandler((exchange, ex) -> {
							exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
							return Mono.empty();
						})
						.build())
				.build()
				.get()
				.uri("/error")
				.exchange()
				.expectStatus().isBadRequest();
	}
}
