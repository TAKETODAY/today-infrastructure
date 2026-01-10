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

import infra.core.ResolvableType;
import infra.core.testfixture.codec.AbstractEncoderTests;
import infra.util.MimeType;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class ByteArrayEncoderTests extends AbstractEncoderTests<ByteArrayEncoder> {

  private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

  private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

  ByteArrayEncoderTests() {
    super(new ByteArrayEncoder());
  }

  @Override
  @Test
  public void canEncode() {
    assertThat(this.encoder.canEncode(ResolvableType.forClass(byte[].class), MimeType.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Integer.class), MimeType.TEXT_PLAIN)).isFalse();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(byte[].class), MimeType.APPLICATION_JSON)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
  }

  @Override
  @Test
  public void encode() {
    Flux<byte[]> input = Flux.just(this.fooBytes, this.barBytes);

    testEncodeAll(input, byte[].class, step -> step
            .consumeNextWith(expectBytes(this.fooBytes))
            .consumeNextWith(expectBytes(this.barBytes))
            .verifyComplete());
  }

}
