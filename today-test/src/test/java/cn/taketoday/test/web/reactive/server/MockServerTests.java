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
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.server.reactive.ServerHttpResponse;

import java.util.Arrays;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test scenarios involving a mock server.
 * @author Rossen Stoyanchev
 */
public class MockServerTests {


	@Test // SPR-15674 (in comments)
	public void mutateDoesNotCreateNewSession() {

		WebTestClient client = WebTestClient
				.bindToWebHandler(exchange -> {
					if (exchange.getRequest().getURI().getPath().equals("/set")) {
						return exchange.getSession()
								.doOnNext(session -> session.getAttributes().put("foo", "bar"))
								.then();
					}
					else {
						return exchange.getSession()
								.map(session -> session.getAttributeOrDefault("foo", "none"))
								.flatMap(value -> {
									DataBuffer buffer = toDataBuffer(value);
									return exchange.getResponse().writeWith(Mono.just(buffer));
								});
					}
				})
				.build();

		// Set the session attribute
		EntityExchangeResult<Void> result = client.get().uri("/set").exchange()
				.expectStatus().isOk().expectBody().isEmpty();

		ResponseCookie session = result.getResponseCookies().getFirst("SESSION");

		// Now get attribute
		client.mutate().build()
				.get().uri("/get")
				.cookie(session.getName(), session.getValue())
				.exchange()
				.expectBody(String.class).isEqualTo("bar");
	}

	@Test // SPR-16059
	public void mutateDoesCopy() {

		WebTestClient.Builder builder = WebTestClient
				.bindToWebHandler(exchange -> exchange.getResponse().setComplete())
				.configureClient();

		builder.filter((request, next) -> next.exchange(request));
		builder.defaultHeader("foo", "bar");
		builder.defaultCookie("foo", "bar");
		WebTestClient client1 = builder.build();

		builder.filter((request, next) -> next.exchange(request));
		builder.defaultHeader("baz", "qux");
		builder.defaultCookie("baz", "qux");
		WebTestClient client2 = builder.build();

		WebTestClient.Builder mutatedBuilder = client1.mutate();

		mutatedBuilder.filter((request, next) -> next.exchange(request));
		mutatedBuilder.defaultHeader("baz", "qux");
		mutatedBuilder.defaultCookie("baz", "qux");
		WebTestClient clientFromMutatedBuilder = mutatedBuilder.build();

		client1.mutate().filters(filters -> assertThat(filters.size()).isEqualTo(1));
		client1.mutate().defaultHeaders(headers -> assertThat(headers.size()).isEqualTo(1));
		client1.mutate().defaultCookies(cookies -> assertThat(cookies.size()).isEqualTo(1));

		client2.mutate().filters(filters -> assertThat(filters.size()).isEqualTo(2));
		client2.mutate().defaultHeaders(headers -> assertThat(headers.size()).isEqualTo(2));
		client2.mutate().defaultCookies(cookies -> assertThat(cookies.size()).isEqualTo(2));

		clientFromMutatedBuilder.mutate().filters(filters -> assertThat(filters.size()).isEqualTo(2));
		clientFromMutatedBuilder.mutate().defaultHeaders(headers -> assertThat(headers.size()).isEqualTo(2));
		clientFromMutatedBuilder.mutate().defaultCookies(cookies -> assertThat(cookies.size()).isEqualTo(2));
	}

	@Test // SPR-16124
	public void exchangeResultHasCookieHeaders() {

		ExchangeResult result = WebTestClient
				.bindToWebHandler(exchange -> {
					ServerHttpResponse response = exchange.getResponse();
					if (exchange.getRequest().getURI().getPath().equals("/cookie")) {
						response.addCookie(ResponseCookie.from("a", "alpha").path("/pathA").build());
						response.addCookie(ResponseCookie.from("b", "beta").path("/pathB").build());
					}
					else {
						response.setStatusCode(HttpStatus.NOT_FOUND);
					}
					return response.setComplete();
				})
				.build()
				.get().uri("/cookie").cookie("a", "alpha").cookie("b", "beta")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals(HttpHeaders.SET_COOKIE, "a=alpha; Path=/pathA", "b=beta; Path=/pathB")
				.expectBody().isEmpty();

		assertThat(result.getRequestHeaders().get(HttpHeaders.COOKIE)).isEqualTo(Arrays.asList("a=alpha", "b=beta"));
	}

	@Test
	public void responseBodyContentWithFluxExchangeResult() {

		FluxExchangeResult<String> result = WebTestClient
				.bindToWebHandler(exchange -> {
					ServerHttpResponse response = exchange.getResponse();
					response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
					return response.writeWith(Flux.just(toDataBuffer("body")));
				})
				.build()
				.get().uri("/")
				.exchange()
				.expectStatus().isOk()
				.returnResult(String.class);

		// Get the raw content without consuming the response body flux..
		byte[] bytes = result.getResponseBodyContent();

		assertThat(bytes).isNotNull();
		assertThat(new String(bytes, UTF_8)).isEqualTo("body");
	}


	private DataBuffer toDataBuffer(String value) {
		byte[] bytes = value.getBytes(UTF_8);
		return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
	}

}
