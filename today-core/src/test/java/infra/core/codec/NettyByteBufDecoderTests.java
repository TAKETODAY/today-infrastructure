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

package infra.core.codec;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.codec.AbstractDecoderTests;
import infra.util.MimeType;
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
    assertThat(this.decoder.canDecode(ResolvableType.forClass(ByteBuf.class), MimeType.TEXT_PLAIN)).isTrue();
    assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class), MimeType.TEXT_PLAIN)).isFalse();
    assertThat(this.decoder.canDecode(ResolvableType.forClass(ByteBuf.class), MimeType.APPLICATION_JSON)).isTrue();
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
