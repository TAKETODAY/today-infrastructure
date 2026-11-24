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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import infra.core.io.buffer.DataBuffer;
import infra.http.HttpStatus;
import infra.http.codec.HttpMessageReader;
import infra.http.reactive.ReactiveHttpInputMessage;
import infra.http.reactive.server.ServerHttpResponse;
import infra.web.UnsupportedMediaTypeException;
import infra.web.testfixture.http.client.reactive.MockClientHttpResponse;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static org.assertj.core.api.Assertions.assertThat;

class BodyExtractorsBehaviorTests {

  @Test
  void unsupportedMonoWithoutContentTypeReleasesBodyAndSignalsError() {
    TestPublisher<DataBuffer> publisher = TestPublisher.create();
    MockServerHttpRequest request = MockServerHttpRequest.post("/").body(publisher.flux());
    BodyExtractor.Context context = emptyContext();

    Mono<String> result = BodyExtractors.toMono(String.class).extract(request, context);
    StepVerifier.create(result)
            .then(() -> publisher.emit(new infra.core.io.buffer.DefaultDataBufferFactory().wrap("body".getBytes())))
            .expectError(UnsupportedMediaTypeException.class)
            .verify();
    publisher.assertWasSubscribed();
  }

  @Test
  void unsupportedClientFluxConsumesAndCancelsWhenContentTypeIsPresent() {
    TestPublisher<DataBuffer> publisher = TestPublisher.create();
    MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
    response.getHeaders().setContentType(infra.http.MediaType.APPLICATION_JSON);
    response.setBody(publisher.flux());

    Flux<String> result = BodyExtractors.toFlux(String.class).extract(response, emptyContext());
    StepVerifier.create(result)
            .then(() -> publisher.emit(new infra.core.io.buffer.DefaultDataBufferFactory().wrap("body".getBytes())))
            .expectError(UnsupportedMediaTypeException.class)
            .verify();
    publisher.assertCancelled();
  }

  @Test
  void returnsRawDataBuffersWithoutUsingReaders() {
    DataBuffer buffer = new infra.core.io.buffer.DefaultDataBufferFactory().wrap("body".getBytes());
    MockServerHttpRequest request = MockServerHttpRequest.post("/").body(Flux.just(buffer));

    assertThat(BodyExtractors.toDataBuffers().extract(request, emptyContext()).blockFirst()).isSameAs(buffer);
  }

  private BodyExtractor.Context emptyContext() {
    return new BodyExtractor.Context() {
      @Override
      public List<HttpMessageReader<?>> messageReaders() {
        return List.of();
      }

      @Override
      public Optional<ServerHttpResponse> serverResponse() {
        return Optional.empty();
      }

      @Override
      public Map<String, Object> hints() {
        return Map.of();
      }
    };
  }

}
