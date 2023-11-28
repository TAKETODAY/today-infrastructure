/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http.codec;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferLimitException;
import cn.taketoday.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.json.Jackson2JsonDecoder;
import cn.taketoday.web.testfixture.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ServerSentEventHttpMessageReader}.
 *
 * @author Sebastien Deleuze
 */
public class ServerSentEventHttpMessageReaderTests extends AbstractLeakCheckingTests {

  private Jackson2JsonDecoder jsonDecoder = new Jackson2JsonDecoder();

  private ServerSentEventHttpMessageReader reader = new ServerSentEventHttpMessageReader(this.jsonDecoder);

  @Test
  public void cantRead() {
    assertThat(reader.canRead(ResolvableType.forClass(Object.class), new MediaType("foo", "bar"))).isFalse();
    assertThat(reader.canRead(ResolvableType.forClass(Object.class), null)).isFalse();
  }

  @Test
  public void canRead() {
    assertThat(reader.canRead(ResolvableType.forClass(Object.class), new MediaType("text", "event-stream"))).isTrue();
    assertThat(reader.canRead(ResolvableType.forClass(ServerSentEvent.class), new MediaType("foo", "bar"))).isTrue();
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void readServerSentEvents() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Mono.just(stringBuffer(
                    "id:c42\nevent:foo\nretry:123\n:bla\n:bla bla\n:bla bla bla\ndata:bar\n\n" +
                            "id:c43\nevent:bar\nretry:456\ndata:baz\n\ndata:\n\ndata: \n\n")));

    Flux<ServerSentEvent> events = this.reader
            .read(ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class),
                    request, Collections.emptyMap()).cast(ServerSentEvent.class);

    StepVerifier.create(events)
            .consumeNextWith(event -> {
              assertThat(event.id()).isEqualTo("c42");
              assertThat(event.event()).isEqualTo("foo");
              assertThat(event.retry()).isEqualTo(Duration.ofMillis(123));
              assertThat(event.comment()).isEqualTo("bla\nbla bla\nbla bla bla");
              assertThat(event.data()).isEqualTo("bar");
            })
            .consumeNextWith(event -> {
              assertThat(event.id()).isEqualTo("c43");
              assertThat(event.event()).isEqualTo("bar");
              assertThat(event.retry()).isEqualTo(Duration.ofMillis(456));
              assertThat(event.comment()).isNull();
              assertThat(event.data()).isEqualTo("baz");
            })
            .consumeNextWith(event -> Assertions.assertThat(event.data()).isNull())
            .consumeNextWith(event -> Assertions.assertThat(event.data()).isNull())
            .expectComplete()
            .verify();
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void readServerSentEventsWithMultipleChunks() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Flux.just(
                    stringBuffer("id:c42\nev"),
                    stringBuffer("ent:foo\nretry:123\n:bla\n:bla bla\n:bla bla bla\ndata:"),
                    stringBuffer("bar\n\nid:c43\nevent:bar\nretry:456\ndata:baz\n\n")));

    Flux<ServerSentEvent> events = reader
            .read(ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class),
                    request, Collections.emptyMap()).cast(ServerSentEvent.class);

    StepVerifier.create(events)
            .consumeNextWith(event -> {
              assertThat(event.id()).isEqualTo("c42");
              assertThat(event.event()).isEqualTo("foo");
              assertThat(event.retry()).isEqualTo(Duration.ofMillis(123));
              assertThat(event.comment()).isEqualTo("bla\nbla bla\nbla bla bla");
              assertThat(event.data()).isEqualTo("bar");
            })
            .consumeNextWith(event -> {
              assertThat(event.id()).isEqualTo("c43");
              assertThat(event.event()).isEqualTo("bar");
              assertThat(event.retry()).isEqualTo(Duration.ofMillis(456));
              assertThat(event.comment()).isNull();
              assertThat(event.data()).isEqualTo("baz");
            })
            .expectComplete()
            .verify();
  }

  @Test
  public void readString() {
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
  public void trimWhitespace() {
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
  public void readPojo() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Mono.just(stringBuffer(
                    "data:{\"foo\": \"foofoo\", \"bar\": \"barbar\"}\n\n" +
                            "data:{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}\n\n")));

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

  @Test  // SPR-15331
  public void decodeFullContentAsString() {
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
  public void readError() {
    Flux<DataBuffer> body =
            Flux.just(stringBuffer("data:foo\ndata:bar\n\ndata:baz\n\n"))
                    .concatWith(Flux.error(new RuntimeException()));

    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(body);

    Flux<String> data = reader.read(ResolvableType.forClass(String.class),
            request, Collections.emptyMap()).cast(String.class);

    StepVerifier.create(data)
            .expectNextMatches(elem -> elem.equals("foo\nbar"))
            .expectNextMatches(elem -> elem.equals("baz"))
            .expectError()
            .verify();
  }

  @Test
  public void maxInMemoryLimit() {

    this.reader.setMaxInMemorySize(17);

    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .body(Flux.just(stringBuffer("data:\"TOO MUCH DATA\"\ndata:bar\n\ndata:baz\n\n")));

    Flux<String> data = this.reader.read(ResolvableType.forClass(String.class),
            request, Collections.emptyMap()).cast(String.class);

    StepVerifier.create(data)
            .expectError(DataBufferLimitException.class)
            .verify();
  }

  @Test // gh-24312
  public void maxInMemoryLimitAllowsReadingPojoLargerThanDefaultSize() {

    int limit = this.jsonDecoder.getMaxInMemorySize();

    String fooValue = getStringOfSize(limit) + "and then some more";
    String content = "data:{\"foo\": \"" + fooValue + "\"}\n\n";
    MockServerHttpRequest request = MockServerHttpRequest.post("/").body(Mono.just(stringBuffer(content)));

    Jackson2JsonDecoder jacksonDecoder = new Jackson2JsonDecoder();
    ServerSentEventHttpMessageReader messageReader = new ServerSentEventHttpMessageReader(jacksonDecoder);

    jacksonDecoder.setMaxInMemorySize(limit + 1024);
    messageReader.setMaxInMemorySize(limit + 1024);

    Flux<Pojo> data = messageReader.read(ResolvableType.forClass(Pojo.class), request,
            Collections.emptyMap()).cast(Pojo.class);

    StepVerifier.create(data)
            .consumeNextWith(pojo -> Assertions.assertThat(pojo.getFoo()).isEqualTo(fooValue))
            .expectComplete()
            .verify();
  }

  private static String getStringOfSize(long size) {
    StringBuilder content = new StringBuilder("Aa");
    while (content.length() < size) {
      content.append(content);
    }
    return content.toString();
  }

  private DataBuffer stringBuffer(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
    buffer.write(bytes);
    return buffer;
  }

}
