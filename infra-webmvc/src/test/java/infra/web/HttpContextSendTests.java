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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.http.HttpEntity;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HttpContextSendTests {

  @Test
  void resultIsPassedUnchangedToConfiguredHandler() throws Exception {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.refresh();
      DispatcherHandler dispatcher = createDispatcher(context);
      ReturnValueHandlerManager manager = mock(ReturnValueHandlerManager.class);
      ReturnValueHandler selected = mock(ReturnValueHandler.class);
      Object result = new Object();
      given(manager.selectHandler(null, result)).willReturn(selected);
      dispatcher.setReturnValueHandler(manager);
      MockHttpContext http = new MockHttpContext(new MockRequest(), dispatcher);

      http.send(result);

      verify(manager).selectHandler(null, result);
      verify(selected).handleReturnValue(http, null, result);
    }
  }

  @Test
  void explicitStringBodyIsSentWithoutViewNameInference() throws Exception {
    verifyFilterResponse(new HttpEntity<>("redirect:/login"), 200, "redirect:/login");
  }

  @Test
  void objectUsesConfiguredMessageConverters() throws Exception {
    verifyFilterResponse(Map.of("message", "denied"), 200, "{\"message\":\"denied\"}");
  }

  @Test
  void responseEntitySetsStatusHeadersAndBody() throws Exception {
    verifyFilterResponse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header("X-Reason", "authentication")
            .body(Map.of("message", "denied")), 401, "{\"message\":\"denied\"}");
  }

  @Test
  void nullLeavesResponseUnchanged() throws Exception {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.refresh();
      DispatcherHandler dispatcher = createDispatcher(context);
      MockHttpContext http = new MockHttpContext(new MockRequest(), dispatcher);
      http.setStatus(HttpStatus.ACCEPTED);
      http.setHeader("X-Existing", "preserved");

      http.send(null);

      assertThat(http.getStatus()).isEqualTo(202);
      assertThat(http.responseHeaders().getFirst("X-Existing")).isEqualTo("preserved");
      assertThat(http.response.getContentAsString()).isEmpty();
      assertThat(http.isRequestCompleted()).isFalse();
      assertThat(http.isCommitted()).isFalse();
    }
  }

  @Test
  void committedResponseIsRejected() throws Exception {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.refresh();
      DispatcherHandler dispatcher = createDispatcher(context);
      MockHttpContext http = new MockHttpContext(new MockRequest(), dispatcher);
      http.response.flushBuffer();

      assertThatThrownBy(() -> http.send("body"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Response is already committed");
    }
  }

  @Test
  void completedRequestIsRejected() throws Exception {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean("filter", Filter.class, () -> (http, chain) -> http.send(null));
      context.refresh();
      DispatcherHandler dispatcher = createDispatcher(context);
      MockHttpContext http = new MockHttpContext(new MockRequest(), dispatcher);
      dispatcher.handleRequest(http);

      assertThatThrownBy(() -> http.send("body"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Request is already completed");
    }
  }

  @Test
  void conversionFailurePropagatesWithoutCompletingRequest() throws Exception {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.refresh();
      DispatcherHandler dispatcher = createDispatcher(context);
      MockHttpContext http = new MockHttpContext(new MockRequest(), dispatcher);
      http.request.addHeader("Accept", "application/xml");

      assertThatThrownBy(() -> http.send(Map.of("message", "denied")))
              .isInstanceOf(HttpMediaTypeNotAcceptableException.class);
      assertThat(http.isRequestCompleted()).isFalse();
      assertThat(http.isCommitted()).isFalse();
    }
  }

  @Test
  void filterSendFailureUsesDispatcherExceptionHandling() throws Exception {
    AtomicInteger completions = new AtomicInteger();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean("filter", Filter.class,
              () -> (http, chain) -> http.send(Map.of("message", "denied")));
      context.refresh();
      DispatcherHandler dispatcher = createDispatcher(context);
      dispatcher.setExceptionHandler((http, failure, handler) -> {
        assertThat(failure).isInstanceOf(HttpMediaTypeNotAcceptableException.class);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.TEXT_PLAIN).body("not acceptable");
      });
      dispatcher.addRequestCompletedActions((http, failure) -> completions.incrementAndGet());
      MockHttpContext http = new MockHttpContext(new MockRequest(), dispatcher);
      http.request.addHeader("Accept", "application/xml");

      dispatcher.handleRequest(http);

      assertThat(http.getStatus()).isEqualTo(406);
      assertThat(http.response.getContentAsString()).isEqualTo("not acceptable");
      assertThat(http.isRequestCompleted()).isTrue();
      assertThat(completions).hasValue(1);
    }
  }

  private void verifyFilterResponse(Object result, int status, String body) throws Exception {
    AtomicInteger completions = new AtomicInteger();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean("filter", Filter.class, () -> (http, chain) -> {
        http.send(result);
        assertThat(http.isRequestCompleted()).isFalse();
      });
      context.refresh();
      DispatcherHandler dispatcher = createDispatcher(context);
      dispatcher.addRequestCompletedActions((http, failure) -> completions.incrementAndGet());
      MockHttpContext http = new MockHttpContext(new MockRequest(), dispatcher);

      dispatcher.handleRequest(http);

      assertThat(http.getStatus()).isEqualTo(status);
      assertThat(http.response.getContentAsString()).isEqualTo(body);
      if (result instanceof ResponseEntity<?>) {
        assertThat(http.responseHeaders().getFirst("X-Reason")).isEqualTo("authentication");
      }
      if (result instanceof Map<?, ?> || result instanceof ResponseEntity<?>) {
        assertThat(http.responseHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
      }
      assertThat(http.isRequestCompleted()).isTrue();
      assertThat(completions).hasValue(1);
    }
  }

  private DispatcherHandler createDispatcher(AnnotationConfigApplicationContext context) {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager(List.of(
            new StringHttpMessageConverter(), new JacksonJsonHttpMessageConverter()));
    manager.setApplicationContext(context);
    manager.registerDefaultHandlers();
    DispatcherHandler dispatcher = new DispatcherHandler(context);
    dispatcher.setReturnValueHandler(manager);
    dispatcher.setHandlerMapping(http -> {
      throw new AssertionError("Handler lookup must not run for a short-circuit response");
    });
    dispatcher.start();
    return dispatcher;
  }

}
