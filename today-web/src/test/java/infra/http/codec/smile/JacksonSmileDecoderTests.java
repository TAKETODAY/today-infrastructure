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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.codec.AbstractDecoderTests;
import infra.http.codec.Pojo;
import infra.util.MimeType;
import reactor.core.publisher.Flux;
import tools.jackson.dataformat.smile.SmileMapper;

import static infra.http.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JacksonSmileDecoder}.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
class JacksonSmileDecoderTests extends AbstractDecoderTests<JacksonSmileDecoder> {

  private static final MimeType SMILE_MIME_TYPE = new MimeType("application", "x-jackson-smile");
  private static final MimeType STREAM_SMILE_MIME_TYPE = new MimeType("application", "stream+x-jackson-smile");

  private Pojo pojo1 = new Pojo("f1", "b1");

  private Pojo pojo2 = new Pojo("f2", "b2");

  private SmileMapper mapper = SmileMapper.builder().build();

  JacksonSmileDecoderTests() {
    super(new JacksonSmileDecoder());
  }

  @Override
  @Test
  protected void canDecode() {
    assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), SMILE_MIME_TYPE)).isTrue();
    assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), STREAM_SMILE_MIME_TYPE)).isTrue();
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

    testDecodeAll(input, Pojo.class, step -> step
            .expectNext(pojo1)
            .expectNext(pojo2)
            .verifyComplete());

  }

  private byte[] writeObject(Object o) {
    return this.mapper.writer().writeValueAsBytes(o);
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
