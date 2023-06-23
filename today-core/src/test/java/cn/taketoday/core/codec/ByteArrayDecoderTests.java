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

package cn.taketoday.core.codec;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class ByteArrayDecoderTests extends AbstractDecoderTests<ByteArrayDecoder> {

  private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

  private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

  ByteArrayDecoderTests() {
    super(new ByteArrayDecoder());
  }

  @Override
  @Test
  public void canDecode() {
    assertThat(this.decoder.canDecode(
            ResolvableType.forClass(byte[].class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.decoder.canDecode(
            ResolvableType.forClass(Integer.class), MimeTypeUtils.TEXT_PLAIN)).isFalse();
    assertThat(this.decoder.canDecode(
            ResolvableType.forClass(byte[].class), MimeTypeUtils.APPLICATION_JSON)).isTrue();
  }

  @Override
  @Test
  public void decode() {
    Flux<DataBuffer> input = Flux.concat(
            dataBuffer(this.fooBytes),
            dataBuffer(this.barBytes));

    testDecodeAll(input, byte[].class, step -> step
            .consumeNextWith(expectBytes(this.fooBytes))
            .consumeNextWith(expectBytes(this.barBytes))
            .verifyComplete());

  }

  @Override
  @Test
  public void decodeToMono() {
    Flux<DataBuffer> input = Flux.concat(
            dataBuffer(this.fooBytes),
            dataBuffer(this.barBytes));

    byte[] expected = new byte[this.fooBytes.length + this.barBytes.length];
    System.arraycopy(this.fooBytes, 0, expected, 0, this.fooBytes.length);
    System.arraycopy(this.barBytes, 0, expected, this.fooBytes.length, this.barBytes.length);

    testDecodeToMonoAll(input, byte[].class, step -> step
            .consumeNextWith(expectBytes(expected))
            .verifyComplete());
  }

  private Consumer<byte[]> expectBytes(byte[] expected) {
    return bytes -> assertThat(bytes).isEqualTo(expected);
  }

}
