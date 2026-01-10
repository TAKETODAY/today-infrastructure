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

package infra.test.web.mock.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.test.web.Person;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.WebTestClient;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;

import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.resultmatchers.JsonPathAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class JsonPathAssertionTests {

  private final WebTestClient client =
          MockMvcWebTestClient.bindToController(new MusicController())
                  .alwaysExpect(status().isOk())
                  .alwaysExpect(content().contentType(MediaType.APPLICATION_JSON))
                  .configureClient()
                  .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                  .build();

  @Test
  public void exists() {
    String composerByName = "$.composers[?(@.name == '%s')]";
    String performerByName = "$.performers[?(@.name == '%s')]";

    client.get().uri("/music/people")
            .exchange()
            .expectBody()
            .jsonPath(composerByName.formatted("Johann Sebastian Bach")).exists()
            .jsonPath(composerByName.formatted("Johannes Brahms")).exists()
            .jsonPath(composerByName.formatted("Edvard Grieg")).exists()
            .jsonPath(composerByName.formatted("Robert Schumann")).exists()
            .jsonPath(performerByName.formatted("Vladimir Ashkenazy")).exists()
            .jsonPath(performerByName.formatted("Yehudi Menuhin")).exists()
            .jsonPath("$.composers[0]").exists()
            .jsonPath("$.composers[1]").exists()
            .jsonPath("$.composers[2]").exists()
            .jsonPath("$.composers[3]").exists();
  }

  @Test
  public void doesNotExist() {
    client.get().uri("/music/people")
            .exchange()
            .expectBody()
            .jsonPath("$.composers[?(@.name == 'Edvard Grieeeeeeg')]").doesNotExist()
            .jsonPath("$.composers[?(@.name == 'Robert Schuuuuuuman')]").doesNotExist()
            .jsonPath("$.composers[4]").doesNotExist();
  }

  @Test
  public void equality() {
    client.get().uri("/music/people")
            .exchange()
            .expectBody()
            .jsonPath("$.composers[0].name").isEqualTo("Johann Sebastian Bach")
            .jsonPath("$.performers[1].name").isEqualTo("Yehudi Menuhin");

    // Hamcrest matchers...
    client.get().uri("/music/people")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.composers[0].name").value(equalTo("Johann Sebastian Bach"))
            .jsonPath("$.performers[1].name").value(equalTo("Yehudi Menuhin"));
  }

  @Test
  public void hamcrestMatcher() {
    client.get().uri("/music/people")
            .exchange()
            .expectBody()
            .jsonPath("$.composers[0].name").value(startsWith("Johann"))
            .jsonPath("$.performers[0].name").value(endsWith("Ashkenazy"))
            .jsonPath("$.performers[1].name").value(containsString("di Me"))
            .jsonPath("$.composers[1].name").value(is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms"))));
  }

  @Test
  public void hamcrestMatcherWithParameterizedJsonPath() {
    client.get().uri("/music/people")
            .exchange()
            .expectBody()
            .jsonPath("$.composers[0].name").value(startsWith("Johann"))
            .jsonPath("$.performers[0].name").value(endsWith("Ashkenazy"))
            .jsonPath("$.performers[1].name").value(containsString("di Me"))
            .jsonPath("$.composers[1].name").value(is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms"))));
  }

  @RestController
  private static class MusicController {

    @RequestMapping("/music/people")
    public MultiValueMap<String, Person> get() {
      MultiValueMap<String, Person> map = new LinkedMultiValueMap<>();

      map.add("composers", new Person("Johann Sebastian Bach"));
      map.add("composers", new Person("Johannes Brahms"));
      map.add("composers", new Person("Edvard Grieg"));
      map.add("composers", new Person("Robert Schumann"));

      map.add("performers", new Person("Vladimir Ashkenazy"));
      map.add("performers", new Person("Yehudi Menuhin"));

      return map;
    }
  }

}
