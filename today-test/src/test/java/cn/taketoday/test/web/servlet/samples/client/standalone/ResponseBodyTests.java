/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web.servlet.samples.client.standalone;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RestController;
import jakarta.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * MockMvcTestClient equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.ResponseBodyTests}.
 *
 * @author Rossen Stoyanchev
 */
class ResponseBodyTests {

  @Test
  void json() {
    execute("/persons/Lee", body -> body.jsonPath("$.name").isEqualTo("Lee")
            .jsonPath("$.age").isEqualTo(42)
            .jsonPath("$.age").value(equalTo(42))
            .jsonPath("$.age").value(Float.class, equalTo(42.0f)));
  }

  @Test
  void jsonPathWithCustomType() {
    execute("/persons/Lee", body -> body.jsonPath("$").isEqualTo(new Person("Lee", 42)));
  }

  @Test
  void jsonPathWithResolvedValue() {
    execute("/persons/Lee", body -> body.jsonPath("$").value(Person.class,
            candidate -> assertThat(candidate).isEqualTo(new Person("Lee", 42))));
  }

  @Test
  void jsonPathWithResolvedGenericValue() {
    execute("/persons", body -> body.jsonPath("$").value(new ParameterizedTypeReference<List<Person>>() { },
            candidate -> assertThat(candidate).hasSize(3).extracting(Person::name)
                    .containsExactly("Rossen", "Juergen", "Arjen")));
  }

  private void execute(String uri, Consumer<WebTestClient.BodyContentSpec> assertions) {
    assertions.accept(MockMvcWebTestClient.bindToController(new PersonController()).build()
            .get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody());
  }

  @RestController
  @SuppressWarnings("unused")
  private static class PersonController {

    @GetMapping("/persons")
    List<Person> getAll() {
      return List.of(new Person("Rossen", 42), new Person("Juergen", 42),
              new Person("Arjen", 42));
    }

    @GetMapping("/persons/{name}")
    Person get(@PathVariable String name) {
      return new Person(name, 42);
    }
  }

  private record Person(@NotNull String name, int age) { }

}
