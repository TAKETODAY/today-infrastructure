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

import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Optional;

import infra.core.ReactiveAdapterRegistry;
import infra.messaging.handler.annotation.Payload;
import infra.messaging.rsocket.RSocketRequester;
import infra.messaging.rsocket.RSocketStrategies;
import infra.messaging.rsocket.TestRSocket;
import infra.util.MimeType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

  @Test
  void resolvesRoutesAndUsesCustomArgumentsFromDefaultMethods() {
    RSocketServiceProxyFactory factory = RSocketServiceProxyFactory.builder().rsocketRequester(this.requester)
            .embeddedValueResolver(value -> value.replace("${service}", "orders").replace("${action}", "find"))
            .reactiveAdapterRegistry(new ReactiveAdapterRegistry())
            .customArgumentResolver((argument, parameter, values) -> {
              if (argument instanceof Search search) {
                values.setPayloadValue(search.query());
                return true;
              }
              return false;
            })
            .build();
    SearchService service = factory.createClient(SearchService.class);
    this.rsocket.setPayloadMonoToReturn(Mono.just(DefaultPayload.create("found")));

    StepVerifier.create(service.findByName("book")).expectNext("found").verifyComplete();

    assertThat(this.rsocket.getSavedPayload().getMetadataUtf8()).isEqualTo("orders.find");
    assertThat(this.rsocket.getSavedPayload().getDataUtf8()).isEqualTo("book");
    assertThatIllegalStateException().isThrownBy(service::notAnExchange)
            .withMessageContaining("Unexpected method invocation", "notAnExchange");
  }

  @Test
  void metadataOnlyExchangeUsesMetadataPush() {
    MetadataService service = RSocketServiceProxyFactory.builder(this.requester).build().createClient(MetadataService.class);

    StepVerifier.create(service.push("status", MimeType.TEXT_PLAIN))
            .expectComplete().verify(Duration.ofSeconds(5));

    assertThat(this.rsocket.getSavedMethodName()).isEqualTo("metadataPush");
    assertThat(this.rsocket.getSavedPayload().getMetadataUtf8()).isEqualTo("status");
  }

  @Test
  void routedExchangeCombinesRoutingAndApplicationMetadata() {
    MimeType composite = MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());
    RSocketRequester requester = RSocketRequester.wrap(this.rsocket, MimeType.TEXT_PLAIN, composite,
            RSocketStrategies.create());
    MetadataService service = RSocketServiceProxyFactory.builder(requester).build().createClient(MetadataService.class);
    StepVerifier.create(service.routedPush("status", MimeType.TEXT_PLAIN))
            .expectComplete().verify(Duration.ofSeconds(5));

    assertThat(this.rsocket.getSavedMethodName()).isEqualTo("metadataPush");
    CompositeMetadata metadata = new CompositeMetadata(this.rsocket.getSavedPayload().sliceMetadata(), false);
    assertThat(metadata).extracting(CompositeMetadata.Entry::getMimeType)
            .containsExactly(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString(), MimeType.TEXT_PLAIN.toString());
  }

  @Test
  void exchangeWithoutRouteOrMetadataIsRejected() {
    MetadataService service = RSocketServiceProxyFactory.builder(this.requester).build().createClient(MetadataService.class);

    assertThatIllegalArgumentException().isThrownBy(service::missingRoute)
            .withMessage("Neither route nor metadata provided");
    assertThat(this.rsocket.getSavedPayload()).isNull();
  }

  @Test
  void emptyOptionalAndMissingRequiredPayload() {
    BlockingService service = RSocketServiceProxyFactory.builder(this.requester)
            .blockTimeout(Duration.ofSeconds(1)).build().createClient(BlockingService.class);

    assertThat(service.requestResponseOptional("request")).isEmpty();
    assertThatIllegalArgumentException().isThrownBy(() -> service.requestResponse(null))
            .withMessage("Missing payload");
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

  private interface MetadataService {

    @RSocketExchange
    Mono<Void> push(String metadata, MimeType mimeType);

    @RSocketExchange("orders")
    Mono<Void> routedPush(String metadata, MimeType mimeType);

    @RSocketExchange
    Mono<Void> missingRoute();
  }

  @RSocketExchange("${service}")
  interface SearchService {

    @RSocketExchange("${action}")
    Mono<String> find(Search request);

    default Mono<String> findByName(String query) {
      return find(new Search(query));
    }

    String notAnExchange();
  }

  record Search(String query) {
  }
}
