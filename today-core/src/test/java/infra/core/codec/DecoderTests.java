/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.core.codec;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/20 21:59
 */
class DecoderTests {

  @Test
  void canDecodeWithNullMimeType() {
    Decoder<String> decoder = new TestDecoder();
    ResolvableType elementType = ResolvableType.forClass(String.class);
    boolean result = decoder.canDecode(elementType, null);
    assertThat(result).isTrue();
  }

  @Test
  void getDecodableMimeTypesReturnsSupportedTypes() {
    Decoder<String> decoder = new TestDecoder();
    List<MimeType> mimeTypes = decoder.getDecodableMimeTypes();
    assertThat(mimeTypes).containsExactly(new MimeType("application", "json"));
  }

  @Test
  void getDecodableMimeTypesForTargetTypeReturnsSupportedTypesWhenCanDecode() {
    Decoder<String> decoder = new TestDecoder();
    ResolvableType targetType = ResolvableType.forClass(String.class);
    List<MimeType> mimeTypes = decoder.getDecodableMimeTypes(targetType);
    assertThat(mimeTypes).containsExactly(new MimeType("application", "json"));
  }

  @Test
  void decodeToMonoMethodCanBeCalledWithValidParameters() {
    Decoder<String> decoder = new TestDecoder();
    Publisher<DataBuffer> inputStream = Mono.empty();
    ResolvableType elementType = ResolvableType.forClass(String.class);

    Mono<String> result = decoder.decodeToMono(inputStream, elementType, null, null);
    assertThat(result).isNotNull();
  }

  @Test
  void decodeMethodReturnsValueFromDecodeToMono() {
    Decoder<String> decoder = new TestDecoder();
    DataBuffer buffer = new DefaultDataBufferFactory().allocateBuffer().write("test".getBytes());
    ResolvableType targetType = ResolvableType.forClass(String.class);

    String result = decoder.decode(buffer, targetType, null, null);
    assertThat(result).isEqualTo("test");
  }

  @Test
  void decodeMethodThrowsDecodingExceptionWhenExecutionExceptionOccurs() {
    Decoder<String> decoder = new FailingTestDecoder();
    DataBuffer buffer = new DefaultDataBufferFactory().allocateBuffer();
    ResolvableType targetType = ResolvableType.forClass(String.class);

    assertThatExceptionOfType(DecodingException.class)
            .isThrownBy(() -> decoder.decode(buffer, targetType, null, null))
            .withMessageContaining("Failed to decode");
  }

  static class TestDecoder implements Decoder<String> {

    @Override
    public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
      return elementType.resolve() == String.class;
    }

    @Override
    public Flux<String> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
            MimeType mimeType, Map<String, Object> hints) {
      return Flux.from(inputStream)
              .map(buffer -> {
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.read(bytes);
                return new String(bytes);
              });
    }

    @Override
    public Mono<String> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
            MimeType mimeType, Map<String, Object> hints) {
      return Flux.from(inputStream)
              .reduce(new DefaultDataBufferFactory().allocateBuffer(), (buffer, dataBuffer) -> {
                buffer.write(dataBuffer);
                return buffer;
              })
              .map(buffer -> {
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.read(bytes);
                return new String(bytes);
              });
    }

    @Override
    public List<MimeType> getDecodableMimeTypes() {
      return List.of(new MimeType("application", "json"));
    }
  }

  static class FailingTestDecoder implements Decoder<String> {

    @Override
    public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
      return true;
    }

    @Override
    public Flux<String> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
            MimeType mimeType, Map<String, Object> hints) {
      return Flux.error(new RuntimeException("Decoding failed"));
    }

    @Override
    public Mono<String> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
            MimeType mimeType, Map<String, Object> hints) {
      return Mono.error(new RuntimeException("Decoding failed"));
    }

    @Override
    public List<MimeType> getDecodableMimeTypes() {
      return List.of();
    }
  }

}