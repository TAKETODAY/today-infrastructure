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

import infra.http.MediaType;
import infra.stereotype.Controller;
import infra.test.web.Person;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.ResponseBody;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.RequestParameterTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestParameterTests {

  @Test
  public void queryParameter() {

    WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController()).build();

    client.get().uri("/search?name=George")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().jsonPath("$.name", "George");
  }

  @Controller
  private class PersonController {

    @RequestMapping(value = "/search")
    @ResponseBody
    public Person get(@RequestParam String name) {
      Person person = new Person(name);
      return person;
    }
  }

}
