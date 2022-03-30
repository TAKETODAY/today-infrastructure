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
import cn.taketoday.http.CacheControl;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.mock.http.client.reactive.MockClientHttpRequest;
import cn.taketoday.mock.http.client.reactive.MockClientHttpResponse;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link HeaderAssertions}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class HeaderAssertionTests {

	@Test
	void valueEquals() {
		HttpHeaders headers = HttpHeaders.create();
		headers.add("foo", "bar");
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.valueEquals("foo", "bar");

		// Missing header
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.valueEquals("what?!", "bar"));

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.valueEquals("foo", "what?!"));

		// Wrong # of values
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.valueEquals("foo", "bar", "what?!"));
	}

	@Test
	void valueEqualsWithMultipleValues() {
		HttpHeaders headers = HttpHeaders.create();
		headers.add("foo", "bar");
		headers.add("foo", "baz");
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.valueEquals("foo", "bar", "baz");

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.valueEquals("foo", "bar", "what?!"));

		// Too few values
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.valueEquals("foo", "bar"));
	}

	@Test
	void valueMatches() {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.valueMatches("Content-Type", ".*UTF-8.*");

		// Wrong pattern
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valueMatches("Content-Type", ".*ISO-8859-1.*"))
				.satisfies(ex -> assertThat(ex).hasMessage("Response header " +
						"'Content-Type'=[application/json;charset=UTF-8] does not match " +
						"[.*ISO-8859-1.*]"));
	}

	@Test
	void valuesMatch() {
		HttpHeaders headers = HttpHeaders.create();
		headers.add("foo", "value1");
		headers.add("foo", "value2");
		headers.add("foo", "value3");
		HeaderAssertions assertions = headerAssertions(headers);

		assertions.valuesMatch("foo", "val.*1", "val.*2", "val.*3");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valuesMatch("foo", ".*", "val.*5"))
				.satisfies(ex -> assertThat(ex).hasMessage(
						"Response header 'foo' has fewer or more values [value1, value2, value3] " +
								"than number of patterns to match with [.*, val.*5]"));

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valuesMatch("foo", ".*", "val.*5", ".*"))
				.satisfies(ex -> assertThat(ex).hasMessage(
						"Response header 'foo'[1]='value2' does not match 'val.*5'"));
	}

	@Test
	void valueMatcher() {
		HttpHeaders headers = HttpHeaders.create();
		headers.add("foo", "bar");
		HeaderAssertions assertions = headerAssertions(headers);

		assertions.value("foo", containsString("a"));
	}

	@Test
	void valuesMatcher() {
		HttpHeaders headers = HttpHeaders.create();
		headers.add("foo", "bar");
		headers.add("foo", "baz");
		HeaderAssertions assertions = headerAssertions(headers);

		assertions.values("foo", hasItems("bar", "baz"));
	}

	@Test
	void exists() {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.exists("Content-Type");

		// Header should not exist
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.exists("Framework"))
			.satisfies(ex -> assertThat(ex).hasMessage("Response header 'Framework' does not exist"));
	}

	@Test
	void doesNotExist() {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.doesNotExist("Framework");

		// Existing header
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.doesNotExist("Content-Type"))
			.satisfies(ex -> assertThat(ex).hasMessage("Response header " +
					"'Content-Type' exists with value=[application/json;charset=UTF-8]"));
	}

	@Test
	void contentTypeCompatibleWith() {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.APPLICATION_XML);
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.contentTypeCompatibleWith(MediaType.parseMediaType("application/*"));

		// MediaTypes not compatible
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertions.contentTypeCompatibleWith(MediaType.TEXT_XML))
			.withMessage("Response header 'Content-Type'=[application/xml] is not compatible with [text/xml]");
	}

	@Test
	void cacheControl() {
		CacheControl control = CacheControl.maxAge(1, TimeUnit.HOURS).noTransform();

		HttpHeaders headers = HttpHeaders.create();
		headers.setCacheControl(control.getHeaderValue());
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.cacheControl(control);

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.cacheControl(CacheControl.noStore()));
	}

	@Test
	void expires() {
		HttpHeaders headers = HttpHeaders.create();
		ZonedDateTime expires = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		headers.setExpires(expires);
		HeaderAssertions assertions = headerAssertions(headers);
		assertions.expires(expires.toInstant().toEpochMilli());

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.expires(expires.toInstant().toEpochMilli() + 1));
	}

	@Test
	void lastModified() {
		HttpHeaders headers = HttpHeaders.create();
		ZonedDateTime lastModified = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		headers.setLastModified(lastModified.toInstant().toEpochMilli());
		HeaderAssertions assertions = headerAssertions(headers);
		assertions.lastModified(lastModified.toInstant().toEpochMilli());

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.lastModified(lastModified.toInstant().toEpochMilli() + 1));
	}

	private HeaderAssertions headerAssertions(HttpHeaders responseHeaders) {
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/"));
		MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
		response.getHeaders().putAll(responseHeaders);

		ExchangeResult result = new ExchangeResult(
				request, response, Mono.empty(), Mono.empty(), Duration.ZERO, null, null);

		return new HeaderAssertions(result, mock(WebTestClient.ResponseSpec.class));
	}

}
