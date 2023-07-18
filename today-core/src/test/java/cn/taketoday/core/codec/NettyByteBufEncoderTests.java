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

package cn.taketoday.core.codec;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.testfixture.codec.AbstractEncoderTests;
import cn.taketoday.util.MimeTypeUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vladislav Kisel
 */
class NettyByteBufEncoderTests extends AbstractEncoderTests<NettyByteBufEncoder> {

  private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

  private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

  NettyByteBufEncoderTests() {
    super(new NettyByteBufEncoder());
  }

  @Override
  @Test
  public void canEncode() {
    assertThat(this.encoder.canEncode(ResolvableType.forClass(ByteBuf.class),
            MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Integer.class),
            MimeTypeUtils.TEXT_PLAIN)).isFalse();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(ByteBuf.class),
            MimeTypeUtils.APPLICATION_JSON)).isTrue();

    // gh-20024
    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
  }

  @Override
  @Test
  public void encode() {
    Flux<ByteBuf> input = Flux.just(this.fooBytes, this.barBytes).map(Unpooled::copiedBuffer);

    testEncodeAll(input, ByteBuf.class, step -> step
            .consumeNextWith(expectBytes(this.fooBytes))
            .consumeNextWith(expectBytes(this.barBytes))
            .verifyComplete());
  }
}
