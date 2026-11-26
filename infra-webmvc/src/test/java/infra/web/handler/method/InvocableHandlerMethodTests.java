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

package infra.web.handler.method;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import infra.context.MessageSource;
import infra.context.support.StaticMessageSource;
import infra.http.HttpStatus;
import infra.web.ServerHttpExchange;
import infra.web.HttpRequestHandler;
import infra.web.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class InvocableHandlerMethodTests {

  @Test
  void responseStatusReasonUsesRequestMessageSourceForEachRequest() throws Exception {
    var messages = new StaticMessageSource();
    messages.addMessage("gone.reason", Locale.FRENCH, "Supprimé");
    messages.addMessage("gone.reason", Locale.GERMAN, "Entfernt");
    var handler = new InvocableHandlerMethod(new TestController(),
            TestController.class.getDeclaredMethod("gone"), new HandlerParameterFactory());

    assertResponseStatusReason(handler, messages, Locale.FRENCH, "Supprimé");
    assertResponseStatusReason(handler, messages, Locale.GERMAN, "Entfernt");
    assertResponseStatusReason(handler, messages, Locale.ENGLISH, "gone.reason");
  }

  @Test
  void responseStatusReasonPrefersConfiguredMessageSource() throws Exception {
    var messages = new StaticMessageSource();
    messages.addMessage("gone.reason", Locale.FRENCH, "Supprimé");
    var requestMessages = new StaticMessageSource();
    requestMessages.addMessage("gone.reason", Locale.FRENCH, "Indisponible");
    var handler = new InvocableHandlerMethod(new TestController(),
            TestController.class.getDeclaredMethod("gone"), messages, new HandlerParameterFactory());

    assertResponseStatusReason(handler, requestMessages, Locale.FRENCH, "Supprimé");
  }

  private void assertResponseStatusReason(InvocableHandlerMethod handler,
          MessageSource messages, Locale locale, String reason) throws Exception {
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    given(request.getMessageSource()).willReturn(messages);
    given(request.getPreferredLocale()).willReturn(locale);

    assertThat(handler.invokeAndHandle(request, null, null)).isSameAs(HttpRequestHandler.NONE_RETURN_VALUE);
    then(request).should().sendError(410, reason);
  }

  static class TestController {

    @ResponseStatus(code = HttpStatus.GONE, reason = "gone.reason")
    public void gone() {
    }
  }

}
