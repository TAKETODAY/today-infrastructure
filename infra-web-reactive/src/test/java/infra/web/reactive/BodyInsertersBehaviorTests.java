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

package infra.web.reactive;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.ByteArrayResource;
import infra.http.codec.HttpMessageWriter;
import infra.http.reactive.ReactiveHttpOutputMessage;
import infra.http.reactive.server.ServerHttpRequest;
import infra.web.UnsupportedMediaTypeException;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BodyInsertersBehaviorTests {

  private static final Executor DIRECT_EXECUTOR = Runnable::run;

  @Test
  void writesOutputStreamContent() {
    BodyInserter<? extends Publisher<DataBuffer>, ReactiveHttpOutputMessage> inserter =
            BodyInserters.fromOutputStream(outputStream -> write(outputStream, "stream"), DIRECT_EXECUTOR, 2);
    MockServerHttpResponse response = new MockServerHttpResponse();

    inserter.insert(response, emptyContext()).block();

    assertThat(response.getBodyAsString().block()).isEqualTo("stream");
  }

  @Test
  void writesOutputStreamContentWithDefaultChunkSize() {
    var inserter = BodyInserters.fromOutputStream(outputStream -> write(outputStream, "default buffer"), DIRECT_EXECUTOR);
    var response = new MockServerHttpResponse();

    inserter.insert(response, emptyContext()).block();

    assertThat(response.getBodyAsString().block()).isEqualTo("default buffer");
  }

  @Test
  void rejectsInvalidOutputStreamArguments() {
    assertThatThrownBy(() -> BodyInserters.fromOutputStream(null, DIRECT_EXECUTOR))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BodyInserters.fromOutputStream(outputStream -> { }, null))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BodyInserters.fromOutputStream(outputStream -> { }, DIRECT_EXECUTOR, 0))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reportsUnsupportedMediaTypeWhenNoWriterMatches() {
    BodyInserter<String, ReactiveHttpOutputMessage> inserter = BodyInserters.fromValue("value");
    MockServerHttpResponse response = new MockServerHttpResponse();

    assertThatThrownBy(() -> inserter.insert(response, emptyContext()).block())
            .isInstanceOf(UnsupportedMediaTypeException.class);
  }

  @Test
  void rejectsReactiveValuePassedAsPlainValue() {
    assertThatThrownBy(() -> BodyInserters.fromValue(Mono.just("value")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("reactive types");
    assertThatThrownBy(() -> BodyInserters.fromProducer("value", String.class))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unknown to ReactiveAdapterRegistry");
  }

  @Test
  void resourceInserterRequiresResourceWriter() {
    var inserter = BodyInserters.fromResource(new ByteArrayResource(new byte[0]));

    assertThatThrownBy(() -> inserter.insert(new MockServerHttpResponse(), emptyContext()))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("No HttpMessageWriter", "Resource");
  }

  private BodyInserter.Context emptyContext() {
    return new BodyInserter.Context() {
      @Override
      public List<HttpMessageWriter<?>> messageWriters() {
        return List.of();
      }

      @Override
      public Optional<ServerHttpRequest> serverRequest() {
        return Optional.empty();
      }

      @Override
      public Map<String, Object> hints() {
        return Map.of();
      }
    };
  }

  private static void write(OutputStream outputStream, String value) {
    try {
      outputStream.write(value.getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

}
