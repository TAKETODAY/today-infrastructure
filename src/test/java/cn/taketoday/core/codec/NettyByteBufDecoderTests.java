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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vladislav Kisel
 */
class NettyByteBufDecoderTests extends AbstractDecoderTests<NettyByteBufDecoder> {

  private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

  private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

  NettyByteBufDecoderTests() {
    super(new NettyByteBufDecoder());
  }

  @Override
  @Test
  public void canDecode() {
    assertThat(this.decoder.canDecode(ResolvableType.fromClass(ByteBuf.class),
            MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.decoder.canDecode(ResolvableType.fromClass(Integer.class),
            MimeTypeUtils.TEXT_PLAIN)).isFalse();
    assertThat(this.decoder.canDecode(ResolvableType.fromClass(ByteBuf.class),
            MimeTypeUtils.APPLICATION_JSON)).isTrue();
  }

  @Override
  @Test
  public void decode() {
    Flux<DataBuffer> input = Flux.concat(
            dataBuffer(this.fooBytes),
            dataBuffer(this.barBytes));

    testDecodeAll(input, ByteBuf.class, step -> step
            .consumeNextWith(expectByteBuffer(Unpooled.copiedBuffer(this.fooBytes)))
            .consumeNextWith(expectByteBuffer(Unpooled.copiedBuffer(this.barBytes)))
            .verifyComplete());
  }

  @Override
  @Test
  public void decodeToMono() {
    Flux<DataBuffer> input = Flux.concat(
            dataBuffer(this.fooBytes),
            dataBuffer(this.barBytes));

    ByteBuf expected = Unpooled.buffer(this.fooBytes.length + this.barBytes.length)
            .writeBytes(this.fooBytes)
            .writeBytes(this.barBytes)
            .readerIndex(0);

    testDecodeToMonoAll(input, ByteBuf.class, step -> step
            .consumeNextWith(expectByteBuffer(expected))
            .verifyComplete());
  }

  private Consumer<ByteBuf> expectByteBuffer(ByteBuf expected) {
    return actual -> assertThat(actual).isEqualTo(expected);
  }

}
