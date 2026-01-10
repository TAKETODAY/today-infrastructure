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

import infra.stereotype.Controller;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.EntityExchangeResult;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.RequestMapping;

import static infra.test.web.mock.result.MockMvcResultMatchers.forwardedUrlPattern;
import static infra.test.web.mock.result.MockMvcResultMatchers.redirectedUrlPattern;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.resultmatchers.UrlAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class UrlAssertionTests {

  private final WebTestClient testClient =
          MockMvcWebTestClient.bindToController(new SimpleController()).build();

  @Test
  public void testRedirect() {
    testClient.get().uri("/persons")
            .exchange()
            .expectStatus().isFound()
            .expectHeader().location("/persons/1");
  }

  @Test
  public void testRedirectPattern() throws Exception {
    EntityExchangeResult<Void> result =
            testClient.get().uri("/persons").exchange().expectBody().isEmpty();

    MockMvcWebTestClient.resultActionsFor(result)
            .andExpect(redirectedUrlPattern("/persons/*"));
  }

  @Test
  public void testForward() {
    testClient.get().uri("/")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("Forwarded-Url", "/home");
  }

  @Test
  public void testForwardPattern() throws Exception {
    EntityExchangeResult<Void> result =
            testClient.get().uri("/").exchange().expectBody().isEmpty();

    MockMvcWebTestClient.resultActionsFor(result)
            .andExpect(forwardedUrlPattern("/ho?e"));
  }

  @Controller
  private static class SimpleController {

    @RequestMapping("/persons")
    public String save() {
      return "redirect:/persons/1";
    }

    @RequestMapping("/")
    public String forward() {
      return "/home";
    }
  }
}
