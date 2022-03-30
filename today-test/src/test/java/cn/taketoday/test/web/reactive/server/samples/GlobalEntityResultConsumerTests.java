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
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests with a globally registered
 * {@link cn.taketoday.test.web.reactive.server.EntityExchangeResult} consumer.
 *
 * @author Rossen Stoyanchev
 */
public class GlobalEntityResultConsumerTests {

	private final StringBuilder output = new StringBuilder();

	private final WebTestClient client = WebTestClient.bindToController(TestController.class)
			.configureClient()
			.entityExchangeResultConsumer(result -> {
				byte[] bytes = result.getResponseBodyContent();
				this.output.append(new String(bytes, StandardCharsets.UTF_8));
			})
			.build();


	@Test
	void json() {
		this.client.get().uri("/person/1")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json("{\"name\":\"Joe\"}");

		assertThat(this.output.toString()).isEqualTo("{\"name\":\"Joe\"}");
	}

	@Test
	void entity() {
		this.client.get().uri("/person/1")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Person.class).isEqualTo(new Person("Joe"));

		assertThat(this.output.toString()).isEqualTo("{\"name\":\"Joe\"}");
	}

	@Test
	void entityList() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(Person.class).hasSize(2);

		assertThat(this.output.toString())
				.isEqualTo("[{\"name\":\"Joe\"},{\"name\":\"Joseph\"}]");
	}


	@RestController
	static class TestController {

		@GetMapping("/person/{id}")
		Person getPerson() {
			return new Person("Joe");
		}

		@GetMapping("/persons")
		List<Person> getPersons() {
			return Arrays.asList(new Person("Joe"), new Person("Joseph"));
		}
	}

}
