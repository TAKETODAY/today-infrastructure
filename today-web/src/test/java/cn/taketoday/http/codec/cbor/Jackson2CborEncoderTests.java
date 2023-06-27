/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.codec.cbor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.testfixture.io.buffer.DataBufferTestUtils;
import cn.taketoday.http.codec.Pojo;
import cn.taketoday.http.codec.ServerSentEvent;
import cn.taketoday.http.codec.json.AbstractLeakCheckingTests;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;

import static cn.taketoday.core.io.buffer.DataBufferUtils.release;
import static cn.taketoday.http.MediaType.APPLICATION_XML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link Jackson2CborEncoder}.
 *
 * @author Sebastien Deleuze
 */
public class Jackson2CborEncoderTests extends AbstractLeakCheckingTests {

  private final static MimeType CBOR_MIME_TYPE = new MimeType("application", "cbor");

  private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.cbor().build();

  private final Jackson2CborEncoder encoder = new Jackson2CborEncoder();

  private Consumer<DataBuffer> pojoConsumer(Pojo expected) {
    return dataBuffer -> {
      try {
        Pojo actual = this.mapper.reader().forType(Pojo.class)
                .readValue(DataBufferTestUtils.dumpBytes(dataBuffer));
        assertThat(actual).isEqualTo(expected);
        release(dataBuffer);
      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    };
  }

  @Test
  public void canEncode() {
    ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
    assertThat(this.encoder.canEncode(pojoType, CBOR_MIME_TYPE)).isTrue();
    assertThat(this.encoder.canEncode(pojoType, null)).isTrue();

    // SPR-15464
    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();
  }

  @Test
  public void canNotEncode() {
    assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isFalse();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML)).isFalse();

    ResolvableType sseType = ResolvableType.forClass(ServerSentEvent.class);
    assertThat(this.encoder.canEncode(sseType, CBOR_MIME_TYPE)).isFalse();
  }

  @Test
  public void encode() {
    Pojo value = new Pojo("foo", "bar");
    DataBuffer result = encoder.encodeValue(value, this.bufferFactory, ResolvableType.forClass(Pojo.class), CBOR_MIME_TYPE, null);
    pojoConsumer(value).accept(result);
  }

  @Test
  public void encodeStream() {
    Pojo pojo1 = new Pojo("foo", "bar");
    Pojo pojo2 = new Pojo("foofoo", "barbar");
    Pojo pojo3 = new Pojo("foofoofoo", "barbarbar");
    Flux<Pojo> input = Flux.just(pojo1, pojo2, pojo3);
    ResolvableType type = ResolvableType.forClass(Pojo.class);
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            encoder.encode(input, this.bufferFactory, type, CBOR_MIME_TYPE, null));
  }
}
