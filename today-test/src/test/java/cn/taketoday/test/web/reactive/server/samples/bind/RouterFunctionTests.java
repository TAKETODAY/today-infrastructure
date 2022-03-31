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

package cn.taketoday.test.web.reactive.server.samples.bind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.web.reactive.function.server.RouterFunction;
import cn.taketoday.web.reactive.function.server.ServerResponse;

import static cn.taketoday.web.reactive.function.server.RequestPredicates.GET;
import static cn.taketoday.web.reactive.function.server.RouterFunctions.route;

/**
 * Sample tests demonstrating "mock" server tests binding to a RouterFunction.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class RouterFunctionTests {

	private WebTestClient testClient;


	@BeforeEach
	public void setUp() throws Exception {

		RouterFunction<?> route = route(GET("/test"), request ->
				ServerResponse.ok().bodyValue("It works!"));

		this.testClient = WebTestClient.bindToRouterFunction(route).build();
	}

	@Test
	public void test() throws Exception {
		this.testClient.get().uri("/test")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}

}
