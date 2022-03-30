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

package cn.taketoday.test.web.client.samples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.ClientHttpRequestExecution;
import cn.taketoday.http.client.ClientHttpRequestInterceptor;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static cn.taketoday.test.web.client.ExpectedCount.manyTimes;
import static cn.taketoday.test.web.client.ExpectedCount.never;
import static cn.taketoday.test.web.client.ExpectedCount.once;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.method;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Examples to demonstrate writing client-side REST tests with Spring MVC Test.
 * While the tests in this class invoke the RestTemplate directly, in actual
 * tests the RestTemplate may likely be invoked indirectly, i.e. through client
 * code.
 *
 * @author Rossen Stoyanchev
 */
public class SampleTests {

	private MockRestServiceServer mockServer;

	private RestTemplate restTemplate;

	@BeforeEach
	public void setup() {
		this.restTemplate = new RestTemplate();
		this.mockServer = MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true).build();
	}

	@Test
	public void performGet() {

		String responseBody = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

		this.mockServer.expect(requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		@SuppressWarnings("unused")
    Person ludwig = this.restTemplate.getForObject("/composers/{id}", Person.class, 42);

		// We are only validating the request. The response is mocked out.
		// hotel.getId() == 42
		// hotel.getName().equals("Holiday Inn")

		this.mockServer.verify();
	}

	@Test
	public void performGetManyTimes() {

		String responseBody = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

		this.mockServer.expect(manyTimes(), requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		@SuppressWarnings("unused")
		Person ludwig = this.restTemplate.getForObject("/composers/{id}", Person.class, 42);

		// We are only validating the request. The response is mocked out.
		// hotel.getId() == 42
		// hotel.getName().equals("Holiday Inn")

		this.restTemplate.getForObject("/composers/{id}", Person.class, 42);
		this.restTemplate.getForObject("/composers/{id}", Person.class, 42);
		this.restTemplate.getForObject("/composers/{id}", Person.class, 42);

		this.mockServer.verify();
	}

	@Test
	public void expectNever() {

		String responseBody = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

		this.mockServer.expect(once(), requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
		this.mockServer.expect(never(), requestTo("/composers/43")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		this.restTemplate.getForObject("/composers/{id}", Person.class, 42);

		this.mockServer.verify();
	}

	@Test
	public void expectNeverViolated() {

		String responseBody = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

		this.mockServer.expect(once(), requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
		this.mockServer.expect(never(), requestTo("/composers/43")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		this.restTemplate.getForObject("/composers/{id}", Person.class, 42);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.restTemplate.getForObject("/composers/{id}", Person.class, 43));
	}

	@Test
	public void performGetWithResponseBodyFromFile() {

		Resource responseBody = new ClassPathResource("ludwig.json", this.getClass());

		this.mockServer.expect(requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		@SuppressWarnings("unused")
		Person ludwig = this.restTemplate.getForObject("/composers/{id}", Person.class, 42);

		// hotel.getId() == 42
		// hotel.getName().equals("Holiday Inn")

		this.mockServer.verify();
	}

	@Test
	public void verify() {

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("2", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("4", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("8", MediaType.TEXT_PLAIN));

		@SuppressWarnings("unused")
		String result1 = this.restTemplate.getForObject("/number", String.class);
		// result1 == "1"

		@SuppressWarnings("unused")
		String result2 = this.restTemplate.getForObject("/number", String.class);
		// result == "2"

		try {
			this.mockServer.verify();
		}
		catch (AssertionError error) {
			assertThat(error.getMessage().contains("2 unsatisfied expectation(s)")).as(error.getMessage()).isTrue();
		}
	}

	@Test // SPR-14694
	public void repeatedAccessToResponseViaResource() {

		Resource resource = new ClassPathResource("ludwig.json", this.getClass());

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setInterceptors(Collections.singletonList(new ContentInterceptor(resource)));

		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate)
				.ignoreExpectOrder(true)
				.bufferContent()  // enable repeated reads of response body
				.build();

		mockServer.expect(requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(resource, MediaType.APPLICATION_JSON));

		restTemplate.getForObject("/composers/{id}", Person.class, 42);

		mockServer.verify();
	}


	private static class ContentInterceptor implements ClientHttpRequestInterceptor {

		private final Resource resource;


		private ContentInterceptor(Resource resource) {
			this.resource = resource;
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {

			ClientHttpResponse response = execution.execute(request, body);
			byte[] expected = FileCopyUtils.copyToByteArray(this.resource.getInputStream());
			byte[] actual = FileCopyUtils.copyToByteArray(response.getBody());
			assertThat(new String(actual)).isEqualTo(new String(expected));
			return response;
		}
	}

}
