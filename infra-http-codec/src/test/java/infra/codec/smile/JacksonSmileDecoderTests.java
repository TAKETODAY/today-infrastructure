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

package infra.codec.smile;

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
