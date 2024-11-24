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

package infra.http.codec;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferLimitException;
import infra.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import infra.http.MediaType;
import infra.http.codec.json.Jackson2JsonDecoder;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ServerSentEventHttpMessageReader}.
 *
 * @author Sebastien Deleuze
 */
class ServerSentEventHttpMessageReaderTests extends AbstractLeakCheckingTests {

  private final Jackson2JsonDecoder jsonDecoder = new Jackson2JsonDecoder();

  private ServerSentEventHttpMessageReader reader = new ServerSentEventHttpMessageReader(this.jsonDecoder);

  @Test
  void cannotRead() {
    assertThat(reader.canRead(ResolvableType.forClass(Object.class), new MediaType("foo", "bar"))).isFalse();
    assertThat(reader.canRead(ResolvableType.forClass(Object.class), null)).isFalse();
  }

  @Test
  void canRead() {
    assertThat(reader.canRead(ResolvableType.forClass(Object.class), new MediaType("text", "event-stream"))).isTrue();
    assertThat(reader.canRead(ResolvableType.forClass(ServerSentEvent.class), new MediaType("foo", "bar"))).isTrue();
  }

  @Test
  @SuppressWarnings("rawtypes")
  void readServerSentEvents() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Mono.just(stringBuffer(
                    "id:c42\nevent:foo\nretry:123\n:bla\n:bla bla\n:bla bla bla\ndata:bar\n\n" +
                            "id:c43\nevent:bar\nretry:456\ndata:baz\n\ndata:\n\ndata: \n\n")));

    Flux<ServerSentEvent> events = this.reader
            .read(ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class),
                    request, Collections.emptyMap()).cast(ServerSentEvent.class);

    StepVerifier.create(events)
            .expectNext(ServerSentEvent.builder().id("c42").event("foo")
                    .retry(Duration.ofMillis(123)).comment("bla\nbla bla\nbla bla bla").data("bar").build())
            .expectNext(ServerSentEvent.builder().id("c43").event("bar")
                    .retry(Duration.ofMillis(456)).data("baz").build())
            .consumeNextWith(event -> assertThat(event.data()).isNull())
            .consumeNextWith(event -> assertThat(event.data()).isNull())
            .expectComplete()
            .verify();
  }

  @Test
  @SuppressWarnings("rawtypes")
  void readServerSentEventsWithMultipleChunks() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Flux.just(
                    stringBuffer("id:c42\nev"),
                    stringBuffer("ent:foo\nretry:123\n:bla\n:bla bla\n:bla bla bla\ndata:"),
                    stringBuffer("bar\n\nid:c43\nevent:bar\nretry:456\ndata:baz\n\n")));

    Flux<ServerSentEvent> events = reader
            .read(ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class),
                    request, Collections.emptyMap()).cast(ServerSentEvent.class);

    StepVerifier.create(events)
            .expectNext(ServerSentEvent.builder().id("c42").event("foo")
                    .retry(Duration.ofMillis(123)).comment("bla\nbla bla\nbla bla bla").data("bar").build())
            .expectNext(ServerSentEvent.builder().id("c43").event("bar")
                    .retry(Duration.ofMillis(456)).data("baz").build())
            .expectComplete()
            .verify();
  }

  @Test
  void readString() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Mono.just(stringBuffer("data:foo\ndata:bar\n\ndata:baz\n\n")));

    Flux<String> data = reader.read(ResolvableType.forClass(String.class),
            request, Collections.emptyMap()).cast(String.class);

    StepVerifier.create(data)
            .expectNextMatches(elem -> elem.equals("foo\nbar"))
            .expectNextMatches(elem -> elem.equals("baz"))
            .expectComplete()
            .verify();
  }

  @Test
  void trimWhitespace() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Mono.just(stringBuffer("data: \tfoo \ndata:bar\t\n\n")));

    Flux<String> data = reader.read(ResolvableType.forClass(String.class),
            request, Collections.emptyMap()).cast(String.class);

    StepVerifier.create(data)
            .expectNext("\tfoo \nbar\t")
            .expectComplete()
            .verify();
  }

  @Test
  void readPojo() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Mono.just(stringBuffer("""
                    data:{"foo": "foofoo", "bar": "barbar"}

                    data:{"foo": "foofoofoo", "bar": "barbarbar"}

                    """)));

    Flux<Pojo> data = reader.read(ResolvableType.forClass(Pojo.class), request,
            Collections.emptyMap()).cast(Pojo.class);

    StepVerifier.create(data)
            .consumeNextWith(pojo -> {
              assertThat(pojo.getFoo()).isEqualTo("foofoo");
              assertThat(pojo.getBar()).isEqualTo("barbar");
            })
            .consumeNextWith(pojo -> {
              assertThat(pojo.getFoo()).isEqualTo("foofoofoo");
              assertThat(pojo.getBar()).isEqualTo("barbarbar");
            })
            .expectComplete()
            .verify();
  }

  @Test
    // gh-24389
  void readPojoWithCommentOnly() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Flux.just(stringBuffer(":ping\n"), stringBuffer("\n")));

    Flux<Object> data = this.reader.read(
            ResolvableType.forType(String.class), request, Collections.emptyMap());

    StepVerifier.create(data).expectComplete().verify();
  }

  @Test
  void decodeFullContentAsString() {
    String body = "data:foo\ndata:bar\n\ndata:baz\n\n";
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Mono.just(stringBuffer(body)));

    String actual = reader
            .readMono(ResolvableType.forClass(String.class), request, Collections.emptyMap())
            .cast(String.class)
            .block(Duration.ZERO);

    assertThat(actual).isEqualTo(body);
  }

  @Test
  void readError() {
    Flux<DataBuffer> body = Flux.just(stringBuffer("data:foo\ndata:bar\n\ndata:baz\n\n"))
            .concatWith(Flux.error(new RuntimeException()));

    MockServerHttpRequest request = MockServerHttpRequest.post("/").body(body);

    Flux<String> data = reader.read(ResolvableType.forClass(String.class),
            request, Collections.emptyMap()).cast(String.class);

    StepVerifier.create(data)
            .expectNextMatches(elem -> elem.equals("foo\nbar"))
            .expectNextMatches(elem -> elem.equals("baz"))
            .expectError()
            .verify();
  }

  @Test
  void maxInMemoryLimit() {
    this.reader.setMaxInMemorySize(17);

    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Flux.just(stringBuffer("data:\"TOO MUCH DATA\"\ndata:bar\n\ndata:baz\n\n")));

    Flux<String> data = this.reader.read(ResolvableType.forClass(String.class),
            request, Collections.emptyMap()).cast(String.class);

    StepVerifier.create(data)
            .expectError(DataBufferLimitException.class)
            .verify();
  }

  @Test
  void maxInMemoryLimitAllowsReadingPojoLargerThanDefaultSize() {
    int limit = this.jsonDecoder.getMaxInMemorySize();

    String fooValue = "x".repeat(limit) + " and then some more";
    String content = "data:{\"foo\": \"" + fooValue + "\"}\n\n";
    MockServerHttpRequest request = MockServerHttpRequest.post("/").body(Mono.just(stringBuffer(content)));

    Jackson2JsonDecoder jacksonDecoder = new Jackson2JsonDecoder();
    ServerSentEventHttpMessageReader messageReader = new ServerSentEventHttpMessageReader(jacksonDecoder);

    jacksonDecoder.setMaxInMemorySize(limit + 1024);
    messageReader.setMaxInMemorySize(limit + 1024);

    Flux<Pojo> data = messageReader.read(ResolvableType.forClass(Pojo.class), request,
            Collections.emptyMap()).cast(Pojo.class);

    StepVerifier.create(data)
            .consumeNextWith(pojo -> assertThat(pojo.getFoo()).isEqualTo(fooValue))
            .expectComplete()
            .verify();
  }

  private DataBuffer stringBuffer(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
    buffer.write(bytes);
    return buffer;
  }

}
