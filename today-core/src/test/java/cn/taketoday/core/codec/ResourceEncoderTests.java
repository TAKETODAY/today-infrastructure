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
import org.reactivestreams.Publisher;

import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.InputStreamResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
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
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(InputStreamResource.class),
            MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(ByteArrayResource.class),
            MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(Resource.class),
            MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(InputStreamResource.class),
            MimeTypeUtils.APPLICATION_JSON)).isTrue();

    // SPR-15464
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
