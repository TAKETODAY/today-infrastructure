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

package infra.http.codec.cbor;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import infra.core.testfixture.io.buffer.DataBufferTestUtils;
import infra.http.MediaType;
import infra.http.codec.Pojo;
import reactor.core.publisher.Flux;
import tools.jackson.dataformat.cbor.CBORMapper;

import static infra.http.MediaType.APPLICATION_XML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JacksonCborEncoder}.
 *
 * @author Sebastien Deleuze
 */
class JacksonCborEncoderTests extends AbstractLeakCheckingTests {

  private final CBORMapper mapper = CBORMapper.builder().build();

  private final JacksonCborEncoder encoder = new JacksonCborEncoder();

  private Consumer<DataBuffer> pojoConsumer(Pojo expected) {
    return dataBuffer -> {
      Pojo actual = this.mapper.reader().forType(Pojo.class)
              .readValue(DataBufferTestUtils.dumpBytes(dataBuffer));
      assertThat(actual).isEqualTo(expected);
      dataBuffer.release();
    };
  }

  @Test
  void canEncode() {
    ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
    assertThat(this.encoder.canEncode(pojoType, MediaType.APPLICATION_CBOR)).isTrue();
    assertThat(this.encoder.canEncode(pojoType, null)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isTrue();

    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();
  }

  @Test
  void canNotEncode() {
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML)).isFalse();
  }

  @Test
  void encode() {
    Pojo value = new Pojo("foo", "bar");
    DataBuffer result = encoder.encodeValue(value, this.bufferFactory, ResolvableType.forClass(Pojo.class),
            MediaType.APPLICATION_CBOR, null);
    pojoConsumer(value).accept(result);
  }

  @Test
  void encodeStream() {
    Pojo pojo1 = new Pojo("foo", "bar");
    Pojo pojo2 = new Pojo("foofoo", "barbar");
    Pojo pojo3 = new Pojo("foofoofoo", "barbarbar");
    Flux<Pojo> input = Flux.just(pojo1, pojo2, pojo3);
    ResolvableType type = ResolvableType.forClass(Pojo.class);
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            encoder.encode(input, this.bufferFactory, type, MediaType.APPLICATION_CBOR, null));
  }
}
