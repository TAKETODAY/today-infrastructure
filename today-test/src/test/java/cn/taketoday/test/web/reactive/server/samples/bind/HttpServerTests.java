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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.web.reactive.function.server.RouterFunctions;
import cn.taketoday.web.reactive.function.server.ServerResponse;
import cn.taketoday.web.testfixture.http.server.reactive.bootstrap.ReactorHttpServer;

import static cn.taketoday.web.reactive.function.server.RequestPredicates.GET;
import static cn.taketoday.web.reactive.function.server.RouterFunctions.route;

/**
 * Sample tests demonstrating live server integration tests.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HttpServerTests {

	private ReactorHttpServer server;

	private WebTestClient client;


	@BeforeEach
	public void start() throws Exception {
		HttpHandler httpHandler = RouterFunctions.toHttpHandler(
				route(GET("/test"), request -> ServerResponse.ok().bodyValue("It works!")));

		this.server = new ReactorHttpServer();
		this.server.setHandler(httpHandler);
		this.server.afterPropertiesSet();
		this.server.start();

		this.client = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + this.server.getPort())
				.build();
	}

	@AfterEach
	public void stop() {
		this.server.stop();
	}


	@Test
	public void test() {
		this.client.get().uri("/test")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}

}
