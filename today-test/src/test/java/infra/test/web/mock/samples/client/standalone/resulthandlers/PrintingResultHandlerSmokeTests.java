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

package infra.test.web.mock.samples.client.standalone.resulthandlers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import infra.http.MediaType;
import infra.test.web.reactive.server.EntityExchangeResult;
import infra.test.web.reactive.server.WebTestClient;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.web.annotation.PostMapping;
import infra.web.annotation.RestController;
import infra.mock.api.http.Cookie;
import infra.mock.api.http.HttpMockResponse;

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
