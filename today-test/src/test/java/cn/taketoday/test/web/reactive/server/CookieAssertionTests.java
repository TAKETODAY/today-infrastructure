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
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.mock.http.client.reactive.MockClientHttpRequest;
import cn.taketoday.mock.http.client.reactive.MockClientHttpResponse;

import java.net.URI;
import java.time.Duration;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link CookieAssertions}
 * @author Rossen Stoyanchev
 */
public class CookieAssertionTests {

	private final ResponseCookie cookie = ResponseCookie.from("foo", "bar")
			.maxAge(Duration.ofMinutes(30))
			.domain("foo.com")
			.path("/foo")
			.secure(true)
			.httpOnly(true)
			.sameSite("Lax")
			.build();

	private final CookieAssertions assertions = cookieAssertions(cookie);


	@Test
	void valueEquals() {
		assertions.valueEquals("foo", "bar");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.valueEquals("what?!", "bar"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.valueEquals("foo", "what?!"));
	}

	@Test
	void value() {
		assertions.value("foo", equalTo("bar"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.value("foo", equalTo("what?!")));
	}

	@Test
	void exists() {
		assertions.exists("foo");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.exists("what?!"));
	}

	@Test
	void doesNotExist() {
		assertions.doesNotExist("what?!");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.doesNotExist("foo"));
	}

	@Test
	void maxAge() {
		assertions.maxAge("foo", Duration.ofMinutes(30));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.maxAge("foo", Duration.ofMinutes(29)));

		assertions.maxAge("foo", equalTo(Duration.ofMinutes(30).getSeconds()));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.maxAge("foo", equalTo(Duration.ofMinutes(29).getSeconds())));
	}

	@Test
	void domain() {
		assertions.domain("foo", "foo.com");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.domain("foo", "what.com"));

		assertions.domain("foo", equalTo("foo.com"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.domain("foo", equalTo("what.com")));
	}

	@Test
	void path() {
		assertions.path("foo", "/foo");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.path("foo", "/what"));

		assertions.path("foo", equalTo("/foo"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.path("foo", equalTo("/what")));
	}

	@Test
	void secure() {
		assertions.secure("foo", true);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.secure("foo", false));
	}

	@Test
	void httpOnly() {
		assertions.httpOnly("foo", true);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.httpOnly("foo", false));
	}

	@Test
	void sameSite() {
		assertions.sameSite("foo", "Lax");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.sameSite("foo", "Strict"));
	}


	private CookieAssertions cookieAssertions(ResponseCookie cookie) {
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/"));
		MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
		response.getCookies().add(cookie.getName(), cookie);

		ExchangeResult result = new ExchangeResult(
				request, response, Mono.empty(), Mono.empty(), Duration.ZERO, null, null);

		return new CookieAssertions(result, mock(WebTestClient.ResponseSpec.class));
	}

}
