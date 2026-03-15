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

package infra.test.web.mock.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.http.ResponseCookie;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.web.RequestContext;
import infra.web.annotation.CookieValue;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;

/**
 * Tests that use a {@link RestTestClient} configured with a {@link MockMvc} instance
 * that uses a standalone controller.
 *
 * @author Rob Worsnop
 * @author Sam Brannen
 * @since 5.0
 */
class MockMvcRestTestClientTests {

  private final RestTestClient client;

  MockMvcRestTestClientTests() {
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController()).build();
    this.client = RestTestClient.bindTo(mockMvc).build();
  }

  @Test
  void withResult() {
    client.get()
            .uri("/foo")
            .cookie("session", "12345")
            .exchange()
            .expectCookie().valueEquals("session", "12345")
            .expectBody(String.class)
            .isEqualTo("bar");
  }

  @Test
  void withError() {
    client.get()
            .uri("/error")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().isEmpty();
  }

  @Test
  void withErrorAndBody() {
    client.get().uri("/errorbody")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(String.class)
            .isEqualTo("some really bad request");
  }

  @RestController
  static class TestController {

    @GetMapping("/foo")
    void foo(@CookieValue("session") String session, RequestContext response) throws IOException {
      response.getWriter().write("bar");
      response.addCookie(ResponseCookie.forSimple("session", session));
    }

    @GetMapping("/error")
    void handleError(RequestContext response) throws Exception {
      response.sendError(400);
    }

    @GetMapping("/errorbody")
    void handleErrorWithBody(RequestContext response) throws Exception {
      response.sendError(400);
      response.getWriter().write("some really bad request");
    }
  }

}
