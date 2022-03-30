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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.content;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.xpath;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Examples of defining expectations on XML request content with XPath expressions.
 *
 * @author Rossen Stoyanchev
 * @see ContentRequestMatchersIntegrationTests
 * @see XmlContentRequestMatchersIntegrationTests
 */
public class XpathRequestMatchersIntegrationTests {

	private static final Map<String, String> NS =
			Collections.singletonMap("ns", "https://example.org/music/people");


	private MockRestServiceServer mockServer;

	private RestTemplate restTemplate;

	private PeopleWrapper people;


	@BeforeEach
	public void setup() {
		List<Person> composers = Arrays.asList(
				new Person("Johann Sebastian Bach").setSomeDouble(21),
				new Person("Johannes Brahms").setSomeDouble(.0025),
				new Person("Edvard Grieg").setSomeDouble(1.6035),
				new Person("Robert Schumann").setSomeDouble(Double.NaN));

		List<Person> performers = Arrays.asList(
				new Person("Vladimir Ashkenazy").setSomeBoolean(false),
				new Person("Yehudi Menuhin").setSomeBoolean(true));

		this.people = new PeopleWrapper(composers, performers);

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new Jaxb2RootElementHttpMessageConverter());

		this.restTemplate = new RestTemplate();
		this.restTemplate.setMessageConverters(converters);

		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
	}


	@Test
	public void testExists() throws Exception {
		String composer = "/ns:people/composers/composer[%s]";
		String performer = "/ns:people/performers/performer[%s]";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(xpath(composer, NS, 1).exists())
			.andExpect(xpath(composer, NS, 2).exists())
			.andExpect(xpath(composer, NS, 3).exists())
			.andExpect(xpath(composer, NS, 4).exists())
			.andExpect(xpath(performer, NS, 1).exists())
			.andExpect(xpath(performer, NS, 2).exists())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void testDoesNotExist() throws Exception {
		String composer = "/ns:people/composers/composer[%s]";
		String performer = "/ns:people/performers/performer[%s]";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(xpath(composer, NS, 0).doesNotExist())
			.andExpect(xpath(composer, NS, 5).doesNotExist())
			.andExpect(xpath(performer, NS, 0).doesNotExist())
			.andExpect(xpath(performer, NS, 3).doesNotExist())
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void testString() throws Exception {
		String composerName = "/ns:people/composers/composer[%s]/name";
		String performerName = "/ns:people/performers/performer[%s]/name";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(xpath(composerName, NS, 1).string("Johann Sebastian Bach"))
			.andExpect(xpath(composerName, NS, 2).string("Johannes Brahms"))
			.andExpect(xpath(composerName, NS, 3).string("Edvard Grieg"))
			.andExpect(xpath(composerName, NS, 4).string("Robert Schumann"))
			.andExpect(xpath(performerName, NS, 1).string("Vladimir Ashkenazy"))
			.andExpect(xpath(performerName, NS, 2).string("Yehudi Menuhin"))
			.andExpect(xpath(composerName, NS, 1).string(equalTo("Johann Sebastian Bach"))) // Hamcrest..
			.andExpect(xpath(composerName, NS, 1).string(startsWith("Johann"))) // Hamcrest..
			.andExpect(xpath(composerName, NS, 1).string(notNullValue())) // Hamcrest..
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void testNumber() throws Exception {
		String composerDouble = "/ns:people/composers/composer[%s]/someDouble";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(xpath(composerDouble, NS, 1).number(21d))
			.andExpect(xpath(composerDouble, NS, 2).number(.0025))
			.andExpect(xpath(composerDouble, NS, 3).number(1.6035))
			.andExpect(xpath(composerDouble, NS, 4).number(Double.NaN))
			.andExpect(xpath(composerDouble, NS, 1).number(equalTo(21d))) // Hamcrest..
			.andExpect(xpath(composerDouble, NS, 3).number(closeTo(1.6, .01))) // Hamcrest..
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void testBoolean() throws Exception {

		String performerBooleanValue = "/ns:people/performers/performer[%s]/someBoolean";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(xpath(performerBooleanValue, NS, 1).booleanValue(false))
			.andExpect(xpath(performerBooleanValue, NS, 2).booleanValue(true))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void testNodeCount() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(xpath("/ns:people/composers/composer", NS).nodeCount(4))
			.andExpect(xpath("/ns:people/performers/performer", NS).nodeCount(2))
			.andExpect(xpath("/ns:people/composers/composer", NS).nodeCount(equalTo(4))) // Hamcrest..
			.andExpect(xpath("/ns:people/performers/performer", NS).nodeCount(equalTo(2))) // Hamcrest..
			.andRespond(withSuccess());

		executeAndVerify();
	}

	private void executeAndVerify() throws URISyntaxException {
		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}


	@SuppressWarnings("unused")
	@XmlRootElement(name="people", namespace="https://example.org/music/people")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class PeopleWrapper {

		@XmlElementWrapper(name="composers")
		@XmlElement(name="composer")
		private List<Person> composers;

		@XmlElementWrapper(name="performers")
		@XmlElement(name="performer")
		private List<Person> performers;

		public PeopleWrapper() {
		}

		public PeopleWrapper(List<Person> composers, List<Person> performers) {
			this.composers = composers;
			this.performers = performers;
		}

		public List<Person> getComposers() {
			return this.composers;
		}

		public List<Person> getPerformers() {
			return this.performers;
		}
	}

}
