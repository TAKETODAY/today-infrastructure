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
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.web.server.ServerWebExchange;
import cn.taketoday.web.server.WebFilter;
import cn.taketoday.web.server.WebFilterChain;
import cn.taketoday.web.server.adapter.WebHttpHandlerBuilder;

import java.nio.charset.StandardCharsets;

import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link AbstractMockServerSpec}.
 * @author Rossen Stoyanchev
 */
public class MockServerSpecTests {

	private final TestMockServerSpec serverSpec = new TestMockServerSpec();


	@Test
	public void applyFiltersAfterConfigurerAdded() {

		this.serverSpec.webFilter(new TestWebFilter("A"));

		this.serverSpec.apply(new MockServerConfigurer() {

			@Override
			public void afterConfigureAdded(WebTestClient.MockServerSpec<?> spec) {
				spec.webFilter(new TestWebFilter("B"));
			}
		});

		this.serverSpec.build().get().uri("/")
				.exchange()
				.expectBody(String.class)
				.consumeWith(result -> assertThat(
						result.getResponseBody()).contains("test-attribute=:A:B"));
	}

	@Test
	public void applyFiltersBeforeServerCreated() {

		this.serverSpec.webFilter(new TestWebFilter("App-A"));
		this.serverSpec.webFilter(new TestWebFilter("App-B"));

		this.serverSpec.apply(new MockServerConfigurer() {

			@Override
			public void beforeServerCreated(WebHttpHandlerBuilder builder) {
				builder.filters(filters -> {
					filters.add(0, new TestWebFilter("Fwk-A"));
					filters.add(1, new TestWebFilter("Fwk-B"));
				});
			}
		});

		this.serverSpec.build().get().uri("/")
				.exchange()
				.expectBody(String.class)
				.consumeWith(result -> assertThat(
						result.getResponseBody()).contains("test-attribute=:Fwk-A:Fwk-B:App-A:App-B"));
	}


	private static class TestMockServerSpec extends AbstractMockServerSpec<TestMockServerSpec> {

		@Override
		protected WebHttpHandlerBuilder initHttpHandlerBuilder() {
			return WebHttpHandlerBuilder.webHandler(exchange -> {
				DefaultDataBufferFactory factory = DefaultDataBufferFactory.sharedInstance;
				String text = exchange.getAttributes().toString();
				DataBuffer buffer = factory.wrap(text.getBytes(StandardCharsets.UTF_8));
				return exchange.getResponse().writeWith(Mono.just(buffer));
			});
		}
	}

	private static class TestWebFilter implements WebFilter {

		private final String name;

		TestWebFilter(String name) {
			this.name = name;
		}

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
			String name = "test-attribute";
			String value = exchange.getAttributeOrDefault(name, "");
			exchange.getAttributes().put(name, value + ":" + this.name);
			return chain.filter(exchange);
		}
	}

}
