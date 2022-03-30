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
import cn.taketoday.mock.http.client.reactive.MockClientHttpRequest;
import cn.taketoday.mock.http.client.reactive.MockClientHttpResponse;

import java.net.URI;
import java.time.Duration;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link StatusAssertions}.
 *
 * @author Rossen Stoyanchev
 */
class StatusAssertionTests {

	@Test
	void isEqualTo() {
		StatusAssertions assertions = statusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.isEqualTo(HttpStatus.CONFLICT);
		assertions.isEqualTo(409);

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.isEqualTo(HttpStatus.REQUEST_TIMEOUT));

		// Wrong status value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.isEqualTo(408));
	}

	@Test  // gh-23630
	void isEqualToWithCustomStatus() {
		statusAssertions(600).isEqualTo(600);
	}

	@Test
	void reasonEquals() {
		StatusAssertions assertions = statusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.reasonEquals("Conflict");

		// Wrong reason
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.reasonEquals("Request Timeout"));
	}

	@Test
	void statusSeries1xx() {
		StatusAssertions assertions = statusAssertions(HttpStatus.CONTINUE);

		// Success
		assertions.is1xxInformational();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.is2xxSuccessful());
	}

	@Test
	void statusSeries2xx() {
		StatusAssertions assertions = statusAssertions(HttpStatus.OK);

		// Success
		assertions.is2xxSuccessful();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.is5xxServerError());
	}

	@Test
	void statusSeries3xx() {
		StatusAssertions assertions = statusAssertions(HttpStatus.PERMANENT_REDIRECT);

		// Success
		assertions.is3xxRedirection();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.is2xxSuccessful());
	}

	@Test
	void statusSeries4xx() {
		StatusAssertions assertions = statusAssertions(HttpStatus.BAD_REQUEST);

		// Success
		assertions.is4xxClientError();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.is2xxSuccessful());
	}

	@Test
	void statusSeries5xx() {
		StatusAssertions assertions = statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR);

		// Success
		assertions.is5xxServerError();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.is2xxSuccessful());
	}

	@Test
	void matchesStatusValue() {
		StatusAssertions assertions = statusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.value(equalTo(409));
		assertions.value(greaterThan(400));

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.value(equalTo(200)));
	}

	@Test  // gh-26658
	void matchesCustomStatusValue() {
		statusAssertions(600).value(equalTo(600));
	}


	private StatusAssertions statusAssertions(HttpStatus status) {
		return statusAssertions(status.value());
	}

	private StatusAssertions statusAssertions(int status) {
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/"));
		MockClientHttpResponse response = new MockClientHttpResponse(status);

		ExchangeResult result = new ExchangeResult(
				request, response, Mono.empty(), Mono.empty(), Duration.ZERO, null, null);

		return new StatusAssertions(result, mock(WebTestClient.ResponseSpec.class));
	}

}
