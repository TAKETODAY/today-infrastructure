/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
import io.netty5.buffer.api.Buffer;
import io.netty5.buffer.api.DefaultBufferAllocators;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class Netty5BufferDecoderTests extends AbstractDecoderTests<Netty5BufferDecoder> {

  private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

  private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

  Netty5BufferDecoderTests() {
    super(new Netty5BufferDecoder());
  }

  @Override
  @Test
  public void canDecode() {
    assertThat(this.decoder.canDecode(ResolvableType.fromClass(Buffer.class),
            MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.decoder.canDecode(ResolvableType.fromClass(Integer.class),
            MimeTypeUtils.TEXT_PLAIN)).isFalse();
    assertThat(this.decoder.canDecode(ResolvableType.fromClass(Buffer.class),
            MimeTypeUtils.APPLICATION_JSON)).isTrue();
  }

  @Override
  @Test
  public void decode() {
    Flux<DataBuffer> input = Flux.concat(
            dataBuffer(this.fooBytes),
            dataBuffer(this.barBytes));

    testDecodeAll(input, Buffer.class, step -> step
            .consumeNextWith(expectByteBuffer(DefaultBufferAllocators.preferredAllocator().copyOf(this.fooBytes)))
            .consumeNextWith(expectByteBuffer(DefaultBufferAllocators.preferredAllocator().copyOf(this.barBytes)))
            .verifyComplete());
  }

  @Override
  @Test
  public void decodeToMono() {
    Flux<DataBuffer> input = Flux.concat(
            dataBuffer(this.fooBytes),
            dataBuffer(this.barBytes));

    Buffer expected = DefaultBufferAllocators.preferredAllocator().allocate(this.fooBytes.length + this.barBytes.length)
            .writeBytes(this.fooBytes)
            .writeBytes(this.barBytes)
            .readerOffset(0);

    testDecodeToMonoAll(input, Buffer.class, step -> step
            .consumeNextWith(expectByteBuffer(expected))
            .verifyComplete());
  }

  private Consumer<Buffer> expectByteBuffer(Buffer expected) {
    return actual -> assertThat(actual).isEqualTo(expected);
  }

}
