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

package cn.taketoday.test.web.reactive.server.samples;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.reactive.server.MockServerConfigurer;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.reactive.server.WebTestClientConfigurer;
import cn.taketoday.util.Assert;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.RestController;
import cn.taketoday.web.server.ServerWebExchange;
import cn.taketoday.web.server.WebFilter;
import cn.taketoday.web.server.WebFilterChain;
import cn.taketoday.web.server.adapter.WebHttpHandlerBuilder;

import java.security.Principal;

import reactor.core.publisher.Mono;

/**
 * Samples tests that demonstrate applying ServerWebExchange initialization.
 *
 * @author Rossen Stoyanchev
 */
class ExchangeMutatorTests {

	private final WebTestClient webTestClient = WebTestClient.bindToController(new TestController())
			.apply(identity("Pablo"))
			.build();


	@Test
	void useGloballyConfiguredIdentity() {
		this.webTestClient.get().uri("/userIdentity")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Hello Pablo!");
	}

	@Test
	void useLocallyConfiguredIdentity() {
		this.webTestClient
				.mutateWith(identity("Giovanni"))
				.get().uri("/userIdentity")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Hello Giovanni!");
	}


	private static IdentityConfigurer identity(String userName) {
		return new IdentityConfigurer(userName);
	}


	@RestController
	static class TestController {

		@GetMapping("/userIdentity")
		public String handle(Principal principal) {
			return "Hello " + principal.getName() + "!";
		}
	}

	private static class TestUser implements Principal {

		private final String name;

		TestUser(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}
	}

	private static class IdentityConfigurer implements MockServerConfigurer, WebTestClientConfigurer {

		private final IdentityFilter filter;


		public IdentityConfigurer(String userName) {
			this.filter = new IdentityFilter(userName);
		}

		@Override
		public void beforeServerCreated(WebHttpHandlerBuilder builder) {
			builder.filters(filters -> filters.add(0, this.filter));
		}

		@Override
		public void afterConfigurerAdded(WebTestClient.Builder builder,
				@Nullable WebHttpHandlerBuilder httpHandlerBuilder,
				@Nullable ClientHttpConnector connector) {

			Assert.notNull(httpHandlerBuilder, "Not a mock server");
			httpHandlerBuilder.filters(filters -> {
				filters.removeIf(filter -> filter instanceof IdentityFilter);
				filters.add(0, this.filter);
			});
		}
	}

	private static class IdentityFilter implements WebFilter {

		private final Mono<Principal> userMono;


		IdentityFilter(String userName) {
			this.userMono = Mono.just(new TestUser(userName));
		}

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
			exchange = exchange.mutate().principal(this.userMono).build();
			return chain.filter(exchange);
		}
	}

}
