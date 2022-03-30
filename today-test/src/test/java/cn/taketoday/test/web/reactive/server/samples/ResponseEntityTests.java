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
import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.test.web.reactive.server.FluxExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.PathVariable;
import cn.taketoday.web.bind.annotation.PostMapping;
import cn.taketoday.web.bind.annotation.RequestBody;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static java.time.Duration.ofMillis;
import static org.hamcrest.Matchers.startsWith;
import static cn.taketoday.http.MediaType.TEXT_EVENT_STREAM;

/**
 * Annotated controllers accepting and returning typed Objects.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ResponseEntityTests {

	private final WebTestClient client = WebTestClient.bindToController(new PersonController())
			.configureClient()
			.baseUrl("/persons")
			.build();


	@Test
	void entity() {
		this.client.get().uri("/John")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(Person.class).isEqualTo(new Person("John"));
	}

	@Test
	void entityMatcher() {
		this.client.get().uri("/John")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(Person.class).value(Person::getName, startsWith("Joh"));
	}

	@Test
	void entityWithConsumer() {
		this.client.get().uri("/John")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(Person.class)
				.consumeWith(result -> assertThat(result.getResponseBody()).isEqualTo(new Person("John")));
	}

	@Test
	void entityList() {
		List<Person> expected = Arrays.asList(
				new Person("Jane"), new Person("Jason"), new Person("John"));

		this.client.get()
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBodyList(Person.class).isEqualTo(expected);
	}

	@Test
	void entityListWithConsumer() {
		this.client.get()
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBodyList(Person.class).value(people ->
					assertThat(people).contains(new Person("Jason"))
				);
	}

	@Test
	void entityMap() {
		Map<String, Person> map = new LinkedHashMap<>();
		map.put("Jane", new Person("Jane"));
		map.put("Jason", new Person("Jason"));
		map.put("John", new Person("John"));

		this.client.get().uri("?map=true")
				.exchange()
				.expectStatus().isOk()
				.expectBody(new ParameterizedTypeReference<Map<String, Person>>() {}).isEqualTo(map);
	}

	@Test
	void entityStream() {
		FluxExchangeResult<Person> result = this.client.get()
				.accept(TEXT_EVENT_STREAM)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
				.returnResult(Person.class);

		StepVerifier.create(result.getResponseBody())
				.expectNext(new Person("N0"), new Person("N1"), new Person("N2"))
				.expectNextCount(4)
				.consumeNextWith(person -> assertThat(person.getName()).endsWith("7"))
				.thenCancel()
				.verify();
	}

	@Test
	void postEntity() {
		this.client.post()
				.bodyValue(new Person("John"))
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().valueEquals("location", "/persons/John")
				.expectBody().isEmpty();
	}


	@RestController
	@RequestMapping("/persons")
	static class PersonController {

		@GetMapping("/{name}")
		Person getPerson(@PathVariable String name) {
			return new Person(name);
		}

		@GetMapping
		Flux<Person> getPersons() {
			return Flux.just(new Person("Jane"), new Person("Jason"), new Person("John"));
		}

		@GetMapping(params = "map")
		Map<String, Person> getPersonsAsMap() {
			Map<String, Person> map = new LinkedHashMap<>();
			map.put("Jane", new Person("Jane"));
			map.put("Jason", new Person("Jason"));
			map.put("John", new Person("John"));
			return map;
		}

		@GetMapping(produces = "text/event-stream")
		Flux<Person> getPersonStream() {
			return Flux.interval(ofMillis(100)).take(50).onBackpressureBuffer(50)
					.map(index -> new Person("N" + index));
		}

		@PostMapping
		ResponseEntity<String> savePerson(@RequestBody Person person) {
			return ResponseEntity.created(URI.create("/persons/" + person.getName())).build();
		}
	}

}
