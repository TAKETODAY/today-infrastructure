/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.reactivestreams.Publisher;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.AbstractDataBufferAllocatingTests;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.codec.json.Jackson2JsonEncoder;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.http.server.reactive.MockServerHttpResponse;
import cn.taketoday.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static cn.taketoday.core.ResolvableType.fromClass;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ServerSentEventHttpMessageWriter}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@SuppressWarnings("rawtypes")
class ServerSentEventHttpMessageWriterTests extends AbstractDataBufferAllocatingTests {

  private static final Map<String, Object> HINTS = Collections.emptyMap();

  private ServerSentEventHttpMessageWriter messageWriter =
          new ServerSentEventHttpMessageWriter(new Jackson2JsonEncoder());

  @ParameterizedDataBufferAllocatingTest
  void canWrite(String displayName, DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    assertThat(this.messageWriter.canWrite(fromClass(Object.class), null)).isTrue();
    assertThat(this.messageWriter.canWrite(fromClass(Object.class), new MediaType("foo", "bar"))).isFalse();

    assertThat(this.messageWriter.canWrite(null, MediaType.TEXT_EVENT_STREAM)).isTrue();
    assertThat(this.messageWriter.canWrite(fromClass(ServerSentEvent.class), new MediaType("foo", "bar"))).isTrue();

    // SPR-15464
    assertThat(this.messageWriter.canWrite(ResolvableType.NONE, MediaType.TEXT_EVENT_STREAM)).isTrue();
    assertThat(this.messageWriter.canWrite(ResolvableType.NONE, new MediaType("foo", "bar"))).isFalse();
  }

  @ParameterizedDataBufferAllocatingTest
  void writeServerSentEvent(String displayName, DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    ServerSentEvent<?> event = ServerSentEvent.builder().data("bar").id("c42").event("foo")
            .comment("bla\nbla bla\nbla bla bla").retry(Duration.ofMillis(123L)).build();

    MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
    Mono<ServerSentEvent> source = Mono.just(event);
    testWrite(source, outputMessage, ServerSentEvent.class);

    StepVerifier.create(outputMessage.getBody())
            .consumeNextWith(stringConsumer(
                    "id:c42\nevent:foo\nretry:123\n:bla\n:bla bla\n:bla bla bla\ndata:bar\n\n"))
            .expectComplete()
            .verify();
  }

  @ParameterizedDataBufferAllocatingTest
  void writeString(String displayName, DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
    Flux<String> source = Flux.just("foo", "bar");
    testWrite(source, outputMessage, String.class);

    StepVerifier.create(outputMessage.getBody())
            .consumeNextWith(stringConsumer("data:foo\n\n"))
            .consumeNextWith(stringConsumer("data:bar\n\n"))
            .expectComplete()
            .verify();
  }

  @ParameterizedDataBufferAllocatingTest
  void writeMultiLineString(String displayName, DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
    Flux<String> source = Flux.just("foo\nbar", "foo\nbaz");
    testWrite(source, outputMessage, String.class);

    StepVerifier.create(outputMessage.getBody())
            .consumeNextWith(stringConsumer("data:foo\ndata:bar\n\n"))
            .consumeNextWith(stringConsumer("data:foo\ndata:baz\n\n"))
            .expectComplete()
            .verify();
  }

  @ParameterizedDataBufferAllocatingTest
    // SPR-16516
  void writeStringWithCustomCharset(String displayName, DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
    Flux<String> source = Flux.just("\u00A3");
    Charset charset = StandardCharsets.ISO_8859_1;
    MediaType mediaType = new MediaType("text", "event-stream", charset);
    testWrite(source, mediaType, outputMessage, String.class);

    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(mediaType);
    StepVerifier.create(outputMessage.getBody())
            .consumeNextWith(dataBuffer -> {
              String value = dataBuffer.toString(charset);
              DataBufferUtils.release(dataBuffer);
              assertThat(value).isEqualTo("data:\u00A3\n\n");
            })
            .expectComplete()
            .verify();
  }

  @ParameterizedDataBufferAllocatingTest
  void writePojo(String displayName, DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
    Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"), new Pojo("foofoofoo", "barbarbar"));
    testWrite(source, outputMessage, Pojo.class);

    StepVerifier.create(outputMessage.getBody())
            .consumeNextWith(stringConsumer("data:"))
            .consumeNextWith(stringConsumer("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"))
            .consumeNextWith(stringConsumer("\n\n"))
            .consumeNextWith(stringConsumer("data:"))
            .consumeNextWith(stringConsumer("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}"))
            .consumeNextWith(stringConsumer("\n\n"))
            .expectComplete()
            .verify();
  }

  @ParameterizedDataBufferAllocatingTest
    // SPR-14899
  void writePojoWithPrettyPrint(String displayName, DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().indentOutput(true).build();
    this.messageWriter = new ServerSentEventHttpMessageWriter(new Jackson2JsonEncoder(mapper));

    MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
    Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"), new Pojo("foofoofoo", "barbarbar"));
    testWrite(source, outputMessage, Pojo.class);

    StepVerifier.create(outputMessage.getBody())
            .consumeNextWith(stringConsumer("data:"))
            .consumeNextWith(stringConsumer("{\n" +
                                                    "data:  \"foo\" : \"foofoo\",\n" +
                                                    "data:  \"bar\" : \"barbar\"\n" + "data:}"))
            .consumeNextWith(stringConsumer("\n\n"))
            .consumeNextWith(stringConsumer("data:"))
            .consumeNextWith(stringConsumer("{\n" +
                                                    "data:  \"foo\" : \"foofoofoo\",\n" +
                                                    "data:  \"bar\" : \"barbarbar\"\n" + "data:}"))
            .consumeNextWith(stringConsumer("\n\n"))
            .expectComplete()
            .verify();
  }

  @ParameterizedDataBufferAllocatingTest
    // SPR-16516, SPR-16539
  void writePojoWithCustomEncoding(String displayName, DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;

    MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
    Flux<Pojo> source = Flux.just(new Pojo("foo\uD834\uDD1E", "bar\uD834\uDD1E"));
    Charset charset = StandardCharsets.UTF_16LE;
    MediaType mediaType = new MediaType("text", "event-stream", charset);
    testWrite(source, mediaType, outputMessage, Pojo.class);

    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(mediaType);
    StepVerifier.create(outputMessage.getBody())
            .consumeNextWith(stringConsumer("data:", charset))
            .consumeNextWith(stringConsumer("{\"foo\":\"foo\uD834\uDD1E\",\"bar\":\"bar\uD834\uDD1E\"}", charset))
            .consumeNextWith(stringConsumer("\n\n", charset))
            .expectComplete()
            .verify();
  }

  private <T> void testWrite(Publisher<T> source, MockServerHttpResponse response, Class<T> clazz) {
    testWrite(source, MediaType.TEXT_EVENT_STREAM, response, clazz);
  }

  private <T> void testWrite(
          Publisher<T> source, MediaType mediaType, MockServerHttpResponse response, Class<T> clazz) {

    Mono<Void> result =
            this.messageWriter.write(source, fromClass(clazz), mediaType, response, HINTS);

    StepVerifier.create(result).verifyComplete();
  }

}
