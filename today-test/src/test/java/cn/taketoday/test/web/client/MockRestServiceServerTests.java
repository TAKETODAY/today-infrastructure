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

package cn.taketoday.test.web.client;

import org.junit.jupiter.api.Test;

import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder;
import cn.taketoday.web.client.RestTemplate;

import java.net.SocketException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static cn.taketoday.http.HttpMethod.POST;
import static cn.taketoday.test.web.client.ExpectedCount.once;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.method;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link MockRestServiceServer}.
 *
 * @author Rossen Stoyanchev
 */
public class MockRestServiceServerTests {

	private final RestTemplate restTemplate = new RestTemplate();


	@Test
	public void buildMultipleTimes() {
		MockRestServiceServerBuilder builder = MockRestServiceServer.bindTo(this.restTemplate);

		MockRestServiceServer server = builder.build();
		server.expect(requestTo("/foo")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();

		server = builder.ignoreExpectOrder(true).build();
		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();

		server = builder.build();
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		server.verify();
	}

	@Test
	public void exactExpectOrder() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate)
				.ignoreExpectOrder(false).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.restTemplate.getForObject("/bar", Void.class));
	}

	@Test
	public void ignoreExpectOrder() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate)
				.ignoreExpectOrder(true).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
	}

	@Test
	public void resetAndReuseServer() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
		server.reset();

		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		server.verify();
	}

	@Test
	public void resetAndReuseServerWithUnorderedExpectationManager() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate)
				.ignoreExpectOrder(true).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
		server.reset();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
	}

	@Test  // gh-24486
	public void resetClearsRequestFailures() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate).build();
		server.expect(once(), requestTo("/remoteurl")).andRespond(withSuccess());
		this.restTemplate.postForEntity("/remoteurl", null, String.class);
		server.verify();

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> this.restTemplate.postForEntity("/remoteurl", null, String.class))
				.withMessageStartingWith("No further requests expected");

		server.reset();

		server.expect(once(), requestTo("/remoteurl")).andRespond(withSuccess());
		this.restTemplate.postForEntity("/remoteurl", null, String.class);
		server.verify();
	}

	@Test  // SPR-16132
	public void followUpRequestAfterFailure() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate).build();

		server.expect(requestTo("/some-service/some-endpoint"))
				.andRespond(request -> { throw new SocketException("pseudo network error"); });

		server.expect(requestTo("/reporting-service/report-error"))
				.andExpect(method(POST)).andRespond(withSuccess());

		try {
			this.restTemplate.getForEntity("/some-service/some-endpoint", String.class);
			fail("Expected exception");
		}
		catch (Exception ex) {
			this.restTemplate.postForEntity("/reporting-service/report-error", ex.toString(), String.class);
		}

		server.verify();
	}

	@Test  // gh-21799
	public void verifyShouldFailIfRequestsFailed() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate).build();
		server.expect(once(), requestTo("/remoteurl")).andRespond(withSuccess());

		this.restTemplate.postForEntity("/remoteurl", null, String.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> this.restTemplate.postForEntity("/remoteurl", null, String.class))
				.withMessageStartingWith("No further requests expected");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(server::verify)
				.withMessageStartingWith("Some requests did not execute successfully");
	}

	@Test
	public void verifyWithTimeout() {
		MockRestServiceServerBuilder builder = MockRestServiceServer.bindTo(this.restTemplate);

		MockRestServiceServer server1 = builder.build();
		server1.expect(requestTo("/foo")).andRespond(withSuccess());
		server1.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);

		assertThatThrownBy(() -> server1.verify(Duration.ofMillis(100))).hasMessage(
				"Further request(s) expected leaving 1 unsatisfied expectation(s).\n" +
						"1 request(s) executed:\n" +
						"GET /foo, headers: [Accept:\"application/json, application/*+json\"]\n");

		MockRestServiceServer server2 = builder.build();
		server2.expect(requestTo("/foo")).andRespond(withSuccess());
		server2.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);
		this.restTemplate.getForObject("/bar", Void.class);

		server2.verify(Duration.ofMillis(100));
	}

}
