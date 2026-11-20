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
import java.time.Duration;

import infra.http.HttpMethod;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class HttpHandlerConnectorTests {

  private static final URI URI_TO_USE = URI.create("https://example.test/resource");

  @Test
  void errorsBeforeResponseCompletionKeepTheOriginalFailure() {
    var failure = new IllegalStateException("handler failed");
    var connector = new HttpHandlerConnector((request, response) -> Mono.error(failure));
    assertThatIllegalStateException().isThrownBy(() -> connector.connect(HttpMethod.GET, URI_TO_USE,
            request -> request.setComplete()).block(Duration.ofSeconds(5))).isSameAs(failure);
    var unused = new HttpHandlerConnector((request, response) -> Mono.empty());
    assertThatIllegalStateException().isThrownBy(() -> unused.connect(HttpMethod.GET, URI_TO_USE,
            request -> Mono.error(failure)).block(Duration.ofSeconds(5))).isSameAs(failure);
  }

  @Test
  void errorsAfterResponseCompletionExposeTheCompletedResponse() {
    var failure = new IllegalStateException("after completion");
    var connector = new HttpHandlerConnector((request, response) -> {
      response.getHeaders().set("X-Completed", "yes");
      return response.setComplete().then(Mono.error(failure));
    });
    assertThatExceptionOfType(HttpHandlerConnector.FailureAfterResponseCompletedException.class)
            .isThrownBy(() -> connector.connect(HttpMethod.GET, URI_TO_USE,
                    request -> request.setComplete()).block(Duration.ofSeconds(5)))
            .withCause(failure).satisfies(error -> {
              assertThat(error.getCompletedResponse().getStatusCode().value()).isEqualTo(200);
              assertThat(error.getCompletedResponse().getHeaders().getFirst("X-Completed")).isEqualTo("yes");
            });
  }

  @Test
  void anUncommittedResponseDefaultsToOkAndHasNoBody() {
    var connector = new HttpHandlerConnector((request, response) -> Mono.empty());
    var result = connector.connect(HttpMethod.GET, URI_TO_USE, request -> request.setComplete())
            .block(Duration.ofSeconds(5));
    assertThat(result.getStatusCode().value()).isEqualTo(200);
    assertThat(result.getBody().collectList().block()).isEmpty();
  }

}
