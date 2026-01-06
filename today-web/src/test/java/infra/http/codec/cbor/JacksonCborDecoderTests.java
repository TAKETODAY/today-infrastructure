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

import java.util.Arrays;
import java.util.List;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.codec.AbstractDecoderTests;
import infra.http.MediaType;
import infra.http.codec.Pojo;
import reactor.core.publisher.Flux;
import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.cbor.CBORMapper;

import static infra.http.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JacksonCborDecoder}.
 *
 * @author Sebastien Deleuze
 */
class JacksonCborDecoderTests extends AbstractDecoderTests<JacksonCborDecoder> {

  private Pojo pojo1 = new Pojo("f1", "b1");

  private Pojo pojo2 = new Pojo("f2", "b2");

  private CBORMapper mapper = CBORMapper.builder().build();

  public JacksonCborDecoderTests() {
    super(new JacksonCborDecoder());
  }

  @Override
  @Test
  protected void canDecode() {
    assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), MediaType.APPLICATION_CBOR)).isTrue();
    assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), null)).isTrue();

    assertThat(decoder.canDecode(ResolvableType.forClass(String.class), null)).isTrue();
    assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), APPLICATION_JSON)).isFalse();
  }

  @Override
  @Test
  protected void decode() {
    Flux<DataBuffer> input = Flux.just(this.pojo1, this.pojo2)
            .map(this::writeObject)
            .flatMap(this::dataBuffer);
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            testDecodeAll(input, Pojo.class, step -> step
                    .expectNext(pojo1)
                    .expectNext(pojo2)
                    .verifyComplete()));

  }

  private byte[] writeObject(Object o) {
    try {
      return this.mapper.writer().writeValueAsBytes(o);
    }
    catch (JacksonException e) {
      throw new AssertionError(e);
    }

  }

  @Override
  @Test
  protected void decodeToMono() {
    List<Pojo> expected = Arrays.asList(pojo1, pojo2);

    Flux<DataBuffer> input = Flux.just(expected)
            .map(this::writeObject)
            .flatMap(this::dataBuffer);

    ResolvableType elementType = ResolvableType.forClassWithGenerics(List.class, Pojo.class);
    testDecodeToMono(input, elementType, step -> step
            .expectNext(expected)
            .expectComplete()
            .verify(), null, null);
  }
}
