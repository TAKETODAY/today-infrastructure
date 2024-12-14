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

package infra.core.codec;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.codec.AbstractDecoderTests;
import infra.util.MimeType;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class DataBufferDecoderTests extends AbstractDecoderTests<DataBufferDecoder> {

  private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

  private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

  DataBufferDecoderTests() {
    super(new DataBufferDecoder());
  }

  @Override
  @Test
  public void canDecode() {
    assertThat(this.decoder.canDecode(ResolvableType.forClass(DataBuffer.class),
            MimeType.TEXT_PLAIN)).isTrue();
    assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class),
            MimeType.TEXT_PLAIN)).isFalse();
    assertThat(this.decoder.canDecode(ResolvableType.forClass(DataBuffer.class),
            MimeType.APPLICATION_JSON)).isTrue();
  }

  @Override
  @Test
  public void decode() {
    Flux<DataBuffer> input = Flux.just(
            this.bufferFactory.wrap(this.fooBytes),
            this.bufferFactory.wrap(this.barBytes));

    testDecodeAll(input, DataBuffer.class, step -> step
            .consumeNextWith(expectDataBuffer(this.fooBytes))
            .consumeNextWith(expectDataBuffer(this.barBytes))
            .verifyComplete());
  }

  @Override
  @Test
  public void decodeToMono() throws Exception {
    Flux<DataBuffer> input = Flux.concat(
            dataBuffer(this.fooBytes),
            dataBuffer(this.barBytes));

    byte[] expected = new byte[this.fooBytes.length + this.barBytes.length];
    System.arraycopy(this.fooBytes, 0, expected, 0, this.fooBytes.length);
    System.arraycopy(this.barBytes, 0, expected, this.fooBytes.length, this.barBytes.length);

    testDecodeToMonoAll(input, DataBuffer.class, step -> step
            .consumeNextWith(expectDataBuffer(expected))
            .verifyComplete());
  }

  private Consumer<DataBuffer> expectDataBuffer(byte[] expected) {
    return actual -> {
      byte[] actualBytes = new byte[actual.readableBytes()];
      actual.read(actualBytes);
      assertThat(actualBytes).isEqualTo(expected);

      actual.release();
    };
  }

}
