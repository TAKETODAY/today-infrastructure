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
import org.reactivestreams.Publisher;

import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.ByteArrayResource;
import infra.core.io.InputStreamResource;
import infra.core.io.Resource;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.codec.AbstractEncoderTests;
import infra.lang.Nullable;
import infra.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class ResourceEncoderTests extends AbstractEncoderTests<ResourceEncoder> {

  private final byte[] bytes = "foo".getBytes(UTF_8);

  ResourceEncoderTests() {
    super(new ResourceEncoder());
  }

  @Override
  @Test
  public void canEncode() {
    assertThat(this.encoder.canEncode(ResolvableType.forClass(InputStreamResource.class), MimeType.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(ByteArrayResource.class), MimeType.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Resource.class), MimeType.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(InputStreamResource.class), MimeType.APPLICATION_JSON)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
  }

  @Override
  @Test
  public void encode() {
    Flux<Resource> input = Flux.just(new ByteArrayResource(this.bytes));

    testEncodeAll(input, Resource.class, step -> step
            .consumeNextWith(expectBytes(this.bytes))
            .verifyComplete());
  }

  @Override
  protected void testEncodeError(Publisher<?> input, ResolvableType outputType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    Flux<Resource> i = Flux.error(new InputException());

    Flux<DataBuffer> result = ((Encoder<Resource>) this.encoder).encode(
            i, this.bufferFactory, outputType, mimeType, hints);

    StepVerifier.create(result)
            .expectError(InputException.class)
            .verify();
  }

}
