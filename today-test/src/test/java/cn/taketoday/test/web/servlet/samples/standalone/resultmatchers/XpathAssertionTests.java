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

package cn.taketoday.test.web.servlet.samples.standalone.resultmatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.ResponseBody;

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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static cn.taketoday.web.bind.annotation.RequestMethod.GET;
import static cn.taketoday.web.bind.annotation.RequestMethod.HEAD;

/**
 * Examples of expectations on XML response content with XPath expressions.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see ContentAssertionTests
 * @see XmlContentAssertionTests
 */
public class XpathAssertionTests {

	private static final Map<String, String> musicNamespace =
		Collections.singletonMap("ns", "https://example.org/music/people");

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() throws Exception {
		this.mockMvc = standaloneSetup(new MusicController())
				.defaultRequest(get("/").accept(MediaType.APPLICATION_XML, MediaType.parseMediaType("application/xml;charset=UTF-8")))
				.alwaysExpect(status().isOk())
				.alwaysExpect(content().contentType(MediaType.parseMediaType("application/xml;charset=UTF-8")))
				.build();
	}

	@Test
	public void testExists() throws Exception {

		String composer = "/ns:people/composers/composer[%s]";
		String performer = "/ns:people/performers/performer[%s]";

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath(composer, musicNamespace, 1).exists())
			.andExpect(xpath(composer, musicNamespace, 2).exists())
			.andExpect(xpath(composer, musicNamespace, 3).exists())
			.andExpect(xpath(composer, musicNamespace, 4).exists())
			.andExpect(xpath(performer, musicNamespace, 1).exists())
			.andExpect(xpath(performer, musicNamespace, 2).exists())
			.andExpect(xpath(composer, musicNamespace, 1).node(notNullValue()));
	}

	@Test
	public void testDoesNotExist() throws Exception {

		String composer = "/ns:people/composers/composer[%s]";
		String performer = "/ns:people/performers/performer[%s]";

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath(composer, musicNamespace, 0).doesNotExist())
			.andExpect(xpath(composer, musicNamespace, 5).doesNotExist())
			.andExpect(xpath(performer, musicNamespace, 0).doesNotExist())
			.andExpect(xpath(performer, musicNamespace, 3).doesNotExist())
			.andExpect(xpath(composer, musicNamespace, 0).node(nullValue()));
	}

	@Test
	public void testString() throws Exception {

		String composerName = "/ns:people/composers/composer[%s]/name";
		String performerName = "/ns:people/performers/performer[%s]/name";

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath(composerName, musicNamespace, 1).string("Johann Sebastian Bach"))
			.andExpect(xpath(composerName, musicNamespace, 2).string("Johannes Brahms"))
			.andExpect(xpath(composerName, musicNamespace, 3).string("Edvard Grieg"))
			.andExpect(xpath(composerName, musicNamespace, 4).string("Robert Schumann"))
			.andExpect(xpath(performerName, musicNamespace, 1).string("Vladimir Ashkenazy"))
			.andExpect(xpath(performerName, musicNamespace, 2).string("Yehudi Menuhin"))
			.andExpect(xpath(composerName, musicNamespace, 1).string(equalTo("Johann Sebastian Bach"))) // Hamcrest..
			.andExpect(xpath(composerName, musicNamespace, 1).string(startsWith("Johann")))
			.andExpect(xpath(composerName, musicNamespace, 1).string(notNullValue()));
	}

	@Test
	public void testNumber() throws Exception {

		String composerDouble = "/ns:people/composers/composer[%s]/someDouble";

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath(composerDouble, musicNamespace, 1).number(21d))
			.andExpect(xpath(composerDouble, musicNamespace, 2).number(.0025))
			.andExpect(xpath(composerDouble, musicNamespace, 3).number(1.6035))
			.andExpect(xpath(composerDouble, musicNamespace, 4).number(Double.NaN))
			.andExpect(xpath(composerDouble, musicNamespace, 1).number(equalTo(21d)))  // Hamcrest..
			.andExpect(xpath(composerDouble, musicNamespace, 3).number(closeTo(1.6, .01)));
	}

	@Test
	public void testBoolean() throws Exception {

		String performerBooleanValue = "/ns:people/performers/performer[%s]/someBoolean";

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath(performerBooleanValue, musicNamespace, 1).booleanValue(false))
			.andExpect(xpath(performerBooleanValue, musicNamespace, 2).booleanValue(true));
	}

	@Test
	public void testNodeCount() throws Exception {

		this.mockMvc.perform(get("/music/people"))
			.andExpect(xpath("/ns:people/composers/composer", musicNamespace).nodeCount(4))
			.andExpect(xpath("/ns:people/performers/performer", musicNamespace).nodeCount(2))
			.andExpect(xpath("/ns:people/composers/composer", musicNamespace).nodeCount(equalTo(4))) // Hamcrest..
			.andExpect(xpath("/ns:people/performers/performer", musicNamespace).nodeCount(equalTo(2)));
	}

	// SPR-10704

	@Test
	public void testFeedWithLinefeedChars() throws Exception {

//		Map<String, String> namespace = Collections.singletonMap("ns", "");

		standaloneSetup(new BlogFeedController()).build()
			.perform(get("/blog.atom").accept(MediaType.APPLICATION_ATOM_XML))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML))
				.andExpect(xpath("//feed/title").string("Test Feed"))
				.andExpect(xpath("//feed/icon").string("https://www.example.com/favicon.ico"));
	}


	@Controller
	private static class MusicController {

		@RequestMapping(value="/music/people")
		public @ResponseBody PeopleWrapper getPeople() {

			List<Person> composers = Arrays.asList(
					new Person("Johann Sebastian Bach").setSomeDouble(21),
					new Person("Johannes Brahms").setSomeDouble(.0025),
					new Person("Edvard Grieg").setSomeDouble(1.6035),
					new Person("Robert Schumann").setSomeDouble(Double.NaN));

			List<Person> performers = Arrays.asList(
					new Person("Vladimir Ashkenazy").setSomeBoolean(false),
					new Person("Yehudi Menuhin").setSomeBoolean(true));

			return new PeopleWrapper(composers, performers);
		}
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


	@Controller
	public class BlogFeedController {

		@RequestMapping(value="/blog.atom", method = { GET, HEAD })
		@ResponseBody
		public String listPublishedPosts() {
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
					+ "<feed xmlns=\"http://www.w3.org/2005/Atom\">\r\n"
					+ "  <title>Test Feed</title>\r\n"
					+ "  <icon>https://www.example.com/favicon.ico</icon>\r\n"
					+ "</feed>\r\n\r\n";
		}
	}

}
