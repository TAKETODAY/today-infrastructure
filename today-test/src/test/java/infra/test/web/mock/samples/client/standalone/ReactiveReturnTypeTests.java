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

package infra.test.web.mock.samples.client.standalone;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.http.MediaType;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static infra.http.MediaType.TEXT_EVENT_STREAM;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.ReactiveReturnTypeTests}.
 *
 * @author Rossen Stoyanchev
 */
public class ReactiveReturnTypeTests {

  @Test
  public void sseWithFlux() {

    WebTestClient testClient =
            MockMvcWebTestClient.bindToController(new ReactiveController()).build();

    Flux<String> bodyFlux = testClient.get().uri("/spr16869")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
            .returnResult(String.class)
            .getResponseBody();

    StepVerifier.create(bodyFlux)
            .expectNext("event0")
            .expectNext("event1")
            .expectNext("event2")
            .verifyComplete();
  }

  @RestController
  static class ReactiveController {

    @GetMapping(path = "/spr16869", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> sseFlux() {
      return Flux.interval(Duration.ofSeconds(1)).take(3)
              .map(aLong -> String.format("event%d", aLong));
    }
  }

}
