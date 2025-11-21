/*
 * Copyright 2017 - 2025 the original author or authors.
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

import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.util.MimeType;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/20 21:58
 */
class EncoderTests {

  @Test
  void canEncodeWithNullMimeType() {
    Encoder<String> encoder = new TestEncoder();
    ResolvableType elementType = ResolvableType.forClass(String.class);
    boolean result = encoder.canEncode(elementType, null);
    assertThat(result).isTrue();
  }

  @Test
  void getEncodableMimeTypesReturnsSupportedTypes() {
    Encoder<String> encoder = new TestEncoder();
    List<MimeType> mimeTypes = encoder.getEncodableMimeTypes();
    assertThat(mimeTypes).containsExactly(new MimeType("application", "json"));
  }

  @Test
  void getEncodableMimeTypesForElementTypeReturnsSupportedTypesWhenCanEncode() {
    Encoder<String> encoder = new TestEncoder();
    ResolvableType elementType = ResolvableType.forClass(String.class);
    List<MimeType> mimeTypes = encoder.getEncodableMimeTypes(elementType);
    assertThat(mimeTypes).containsExactly(new MimeType("application", "json"));
  }

  @Test
  void encodeValueThrowsUnsupportedOperationExceptionByDefault() {
    Encoder<String> encoder = new TestEncoder();
    DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
    ResolvableType valueType = ResolvableType.forClass(String.class);

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> encoder.encodeValue("test", bufferFactory, valueType, null, null));
  }

  @Test
  void encodeMethodCanBeCalledWithValidParameters() {
    Encoder<String> encoder = new TestEncoder();
    Publisher<String> inputStream = Flux.just("test");
    DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
    ResolvableType elementType = ResolvableType.forClass(String.class);

    Flux<DataBuffer> result = encoder.encode(inputStream, bufferFactory, elementType, null, null);
    assertThat(result).isNotNull();
  }

  static class TestEncoder implements Encoder<String> {

    @Override
    public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
      return elementType.resolve() == String.class;
    }

    @Override
    public Flux<DataBuffer> encode(Publisher<? extends String> inputStream,
            DataBufferFactory bufferFactory, ResolvableType elementType,
            MimeType mimeType, Map<String, Object> hints) {
      return Flux.from(inputStream)
              .map(s -> {
                byte[] bytes = s.getBytes();
                DataBuffer buffer = bufferFactory.allocateBuffer(bytes.length);
                buffer.write(bytes);
                return buffer;
              });
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
      return List.of(new MimeType("application", "json"));
    }
  }

}