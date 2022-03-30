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
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ReactiveHttpOutputMessage;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.client.reactive.ClientHttpResponse;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.mock.http.server.reactive.MockServerHttpRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpHandlerConnector}.
 * @author Rossen Stoyanchev
 */
public class HttpHandlerConnectorTests {


	@Test
	public void adaptRequest() {

		TestHttpHandler handler = new TestHttpHandler(response -> {
			response.setStatusCode(HttpStatus.OK);
			return response.setComplete();
		});

		new HttpHandlerConnector(handler).connect(HttpMethod.POST, URI.create("/custom-path"),
				request -> {
					request.getHeaders().put("custom-header", Arrays.asList("h0", "h1"));
					request.getCookies().add("custom-cookie", new HttpCookie("custom-cookie", "c0"));
					return request.writeWith(Mono.just(toDataBuffer("Custom body")));
				}).block(Duration.ofSeconds(5));

		MockServerHttpRequest request = (MockServerHttpRequest) handler.getSavedRequest();
		assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
		assertThat(request.getURI().toString()).isEqualTo("/custom-path");

		HttpHeaders headers = request.getHeaders();
		assertThat(headers.get("custom-header")).isEqualTo(Arrays.asList("h0", "h1"));
		assertThat(request.getCookies().getFirst("custom-cookie")).isEqualTo(new HttpCookie("custom-cookie", "c0"));
		assertThat(headers.get(HttpHeaders.COOKIE)).isEqualTo(Collections.singletonList("custom-cookie=c0"));

		DataBuffer buffer = request.getBody().blockFirst(Duration.ZERO);
		assertThat(buffer.toString(UTF_8)).isEqualTo("Custom body");
	}

	@Test
	public void adaptResponse() {

		ResponseCookie cookie = ResponseCookie.from("custom-cookie", "c0").build();

		TestHttpHandler handler = new TestHttpHandler(response -> {
			response.setStatusCode(HttpStatus.OK);
			response.getHeaders().put("custom-header", Arrays.asList("h0", "h1"));
			response.addCookie(cookie);
			return response.writeWith(Mono.just(toDataBuffer("Custom body")));
		});

		ClientHttpResponse response = new HttpHandlerConnector(handler)
				.connect(HttpMethod.GET, URI.create("/custom-path"), ReactiveHttpOutputMessage::setComplete)
				.block(Duration.ofSeconds(5));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		HttpHeaders headers = response.getHeaders();
		assertThat(headers.get("custom-header")).isEqualTo(Arrays.asList("h0", "h1"));
		assertThat(response.getCookies().getFirst("custom-cookie")).isEqualTo(cookie);
		assertThat(headers.get(HttpHeaders.SET_COOKIE)).isEqualTo(Collections.singletonList("custom-cookie=c0"));

		DataBuffer buffer = response.getBody().blockFirst(Duration.ZERO);
		assertThat(buffer.toString(UTF_8)).isEqualTo("Custom body");
	}

	@Test // gh-23936
	public void handlerOnNonBlockingThread() {

		TestHttpHandler handler = new TestHttpHandler(response -> {

			assertThat(Schedulers.isInNonBlockingThread()).isTrue();

			response.setStatusCode(HttpStatus.OK);
			return response.setComplete();
		});

		new HttpHandlerConnector(handler)
				.connect(HttpMethod.POST, URI.create("/path"), request -> request.writeWith(Mono.empty()))
				.block(Duration.ofSeconds(5));
	}

	private DataBuffer toDataBuffer(String body) {
		return DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(UTF_8));
	}


	private static class TestHttpHandler implements HttpHandler {

		private ServerHttpRequest savedRequest;

		private final Function<ServerHttpResponse, Mono<Void>> responseMonoFunction;


		public TestHttpHandler(Function<ServerHttpResponse, Mono<Void>> function) {
			this.responseMonoFunction = function;
		}

		public ServerHttpRequest getSavedRequest() {
			return this.savedRequest;
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			this.savedRequest = request;
			return this.responseMonoFunction.apply(response);
		}
	}

}
