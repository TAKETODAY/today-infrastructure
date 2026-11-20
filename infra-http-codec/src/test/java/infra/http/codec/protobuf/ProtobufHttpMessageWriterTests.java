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

package infra.http.codec.protobuf;

import com.google.protobuf.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.EncodingException;
import infra.core.io.buffer.DataBufferUtils;
import infra.http.MediaType;
import infra.http.protobuf.Msg;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class ProtobufHttpMessageWriterTests {

  private final ProtobufHttpMessageWriter writer = new ProtobufHttpMessageWriter();

  private final MockServerHttpResponse response = new MockServerHttpResponse();

  @Test
  void writesSchemaHeadersAndAnUndelimitedSingleMessage() {
    Msg value = Msg.newBuilder().setFoo("single").build();
    write(Mono.just(value), null);

    assertThat(this.response.getHeaders().getFirst("X-Protobuf-Schema")).isEqualTo(Msg.getDescriptor().getFile().getName());
    assertThat(this.response.getHeaders().getFirst("X-Protobuf-Message")).isEqualTo(Msg.getDescriptor().getFullName());
    assertThat(this.response.getHeaders().getContentType().getParameter("delimited")).isNull();
    StepVerifier.create(this.response.getBody())
            .assertNext(buffer -> {
              try (var input = buffer.asInputStream(true)) {
                assertThat(Msg.parseFrom(input)).isEqualTo(value);
              }
              catch (IOException ex) {
                throw new AssertionError(ex);
              }
            }).verifyComplete();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = { "application/x-protobuf;version=1", "application/x-protobuf;delimited=true" })
  void writesDelimitedMessagesAndRetainsExplicitMediaTypeParameters(String contentType) {
    Msg first = Msg.newBuilder().setFoo("first").build();
    Msg second = Msg.newBuilder().setFoo("second").build();
    write(Flux.just(first, second), contentType == null ? null : MediaType.parseMediaType(contentType));

    MediaType actual = this.response.getHeaders().getContentType();
    assertThat(actual.getParameter("delimited")).isEqualTo("true");
    if (contentType != null && contentType.contains("version=")) {
      assertThat(actual.getParameter("version")).isEqualTo("1");
    }
    StepVerifier.create(DataBufferUtils.join(this.response.getBody()))
            .assertNext(buffer -> {
              try (var input = buffer.asInputStream(true)) {
                assertThat(Msg.parseDelimitedFrom(input)).isEqualTo(first);
                assertThat(Msg.parseDelimitedFrom(input)).isEqualTo(second);
                assertThat(Msg.parseDelimitedFrom(input)).isNull();
              }
              catch (IOException ex) {
                throw new AssertionError(ex);
              }
            }).verifyComplete();
  }

  @Test
  void reportsAnEncodingErrorForATypeWithoutAProtobufBuilder() {
    StepVerifier.create(this.writer.write(Mono.empty(), ResolvableType.forClass(String.class), null, this.response, Map.of()))
            .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(EncodingException.class)
                    .hasMessageContaining("Could not write Protobuf message").hasCauseInstanceOf(NoSuchMethodException.class))
            .verify();
  }

  private void write(Publisher<? extends Message> input, MediaType mediaType) {
    StepVerifier.create(this.writer.write(input, ResolvableType.forClass(Msg.class), mediaType, this.response, Map.of()))
            .verifyComplete();
  }
}
