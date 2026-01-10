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

import java.nio.ByteBuffer;
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
class ByteBufferDecoderTests extends AbstractDecoderTests<ByteBufferDecoder> {

  private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

  private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

  ByteBufferDecoderTests() {
    super(new ByteBufferDecoder());
  }

  @Override
  @Test
  public void canDecode() {
    assertThat(this.decoder.canDecode(ResolvableType.forClass(ByteBuffer.class), MimeType.TEXT_PLAIN)).isTrue();
    assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class), MimeType.TEXT_PLAIN)).isFalse();
    assertThat(this.decoder.canDecode(ResolvableType.forClass(ByteBuffer.class), MimeType.APPLICATION_JSON)).isTrue();
  }

  @Override
  @Test
  public void decode() {
    Flux<DataBuffer> input = Flux.concat(
            dataBuffer(this.fooBytes),
            dataBuffer(this.barBytes));

    testDecodeAll(input, ByteBuffer.class, step -> step
            .consumeNextWith(expectByteBuffer(ByteBuffer.wrap(this.fooBytes)))
            .consumeNextWith(expectByteBuffer(ByteBuffer.wrap(this.barBytes)))
            .verifyComplete());

  }

  @Override
  @Test
  public void decodeToMono() {
    Flux<DataBuffer> input = Flux.concat(
            dataBuffer(this.fooBytes),
            dataBuffer(this.barBytes));
    ByteBuffer expected = ByteBuffer.allocate(this.fooBytes.length + this.barBytes.length);
    expected.put(this.fooBytes).put(this.barBytes).flip();

    testDecodeToMonoAll(input, ByteBuffer.class, step -> step
            .consumeNextWith(expectByteBuffer(expected))
            .verifyComplete());

  }

  private Consumer<ByteBuffer> expectByteBuffer(ByteBuffer expected) {
    return actual -> assertThat(actual).isEqualTo(expected);
  }

}
