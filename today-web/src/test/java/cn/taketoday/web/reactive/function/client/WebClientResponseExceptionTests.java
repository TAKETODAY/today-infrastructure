/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.reactive.function.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/7 11:57
 */
class WebClientResponseExceptionTests {

  @Test
  void constructWithSuccessStatusCodeAndNoCauseAdditionalMessage() {
    assertThat(new WebClientResponseException(200, "OK", null, null, null))
            .hasNoCause()
            .hasMessage("200 OK, but response failed with cause: null");
  }

  @Test
  void constructWith1xxStatusCodeAndCauseAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException(100, "reasonPhrase", null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("100 reasonPhrase, but response failed with cause: java.lang.RuntimeException: example cause");
  }

  @Test
  void constructWith2xxStatusCodeAndCauseAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException(200, "reasonPhrase", null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("200 reasonPhrase, but response failed with cause: java.lang.RuntimeException: example cause");
  }

  @Test
  void constructWith3xxStatusCodeAndCauseAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException(300, "reasonPhrase", null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("300 reasonPhrase, but response failed with cause: java.lang.RuntimeException: example cause");
  }

  @Test
  void constructWithExplicitMessageAndNotErrorCodeAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException("explicit message", 100, "reasonPhrase", null, null, null);
    assertThat(ex).hasMessage("explicit message, but response failed with cause: null");
  }

  @Test
  void constructWithExplicitMessageAndNotErrorCodeAndCauseAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException("explicit message", 100, "reasonPhrase", null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("explicit message, but response failed with cause: java.lang.RuntimeException: example cause")
            .hasRootCauseMessage("example cause");
  }

  @Test
  void constructWithExplicitMessageAndErrorCodeAndCauseNoAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException("explicit message", 404, "reasonPhrase", null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("explicit message").hasRootCauseMessage("example cause");
  }

  @Test
  void constructWith4xxStatusCodeAndCauseNoAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException(400, "reasonPhrase", null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("400 reasonPhrase").hasRootCauseMessage("example cause");
  }

  @Test
  void constructWith5xxStatusCodeAndCauseNoAdditionalMessage() {
    WebClientResponseException ex = new WebClientResponseException(500, "reasonPhrase", null, null, null);
    ex.initCause(new RuntimeException("example cause"));
    assertThat(ex).hasMessage("500 reasonPhrase").hasRootCauseMessage("example cause");
  }

}