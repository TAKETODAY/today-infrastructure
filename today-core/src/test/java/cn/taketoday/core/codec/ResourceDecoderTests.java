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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.InputStreamResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.StreamUtils;
import reactor.core.publisher.Flux;

import static cn.taketoday.core.ResolvableType.forClass;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class ResourceDecoderTests extends AbstractDecoderTests<ResourceDecoder> {

  private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

  private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

  ResourceDecoderTests() {
    super(new ResourceDecoder());
  }

  @Override
  @Test
  public void canDecode() {
    assertThat(this.decoder.canDecode(forClass(InputStreamResource.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.decoder.canDecode(forClass(ByteArrayResource.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.decoder.canDecode(forClass(Resource.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.decoder.canDecode(forClass(InputStreamResource.class), MimeTypeUtils.APPLICATION_JSON)).isTrue();
    assertThat(this.decoder.canDecode(forClass(Object.class), MimeTypeUtils.APPLICATION_JSON)).isFalse();
  }

  @Override
  @Test
  public void decode() {
    Flux<DataBuffer> input = Flux.concat(dataBuffer(this.fooBytes), dataBuffer(this.barBytes));

    testDecodeAll(input, Resource.class, step -> step
            .consumeNextWith(resource -> {
              try {
                byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
                assertThat(new String(bytes)).isEqualTo("foobar");
              }
              catch (IOException ex) {
                throw new AssertionError(ex.getMessage(), ex);
              }
            })
            .expectComplete()
            .verify());
  }

  @Override
  @Test
  public void decodeToMono() {
    Flux<DataBuffer> input = Flux.concat(dataBuffer(this.fooBytes), dataBuffer(this.barBytes));
    testDecodeToMonoAll(
            input, ResolvableType.forClass(Resource.class), step -> step.consumeNextWith(value -> {
                      Resource resource = (Resource) value;
                      try {
                        byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
                        assertThat(new String(bytes)).isEqualTo("foobar");
                        assertThat(resource.getName()).isEqualTo("testFile");
                      }
                      catch (IOException ex) {
                        throw new AssertionError(ex.getMessage(), ex);
                      }
                    })
                    .expectComplete()
                    .verify(),
            null,
            Collections.singletonMap(ResourceDecoder.FILENAME_HINT, "testFile"));
  }

  @Test
  public void decodeInputStreamResource() {
    Flux<DataBuffer> input = Flux.concat(dataBuffer(this.fooBytes), dataBuffer(this.barBytes));
    testDecodeAll(input, InputStreamResource.class, step -> step
            .consumeNextWith(resource -> {
              try {
                byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
                assertThat(new String(bytes)).isEqualTo("foobar");
                assertThat(resource.contentLength()).isEqualTo(fooBytes.length + barBytes.length);
              }
              catch (IOException ex) {
                throw new AssertionError(ex.getMessage(), ex);
              }
            })
            .expectComplete()
            .verify());
  }

}
