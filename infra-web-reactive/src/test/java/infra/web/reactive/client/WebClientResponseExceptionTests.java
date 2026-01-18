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

package infra.web.reactive.client;

import org.junit.jupiter.api.Test;

import infra.http.HttpStatusCode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/7 11:57
 */
class WebClientResponseExceptionTests {

  @Test
  void constructWithSuccessStatusCodeAndNoCauseAdditionalMessage() {
    assertThat(new WebClientResponseException(HttpStatusCode.valueOf(200), "OK", null, null, null, null))
            .hasNoCause()
            .hasMessage("200 OK, but response failed with cause: null");
  }

  @Test
  void constructWith1xxStatusCodeAndCauseAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException(HttpStatusCode.valueOf(100), "reasonPhrase", null, null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("100 reasonPhrase, but response failed with cause: java.lang.RuntimeException: example cause");
  }

  @Test
  void constructWith2xxStatusCodeAndCauseAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException(HttpStatusCode.valueOf(200), "reasonPhrase", null, null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("200 reasonPhrase, but response failed with cause: java.lang.RuntimeException: example cause");
  }

  @Test
  void constructWith3xxStatusCodeAndCauseAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException(HttpStatusCode.valueOf(300), "reasonPhrase", null, null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("300 reasonPhrase, but response failed with cause: java.lang.RuntimeException: example cause");
  }

  @Test
  void constructWithExplicitMessageAndNotErrorCodeAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException("explicit message", HttpStatusCode.valueOf(100), "reasonPhrase", null, null, null, null);
    assertThat(ex).hasMessage("explicit message, but response failed with cause: null");
  }

  @Test
  void constructWithExplicitMessageAndNotErrorCodeAndCauseAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException("explicit message", HttpStatusCode.valueOf(100), "reasonPhrase", null, null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("explicit message, but response failed with cause: java.lang.RuntimeException: example cause")
            .hasRootCauseMessage("example cause");
  }

  @Test
  void constructWithExplicitMessageAndErrorCodeAndCauseNoAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException("explicit message", HttpStatusCode.valueOf(404), "reasonPhrase", null, null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("explicit message").hasRootCauseMessage("example cause");
  }

  @Test
  void constructWith4xxStatusCodeAndCauseNoAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException(HttpStatusCode.valueOf(400), "reasonPhrase", null, null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("400 reasonPhrase").hasRootCauseMessage("example cause");
  }

  @Test
  void constructWith5xxStatusCodeAndCauseNoAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException(HttpStatusCode.valueOf(500), "reasonPhrase", null, null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("500 reasonPhrase").hasRootCauseMessage("example cause");
  }

}