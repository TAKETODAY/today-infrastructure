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
