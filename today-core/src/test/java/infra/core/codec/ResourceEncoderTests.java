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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.ByteArrayResource;
import infra.core.io.InputStreamResource;
import infra.core.io.Resource;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.codec.AbstractEncoderTests;
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
