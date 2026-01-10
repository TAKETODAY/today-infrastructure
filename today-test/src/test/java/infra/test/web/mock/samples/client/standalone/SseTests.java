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

import infra.test.web.Person;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.FluxExchangeResult;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * SSE controller tests with MockMvc and WebTestClient.
 *
 * @author Rossen Stoyanchev
 */
public class SseTests {

  private final WebTestClient testClient =
          MockMvcWebTestClient.bindToController(new SseController()).build();

  @Test
  public void sse() {
    FluxExchangeResult<Person> exchangeResult = this.testClient.get()
            .uri("/persons")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("text/event-stream")
            .returnResult(Person.class);

    StepVerifier.create(exchangeResult.getResponseBody())
            .expectNext(new Person("N0"), new Person("N1"), new Person("N2"))
            .expectNextCount(4)
            .consumeNextWith(person -> assertThat(person.getName()).endsWith("7"))
            .thenCancel()
            .verify();
  }

  @RestController
  private static class SseController {

    @GetMapping(path = "/persons", produces = "text/event-stream")
    public Flux<Person> getPersonStream() {
      return Flux.interval(ofMillis(100)).take(50).onBackpressureBuffer(50)
              .map(index -> new Person("N" + index));
    }
  }

}
