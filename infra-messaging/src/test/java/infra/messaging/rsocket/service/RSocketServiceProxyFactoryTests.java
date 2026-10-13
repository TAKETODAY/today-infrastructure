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

package infra.messaging.rsocket.service;

import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Optional;

import infra.messaging.handler.annotation.Payload;
import infra.messaging.rsocket.RSocketRequester;
import infra.messaging.rsocket.RSocketStrategies;
import infra.messaging.rsocket.TestRSocket;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link RSocketServiceProxyFactory}.
 *
 * @author Sam Brannen
 * @since 5.0
 */
class RSocketServiceProxyFactoryTests {

  private final TestRSocket rsocket = new TestRSocket();

  private final RSocketRequester requester =
          RSocketRequester.wrap(this.rsocket, infra.util.MimeType.valueOf("text/plain"),
                  infra.util.MimeType.valueOf("text/plain"), RSocketStrategies.create());

  @Test
  @Timeout(5)
  void blockTimeoutZeroSpecifiesInfiniteTimeout() {
    var factory = RSocketServiceProxyFactory.builder(this.requester).blockTimeout(Duration.ZERO).build();
    BlockingService service = factory.createClient(BlockingService.class);
    this.rsocket.setPayloadMonoToReturn(delayedPayload("p2"));
    assertThat(service.requestResponse("p1")).isEqualTo("p2");
    this.rsocket.setPayloadMonoToReturn(delayedPayload("p3"));
    assertThat(service.requestResponseOptional("p1")).contains("p3");
  }

  @Test
  void blockTimeoutExceeded() {
    var factory = RSocketServiceProxyFactory.builder(this.requester).blockTimeout(Duration.ofMillis(10)).build();
    BlockingService service = factory.createClient(BlockingService.class);
    this.rsocket.setPayloadMonoToReturn(Mono.never());
    assertThatIllegalStateException().isThrownBy(() -> service.requestResponse("p1"))
            .withMessageStartingWith("Timeout on blocking read");
  }

  @Test
  void negativeBlockTimeoutIsRejected() {
    var builder = RSocketServiceProxyFactory.builder(this.requester);
    assertThatIllegalArgumentException().isThrownBy(() -> builder.blockTimeout(Duration.ofMillis(-1)))
            .withMessage("Timeout must be a non-negative value");
  }

  private static Mono<io.rsocket.Payload> delayedPayload(String data) {
    return Mono.just(DefaultPayload.create(data)).delayElement(Duration.ofMillis(50));
  }

  private interface BlockingService {

    @RSocketExchange("rr")
    String requestResponse(@Payload String input);

    @RSocketExchange("rr")
    Optional<String> requestResponseOptional(@Payload String input);
  }
}
