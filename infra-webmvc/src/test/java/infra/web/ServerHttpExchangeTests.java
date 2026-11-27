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

package infra.web;

import org.junit.jupiter.api.Test;

import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.session.Session;
import infra.web.ServerHttpExchange.Lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ServerHttpExchangeTests {

  private final ServerHttpExchange exchange = mock(ServerHttpExchange.class, CALLS_REAL_METHODS);

  @Test
  void cookieHelpersPreserveNamesAndValues() {
    HttpCookie requestCookie = new HttpCookie("locale", "en");
    given(exchange.getCookie("locale")).willReturn(requestCookie);

    assertThat(exchange.cookie("locale")).containsSame(requestCookie);
    assertThat(exchange.cookie("missing")).isEmpty();

    exchange.addCookie(ResponseCookie.builder("theme", "dark").path("/settings"));
    exchange.addCookie("language", builder -> builder.value("en").httpOnly(true));
    exchange.addCookie(requestCookie);

    verify(exchange).addCookie(ResponseCookie.builder("theme", "dark").path("/settings").build());
    verify(exchange).addCookie(ResponseCookie.builder("language", "en").httpOnly(true).build());
    verify(exchange).addCookie("locale", "en");
  }

  @Test
  void lifecycleHelpersRegisterCallbacksWithGeneratedNamesAndCorrectPhases() {
    Runnable callback = () -> { };
    String completed = exchange.registerCompletedCallback(callback);
    String committing = exchange.registerCommittingCallback(callback);
    String committed = exchange.registerCommittedCallback(callback);

    assertThat(new String[] { completed, committing, committed }).doesNotHaveDuplicates();
    verify(exchange).registerCallback(Lifecycle.COMPLETED, completed, callback);
    verify(exchange).registerCallback(Lifecycle.COMMITTING, committing, callback);
    verify(exchange).registerCallback(Lifecycle.COMMITTED, committed, callback);
  }

  @Test
  void conditionalRequestHelpersPassOnlyTheSuppliedPrecondition() {
    given(exchange.checkNotModified(null, 1234L)).willReturn(true);
    given(exchange.checkNotModified("version", -1)).willReturn(true);

    assertThat(exchange.checkNotModified(1234L)).isTrue();
    assertThat(exchange.checkNotModified("version")).isTrue();
  }

  @Test
  void responseContentHelpersSetLengthAndTranslateMediaType() {
    HttpHeaders headers = HttpHeaders.forWritable();
    given(exchange.responseHeaders()).willReturn(headers);

    exchange.setContentLength(42);
    exchange.setContentType(MediaType.TEXT_PLAIN);
    exchange.setContentType((MediaType) null);

    assertThat(headers.getContentLength()).isEqualTo(42);
    verify(exchange).setContentType("text/plain");
    verify(exchange).setContentType((String) null);
  }

  @Test
  void trailersAreUnsupportedUnlessTheImplementationProvidesThem() {
    assertThatThrownBy(() -> exchange.setTrailerFields(new Trailer("X-Checksum", () -> "value")))
            .isInstanceOf(UnsupportedOperationException.class).hasMessage("Response trailers are not supported");
  }

  @Test
  void changesSessionIdWithoutCreatingASession() {
    assertThatIllegalStateException().isThrownBy(exchange::changeSessionId)
            .withMessageContaining("no session associated");

    Session session = mock(Session.class);
    given(exchange.getSession(false)).willReturn(session);
    given(session.changeSessionId()).willReturn("new-session-id");

    assertThat(exchange.changeSessionId()).isEqualTo("new-session-id");
  }

}
