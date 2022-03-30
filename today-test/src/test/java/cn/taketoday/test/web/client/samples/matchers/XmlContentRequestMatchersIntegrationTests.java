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
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import static org.hamcrest.Matchers.hasXPath;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.content;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Examples of defining expectations on XML request content with XMLUnit.
 *
 * @author Rossen Stoyanchev
 * @see ContentRequestMatchersIntegrationTests
 * @see XpathRequestMatchersIntegrationTests
 */
public class XmlContentRequestMatchersIntegrationTests {

	private static final String PEOPLE_XML =
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
			"<people><composers>" +
			"<composer><name>Johann Sebastian Bach</name><someBoolean>false</someBoolean><someDouble>21.0</someDouble></composer>" +
			"<composer><name>Johannes Brahms</name><someBoolean>false</someBoolean><someDouble>0.0025</someDouble></composer>" +
			"<composer><name>Edvard Grieg</name><someBoolean>false</someBoolean><someDouble>1.6035</someDouble></composer>" +
			"<composer><name>Robert Schumann</name><someBoolean>false</someBoolean><someDouble>NaN</someDouble></composer>" +
			"</composers></people>";


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

		this.people = new PeopleWrapper(composers);

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new Jaxb2RootElementHttpMessageConverter());

		this.restTemplate = new RestTemplate();
		this.restTemplate.setMessageConverters(converters);

		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
	}

	@Test
	public void testXmlEqualTo() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(content().xml(PEOPLE_XML))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	@Test
	public void testHamcrestNodeMatcher() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/xml"))
			.andExpect(content().node(hasXPath("/people/composers/composer[1]")))
			.andRespond(withSuccess());

		executeAndVerify();
	}

	private void executeAndVerify() throws URISyntaxException {
		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}


	@SuppressWarnings("unused")
	@XmlRootElement(name="people")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class PeopleWrapper {

		@XmlElementWrapper(name="composers")
		@XmlElement(name="composer")
		private List<Person> composers;

		public PeopleWrapper() {
		}

		public PeopleWrapper(List<Person> composers) {
			this.composers = composers;
		}

		public List<Person> getComposers() {
			return this.composers;
		}
	}

}
