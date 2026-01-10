/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.web.mock.samples.client.standalone;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import infra.core.ParameterizedTypeReference;
import infra.http.MediaType;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RestController;
import jakarta.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * MockMvcTestClient equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.ResponseBodyTests}.
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
