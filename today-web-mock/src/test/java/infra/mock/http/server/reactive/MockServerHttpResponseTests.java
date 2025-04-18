/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.mock.http.server.reactive;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import infra.http.HttpHeaders;
import infra.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MockServerHttpResponse}.
 * @author Rossen Stoyanchev
 */
class MockServerHttpResponseTests {

	@Test
	void cookieHeaderSet() throws Exception {

		ResponseCookie foo11 = ResponseCookie.from("foo1", "bar1").build();
		ResponseCookie foo12 = ResponseCookie.from("foo1", "bar2").build();
		ResponseCookie foo21 = ResponseCookie.from("foo2", "baz1").build();
		ResponseCookie foo22 = ResponseCookie.from("foo2", "baz2").build();

		MockServerHttpResponse response = new MockServerHttpResponse();
		response.addCookie(foo11);
		response.addCookie(foo12);
		response.addCookie(foo21);
		response.addCookie(foo22);

		response.applyCookies();

		assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isEqualTo(Arrays.asList("foo1=bar1", "foo1=bar2", "foo2=baz1", "foo2=baz2"));
	}

}
