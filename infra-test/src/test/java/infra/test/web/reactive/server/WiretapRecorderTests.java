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

package infra.test.web.reactive.server;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.core.io.buffer.DataBuffer;
import infra.http.HttpMethod;
import infra.mock.http.client.reactive.MockClientHttpRequest;
import infra.mock.http.client.reactive.MockClientHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class WiretapRecorderTests {

  @Test
  void validatesPublishersAndEmptyRecordings() {
    assertThatIllegalArgumentException().isThrownBy(() -> new WiretapConnector.WiretapRecorder(Flux.empty(), Flux.empty()));
    var recorder = new WiretapConnector.WiretapRecorder(null, null);
    assertThat(recorder.getContent().block()).isNull();
    assertThatIllegalArgumentException().isThrownBy(recorder::getPublisherToUse);
    assertThatIllegalArgumentException().isThrownBy(recorder::getNestedPublisherToUse);
  }

  @Test
  void recordingCapturesNormalNestedAndCancelledContent() {
    var buffer = DefaultDataBufferFactory.sharedInstance;
    var recorder = new WiretapConnector.WiretapRecorder(Flux.just(buffer.wrap("hello".getBytes(StandardCharsets.UTF_8))), null);
    assertThat(new String(recorder.getContent().block(), StandardCharsets.UTF_8)).isEqualTo("hello");
    assertThat(new String(recorder.getContent().block(), StandardCharsets.UTF_8)).isEqualTo("hello");
    var source = Sinks.many().unicast().<Flux<DataBuffer>>onBackpressureBuffer();
    var nested = new WiretapConnector.WiretapRecorder(null, source.asFlux());
    Flux.from(nested.getNestedPublisherToUse()).concatMap(Flux::from).subscribe();
    source.tryEmitNext(Flux.just(buffer.wrap("nested".getBytes(StandardCharsets.UTF_8))));
    source.tryEmitComplete();
    assertThat(new String(nested.getContent().block(), StandardCharsets.UTF_8)).isEqualTo("nested");
    var cancelled = new WiretapConnector.WiretapRecorder(Flux.never(), null);
    var subscription = Flux.from(cancelled.getPublisherToUse()).subscribe();
    subscription.dispose();
    assertThat(cancelled.getContent().block()).isEmpty();
  }

  @Test
  void errorsFromConsumedPublishersRemainAvailableInTheRecording() {
    var failure = new IllegalStateException("body");
    var recorder = new WiretapConnector.WiretapRecorder(Flux.error(failure), null);
    assertThatIllegalStateException().isThrownBy(() -> Flux.from(recorder.getPublisherToUse()).blockLast()).isSameAs(failure);
    assertThatIllegalStateException().isThrownBy(() -> recorder.getContent().block()).isSameAs(failure);
    var nested = new WiretapConnector.WiretapRecorder(null, Flux.error(failure));
    assertThatIllegalStateException().isThrownBy(() -> Flux.from(nested.getNestedPublisherToUse()).blockLast()).isSameAs(failure);
    assertThatIllegalStateException().isThrownBy(() -> nested.getContent().block()).isSameAs(failure);
  }

  @Test
  void connectorRequiresRequestIdsAndRejectsMissingExchanges() {
    var connector = new WiretapConnector((method, uri, callback) ->
            callback.apply(new MockClientHttpRequest(method, uri)).thenReturn(new MockClientHttpResponse(200)));
    assertThatIllegalStateException().isThrownBy(() -> connector.connect(HttpMethod.GET, URI.create("/"), request -> request.setComplete()).block())
            .withMessageContaining("header");
    assertThatIllegalStateException().isThrownBy(() -> connector.getExchangeResult("missing", null, Duration.ofSeconds(1)))
            .withMessageContaining("No match");
  }

  @Test
  void nestedRequestBodiesAreRecordedThroughTheConnector() {
    var connector = new WiretapConnector(new HttpHandlerConnector((request, response) ->
            request.getBody().then(response.setComplete())));
    connector.connect(HttpMethod.POST, URI.create("https://example.test/"), request -> {
      request.getHeaders().set(WebTestClient.WEBTESTCLIENT_REQUEST_ID, "nested");
      return request.writeAndFlushWith(Flux.just(Flux.just(DefaultDataBufferFactory.sharedInstance
              .wrap("body".getBytes(StandardCharsets.UTF_8)))));
    }).block(Duration.ofSeconds(5));
    var result = connector.getExchangeResult("nested", null, Duration.ofSeconds(5));
    assertThat(result.getRequestBodyContent()).isEqualTo("body".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void contentCanBeAwaitedWhileAnExistingConsumerIsStillReading() {
    var source = Sinks.many().unicast().<DataBuffer>onBackpressureBuffer();
    var recorder = new WiretapConnector.WiretapRecorder(source.asFlux(), null);
    Flux.from(recorder.getPublisherToUse()).subscribe();
    var content = recorder.getContent().toFuture();
    assertThat(content).isNotDone();
    source.tryEmitComplete();
    assertThat(content.join()).isEmpty();
    var nested = new WiretapConnector.WiretapRecorder(null, Flux.empty());
    assertThat(nested.getContent().block()).isEmpty();
  }

}
