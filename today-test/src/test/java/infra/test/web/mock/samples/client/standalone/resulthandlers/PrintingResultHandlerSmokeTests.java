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

package infra.test.web.mock.samples.client.standalone.resulthandlers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import infra.http.MediaType;
import infra.mock.api.http.Cookie;
import infra.mock.api.http.HttpMockResponse;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.EntityExchangeResult;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.PostMapping;
import infra.web.annotation.RestController;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.resulthandlers.PrintingResultHandlerSmokeTests}.
 *
 * @author Rossen Stoyanchev
 */
@Disabled
public class PrintingResultHandlerSmokeTests {

  private final WebTestClient testClient =
          MockMvcWebTestClient.bindToController(new SimpleController()).build();

  // Not intended to be executed with the build.
  // Comment out class-level @Disabled to see the output.

  @Test
  public void printViaConsumer() {
    testClient.post().uri("/")
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue("Hello Request".getBytes(StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(System.out::println);
  }

  @Test
  public void returnResultAndPrint() {
    EntityExchangeResult<String> result = testClient.post().uri("/")
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue("Hello Request".getBytes(StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult();

    System.out.println(result);
  }

  @RestController
  private static class SimpleController {

    @PostMapping("/")
    public String hello(HttpMockResponse response) {
      response.addCookie(new Cookie("enigma", "42"));
      return "Hello Response";
    }
  }
}
