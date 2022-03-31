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

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.containsString;

/**
 * Samples of tests using {@link WebTestClient} with serialized JSON content.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
class JsonContentTests {

	private final WebTestClient client = WebTestClient.bindToController(new PersonController()).build();


	@Test
	void jsonContentWithDefaultLenientMode() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json("""
						[
							{"firstName":"Jane"},
							{"firstName":"Jason"},
							{"firstName":"John"}
						]
						""");
	}

	@Test
	void jsonContentWithStrictMode() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json("""
						[
							{"firstName":"Jane", "lastName":"Williams"},
							{"firstName":"Jason","lastName":"Johnson"},
							{"firstName":"John", "lastName":"Smith"}
						]
						""",
						true);
	}

	@Test
	void jsonContentWithStrictModeAndMissingAttributes() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectBody().json("""
						[
							{"firstName":"Jane"},
							{"firstName":"Jason"},
							{"firstName":"John"}
						]
						""",
						true)
		);
	}

	@Test
	void jsonPathIsEqualTo() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$[0].firstName").isEqualTo("Jane")
				.jsonPath("$[1].firstName").isEqualTo("Jason")
				.jsonPath("$[2].firstName").isEqualTo("John");
	}

	@Test
	void jsonPathMatches() {
		this.client.get().uri("/persons/John/Smith")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.firstName").value(containsString("oh"));
	}

	@Test
	void postJsonContent() {
		this.client.post().uri("/persons")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{"firstName":"John", "lastName":"Smith"}
						""")
				.exchange()
				.expectStatus().isCreated()
				.expectBody().isEmpty();
	}


	@RestController
	@RequestMapping("/persons")
	static class PersonController {

		@GetMapping
		Flux<Person> getPersons() {
			return Flux.just(new Person("Jane", "Williams"), new Person("Jason", "Johnson"), new Person("John", "Smith"));
		}

		@GetMapping("/{firstName}/{lastName}")
		Person getPerson(@PathVariable String firstName, @PathVariable String lastName) {
			return new Person(firstName, lastName);
		}

		@PostMapping
		ResponseEntity<String> savePerson(@RequestBody Person person) {
			return ResponseEntity.created(URI.create(String.format("/persons/%s/%s", person.getFirstName(), person.getLastName()))).build();
		}
	}

	static class Person {
		private String firstName;
		private String lastName;

		public Person() {
		}

		public Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String getFirstName() {
			return this.firstName;
		}

		public String getLastName() {
			return this.lastName;
		}
	}

}
