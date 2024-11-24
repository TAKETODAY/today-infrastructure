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

import infra.test.web.Person;
import infra.test.web.reactive.server.FluxExchangeResult;
import infra.test.web.reactive.server.WebTestClient;
import infra.test.web.mock.client.MockMvcWebTestClient;
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
