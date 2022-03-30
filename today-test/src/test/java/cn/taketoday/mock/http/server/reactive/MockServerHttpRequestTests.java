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

package cn.taketoday.mock.http.server.reactive;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.mock.http.server.reactive.MockServerHttpRequest;
import cn.taketoday.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Named.named;

/**
 * Unit tests for {@link MockServerHttpRequest}.
 * @author Rossen Stoyanchev
 */
class MockServerHttpRequestTests {

	@Test
	void cookieHeaderSet() {
		HttpCookie foo11 = new HttpCookie("foo1", "bar1");
		HttpCookie foo12 = new HttpCookie("foo1", "bar2");
		HttpCookie foo21 = new HttpCookie("foo2", "baz1");
		HttpCookie foo22 = new HttpCookie("foo2", "baz2");

		MockServerHttpRequest request = MockServerHttpRequest.get("/")
				.cookie(foo11, foo12, foo21, foo22).build();

		assertThat(request.getCookies().get("foo1")).isEqualTo(Arrays.asList(foo11, foo12));
		assertThat(request.getCookies().get("foo2")).isEqualTo(Arrays.asList(foo21, foo22));
		assertThat(request.getHeaders().get(HttpHeaders.COOKIE)).isEqualTo(Arrays.asList("foo1=bar1", "foo1=bar2", "foo2=baz1", "foo2=baz2"));
	}

	@Test
	void queryParams() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/foo bar?a=b")
				.queryParam("name A", "value A1", "value A2")
				.queryParam("name B", "value B1")
				.build();

		assertThat(request.getURI().toString()).isEqualTo("/foo%20bar?a=b&name%20A=value%20A1&name%20A=value%20A2&name%20B=value%20B1");
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource
	void httpMethodNotNullOrEmpty(ThrowingCallable callable) {
		assertThatIllegalArgumentException()
			.isThrownBy(callable)
			.withMessageContaining("HTTP method is required.");
	}

	@SuppressWarnings("deprecation")
	static Stream<Named<ThrowingCallable>> httpMethodNotNullOrEmpty() {
		String uriTemplate = "/foo bar?a=b";
		return Stream.of(
				named("null HttpMethod, URI", () -> MockServerHttpRequest.method(null, UriComponentsBuilder.fromUriString(uriTemplate).build("")).build()),
				named("null HttpMethod, uriTemplate", () -> MockServerHttpRequest.method((HttpMethod) null, uriTemplate).build()),
				named("null String, uriTemplate", () -> MockServerHttpRequest.method((String) null, uriTemplate).build()),
				named("empty String, uriTemplate", () -> MockServerHttpRequest.method("", uriTemplate).build()),
				named("blank String, uriTemplate", () -> MockServerHttpRequest.method("   ", uriTemplate).build())
		);
	}

}
