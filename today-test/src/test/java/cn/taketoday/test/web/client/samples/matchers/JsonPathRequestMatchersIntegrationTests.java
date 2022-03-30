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

package cn.taketoday.test.web.client.samples.matchers;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.client.match.JsonPathRequestMatchersTests;
import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.content;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Examples of defining expectations on JSON request content with
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expressions.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see cn.taketoday.test.web.client.match.JsonPathRequestMatchers
 * @see JsonPathRequestMatchersTests
 */
public class JsonPathRequestMatchersIntegrationTests {

	private static final MultiValueMap<String, Person> people = new LinkedMultiValueMap<>();

	static {
		people.add("composers", new Person("Johann Sebastian Bach"));
		people.add("composers", new Person("Johannes Brahms"));
		people.add("composers", new Person("Edvard Grieg"));
		people.add("composers", new Person("Robert Schumann"));
		people.add("performers", new Person("Vladimir Ashkenazy"));
		people.add("performers", new Person("Yehudi Menuhin"));
	}


	private final RestTemplate restTemplate =
			new RestTemplate(Collections.singletonList(new MappingJackson2HttpMessageConverter()));

	private final MockRestServiceServer mockServer = MockRestServiceServer.createServer(this.restTemplate);


	@Test
	public void exists() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0]").exists())
			.andExpect(jsonPath("$.composers[1]").exists())
			.andExpect(jsonPath("$.composers[2]").exists())
			.andExpect(jsonPath("$.composers[3]").exists())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void doesNotExist() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[?(@.name == 'Edvard Grieeeeeeg')]").doesNotExist())
			.andExpect(jsonPath("$.composers[?(@.name == 'Robert Schuuuuuuman')]").doesNotExist())
			.andExpect(jsonPath("$.composers[4]").doesNotExist())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void value() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0].name").value("Johann Sebastian Bach"))
			.andExpect(jsonPath("$.performers[1].name").value("Yehudi Menuhin"))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void hamcrestMatchers() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0].name").value(equalTo("Johann Sebastian Bach")))
			.andExpect(jsonPath("$.performers[1].name").value(equalTo("Yehudi Menuhin")))
			.andExpect(jsonPath("$.composers[0].name", startsWith("Johann")))
			.andExpect(jsonPath("$.performers[0].name", endsWith("Ashkenazy")))
			.andExpect(jsonPath("$.performers[1].name", containsString("di Me")))
			.andExpect(jsonPath("$.composers[1].name", is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms")))))
			.andExpect(jsonPath("$.composers[:3].name", hasItem("Johannes Brahms")))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void hamcrestMatchersWithParameterizedJsonPaths() throws Exception {
		String composerName = "$.composers[%s].name";
		String performerName = "$.performers[%s].name";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath(composerName, 0).value(startsWith("Johann")))
			.andExpect(jsonPath(performerName, 0).value(endsWith("Ashkenazy")))
			.andExpect(jsonPath(performerName, 1).value(containsString("di Me")))
			.andExpect(jsonPath(composerName, 1).value(is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms")))))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void isArray() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers").isArray())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void isString() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0].name").isString())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void isNumber() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0].someDouble").isNumber())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void isBoolean() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.composers[0].someBoolean").isBoolean())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	private void executeAndVerify() throws URISyntaxException {
		this.restTemplate.put(new URI("/composers"), people);
		this.mockServer.verify();
	}

}
