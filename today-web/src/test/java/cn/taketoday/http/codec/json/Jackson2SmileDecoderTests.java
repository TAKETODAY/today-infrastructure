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

package cn.taketoday.http.codec.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.testfixture.codec.AbstractDecoderTests;
import cn.taketoday.http.codec.Pojo;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;

import static cn.taketoday.http.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Jackson2SmileDecoder}.
 *
 * @author Sebastien Deleuze
 */
public class Jackson2SmileDecoderTests extends AbstractDecoderTests<Jackson2SmileDecoder> {

  private final static MimeType SMILE_MIME_TYPE = new MimeType("application", "x-jackson-smile");
  private final static MimeType STREAM_SMILE_MIME_TYPE = new MimeType("application", "stream+x-jackson-smile");

  private Pojo pojo1 = new Pojo("f1", "b1");

  private Pojo pojo2 = new Pojo("f2", "b2");

  private ObjectMapper mapper = Jackson2ObjectMapperBuilder.smile().build();

  public Jackson2SmileDecoderTests() {
    super(new Jackson2SmileDecoder());
  }

  @Override
  @Test
  public void canDecode() {
    assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), SMILE_MIME_TYPE)).isTrue();
    assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), STREAM_SMILE_MIME_TYPE)).isTrue();
    assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), null)).isTrue();

    assertThat(decoder.canDecode(ResolvableType.forClass(String.class), null)).isFalse();
    assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), APPLICATION_JSON)).isFalse();
  }

  @Override
  @Test
  public void decode() {
    Flux<DataBuffer> input = Flux.just(this.pojo1, this.pojo2)
            .map(this::writeObject)
            .flatMap(this::dataBuffer);

    testDecodeAll(input, Pojo.class, step -> step
            .expectNext(pojo1)
            .expectNext(pojo2)
            .verifyComplete());

  }

  private byte[] writeObject(Object o) {
    try {
      return this.mapper.writer().writeValueAsBytes(o);
    }
    catch (JsonProcessingException e) {
      throw new AssertionError(e);
    }

  }

  @Override
  @Test
  public void decodeToMono() {
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
