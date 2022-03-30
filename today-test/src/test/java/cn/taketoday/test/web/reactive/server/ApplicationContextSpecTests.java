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
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.mock.web.server.MockWebSession;
import cn.taketoday.web.reactive.config.EnableWebFlux;
import cn.taketoday.web.reactive.function.server.RouterFunction;
import cn.taketoday.web.reactive.function.server.RouterFunctions;
import cn.taketoday.web.reactive.function.server.ServerResponse;
import cn.taketoday.web.server.session.WebSessionManager;

import reactor.core.publisher.Mono;

/**
 * Unit tests with {@link ApplicationContextSpec}.
 * @author Rossen Stoyanchev
 */
public class ApplicationContextSpecTests {


	@Test // SPR-17094
	public void sessionManagerBean() {
		ApplicationContext context = new AnnotationConfigApplicationContext(WebConfig.class);
		ApplicationContextSpec spec = new ApplicationContextSpec(context);
		WebTestClient testClient = spec.configureClient().build();

		for (int i=0; i < 2; i++) {
			testClient.get().uri("/sessionClassName")
					.exchange()
					.expectStatus().isOk()
					.expectBody(String.class).isEqualTo("MockWebSession");
		}
	}


	@Configuration
	@EnableWebFlux
	static class WebConfig {

		@Bean
		public RouterFunction<?> handler() {
			return RouterFunctions.route()
					.GET("/sessionClassName", request ->
							request.session().flatMap(session -> {
								String className = session.getClass().getSimpleName();
								return ServerResponse.ok().bodyValue(className);
							}))
					.build();
		}

		@Bean
		public WebSessionManager webSessionManager() {
			MockWebSession session = new MockWebSession();
			return exchange -> Mono.just(session);
		}
	}

}
