/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.codec.smile;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferUtils;
import infra.core.testfixture.codec.AbstractEncoderTests;
import infra.http.codec.Pojo;
import infra.http.codec.ServerSentEvent;
import infra.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.smile.SmileMapper;

import static infra.http.MediaType.APPLICATION_XML;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JacksonSmileEncoder}.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
class JacksonSmileEncoderTests extends AbstractEncoderTests<JacksonSmileEncoder> {

  private static final MimeType SMILE_MIME_TYPE = new MimeType("application", "x-jackson-smile");
  private static final MimeType STREAM_SMILE_MIME_TYPE = new MimeType("application", "stream+x-jackson-smile");

  private final SmileMapper mapper = SmileMapper.builder().build();

  JacksonSmileEncoderTests() {
    super(new JacksonSmileEncoder());
  }

  @Test
  @Override
  protected void canEncode() {
    ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
    assertThat(this.encoder.canEncode(pojoType, SMILE_MIME_TYPE)).isTrue();
    assertThat(this.encoder.canEncode(pojoType, STREAM_SMILE_MIME_TYPE)).isTrue();
    assertThat(this.encoder.canEncode(pojoType, null)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isTrue();

    // SPR-15464
    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();
  }

  @Test
  void cannotEncode() {

    assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML)).isFalse();
  }

  @Test
  @Disabled("Determine why this fails with JacksonSmileEncoder but passes with Jackson2SmileEncoder")
  void cannotEncodeServerSentEvent() {
    ResolvableType sseType = ResolvableType.forClass(ServerSentEvent.class);
    assertThat(this.encoder.canEncode(sseType, SMILE_MIME_TYPE)).isFalse();
  }

  @Test
  @Override
  protected void encode() {
    List<Pojo> list = Arrays.asList(
            new Pojo("foo", "bar"),
            new Pojo("foofoo", "barbar"),
            new Pojo("foofoofoo", "barbarbar"));

    Flux<Pojo> input = Flux.fromIterable(list);

    testEncode(input, Pojo.class, step -> step
            .consumeNextWith(dataBuffer -> {
              try {
                Object actual = this.mapper.reader().forType(List.class)
                        .readValue(dataBuffer.asInputStream());
                assertThat(actual).isEqualTo(list);
              }
              finally {
                dataBuffer.release();
              }
            }));
  }

  @Test
  void encodeError() {
    Mono<Pojo> input = Mono.error(new InputException());
    testEncode(input, Pojo.class, step -> step.expectError(InputException.class).verify());
  }

  @Test
  void encodeAsStream() {
    Pojo pojo1 = new Pojo("foo", "bar");
    Pojo pojo2 = new Pojo("foofoo", "barbar");
    Pojo pojo3 = new Pojo("foofoofoo", "barbarbar");
    Flux<Pojo> input = Flux.just(pojo1, pojo2, pojo3);
    ResolvableType type = ResolvableType.forClass(Pojo.class);

    Flux<DataBuffer> result = this.encoder
            .encode(input, bufferFactory, type, STREAM_SMILE_MIME_TYPE, null);

    Mono<MappingIterator<Pojo>> joined = DataBufferUtils.join(result)
            .map(buffer -> this.mapper.reader().forType(Pojo.class).readValues(buffer.asInputStream(true)));

    StepVerifier.create(joined)
            .assertNext(iter -> assertThat(iter).toIterable().contains(pojo1, pojo2, pojo3))
            .verifyComplete();
  }

}
