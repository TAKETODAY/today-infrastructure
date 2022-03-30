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

package cn.taketoday.test.web.reactive.server.samples;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.PathVariable;
import cn.taketoday.web.bind.annotation.PostMapping;
import cn.taketoday.web.bind.annotation.RequestBody;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import static org.hamcrest.Matchers.startsWith;

/**
 * Samples of tests using {@link WebTestClient} with XML content.
 *
 * @author Eric Deandrea
 * @since 5.1
 */
class XmlContentTests {

	private static final String persons_XML =
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
			+ "<persons>"
			+ "<person><name>Jane</name></person>"
			+ "<person><name>Jason</name></person>"
			+ "<person><name>John</name></person>"
			+ "</persons>";


	private final WebTestClient client = WebTestClient.bindToController(new PersonController()).build();


	@Test
	void xmlContent() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectBody().xml(persons_XML);
	}

	@Test
	void xpathIsEqualTo() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.xpath("/").exists()
				.xpath("/persons").exists()
				.xpath("/persons/person").exists()
				.xpath("/persons/person").nodeCount(3)
				.xpath("/persons/person[1]/name").isEqualTo("Jane")
				.xpath("/persons/person[2]/name").isEqualTo("Jason")
				.xpath("/persons/person[3]/name").isEqualTo("John");
	}

	@Test
	void xpathMatches() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.xpath("//person/name").string(startsWith("J"));
	}

	@Test
	void xpathContainsSubstringViaRegex() {
		this.client.get().uri("/persons/John")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.xpath("//name[contains(text(), 'oh')]").exists();
	}

	@Test
	void postXmlContent() {
		String content =
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
				"<person><name>John</name></person>";

		this.client.post().uri("/persons")
				.contentType(MediaType.APPLICATION_XML)
				.bodyValue(content)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().valueEquals(HttpHeaders.LOCATION, "/persons/John")
				.expectBody().isEmpty();
	}


	@SuppressWarnings("unused")
	@XmlRootElement(name="persons")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class PersonsWrapper {

		@XmlElement(name="person")
		private final List<Person> persons = new ArrayList<>();

		public PersonsWrapper() {
		}

		public PersonsWrapper(List<Person> persons) {
			this.persons.addAll(persons);
		}

		public PersonsWrapper(Person... persons) {
			this.persons.addAll(Arrays.asList(persons));
		}

		public List<Person> getpersons() {
			return this.persons;
		}
	}

	@RestController
	@RequestMapping("/persons")
	static class PersonController {

		@GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
		PersonsWrapper getPersons() {
			return new PersonsWrapper(new Person("Jane"), new Person("Jason"), new Person("John"));
		}

		@GetMapping(path = "/{name}", produces = MediaType.APPLICATION_XML_VALUE)
		Person getPerson(@PathVariable String name) {
			return new Person(name);
		}

		@PostMapping(consumes = MediaType.APPLICATION_XML_VALUE)
		ResponseEntity<Object> savepersons(@RequestBody Person person) {
			URI location = URI.create(String.format("/persons/%s", person.getName()));
			return ResponseEntity.created(location).build();
		}
	}

}
