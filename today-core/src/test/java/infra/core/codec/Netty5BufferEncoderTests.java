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

import infra.core.ResolvableType;
import infra.core.testfixture.codec.AbstractEncoderTests;
import infra.util.MimeType;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.DefaultBufferAllocators;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class Netty5BufferEncoderTests extends AbstractEncoderTests<Netty5BufferEncoder> {

  private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

  private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

  Netty5BufferEncoderTests() {
    super(new Netty5BufferEncoder());
  }

  @Test
  @Override
  public void canEncode() {
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Buffer.class), MimeType.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Integer.class), MimeType.TEXT_PLAIN)).isFalse();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Buffer.class), MimeType.APPLICATION_JSON)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
  }

  @Test
  @Override
  @SuppressWarnings("resource")
  public void encode() {
    Flux<Buffer> input = Flux.just(this.fooBytes, this.barBytes)
            .map(DefaultBufferAllocators.preferredAllocator()::copyOf);

    testEncodeAll(input, Buffer.class, step -> step
            .consumeNextWith(expectBytes(this.fooBytes))
            .consumeNextWith(expectBytes(this.barBytes))
            .verifyComplete());
  }

}
