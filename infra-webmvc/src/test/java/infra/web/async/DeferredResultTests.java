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

package infra.web.async;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 19:00
 */
class DeferredResultTests {

  @Test
  void ignoresHandlerRegisteredAfterCompletion() throws Exception {
    DeferredResult<String> result = new DeferredResult<>();
    result.createInterceptor().afterCompletion(null, result);
    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    result.setResultHandler(handler);

    assertThat(result.setResult("late")).isFalse();
    assertThat(result.hasResult()).isFalse();
    verifyNoInteractions(handler);
  }

  @Test
  void retainsExistingResultWhenLateHandlerThrows() {
    var result = new DeferredResult<String>();
    assertThat(result.setResult("ready")).isTrue();
    DeferredResultHandler handler = mock(DeferredResultHandler.class);
    willThrow(new IllegalStateException("handler failure")).given(handler).handleResult("ready");

    result.setResultHandler(handler);

    assertThat(result.getResult()).isEqualTo("ready");
    verify(handler).handleResult("ready");
  }

  @Test
  void retainsTimeoutResultWhenHandlerThrows() throws Exception {
    var result = new DeferredResult<String>(null, "fallback");
    DeferredResultHandler handler = mock(DeferredResultHandler.class);
    willThrow(new IllegalStateException("handler failure")).given(handler).handleResult("fallback");
    result.setResultHandler(handler);

    assertThat(result.createInterceptor().handleTimeout(null, result)).isFalse();
    assertThat(result.getResult()).isEqualTo("fallback");
    verify(handler).handleResult("fallback");
  }

  @Test
  void retainsRequestErrorWhenHandlerThrows() throws Exception {
    var result = new DeferredResult<String>();
    Exception requestError = new Exception("request failure");
    DeferredResultHandler handler = mock(DeferredResultHandler.class);
    willThrow(new IllegalStateException("handler failure")).given(handler).handleResult(requestError);
    result.setResultHandler(handler);

    assertThat(result.createInterceptor().handleError(null, result, requestError)).isFalse();
    assertThat(result.getResult()).isSameAs(requestError);
    verify(handler).handleResult(requestError);
  }

  @Test
  public void setResult() {
    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>();
    result.setResultHandler(handler);

    assertThat(result.setResult("hello")).isTrue();
    verify(handler).handleResult("hello");
  }

  @Test
  public void setResultTwice() {
    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>();
    result.setResultHandler(handler);

    assertThat(result.setResult("hello")).isTrue();
    assertThat(result.setResult("hi")).isFalse();

    verify(handler).handleResult("hello");
  }

  @Test
  public void isSetOrExpired() {
    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>();
    result.setResultHandler(handler);

    assertThat(result.isSetOrExpired()).isFalse();

    result.setResult("hello");

    assertThat(result.isSetOrExpired()).isTrue();

    verify(handler).handleResult("hello");
  }

  @Test
  public void hasResult() {
    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>();
    result.setResultHandler(handler);

    assertThat(result.hasResult()).isFalse();
    assertThat(result.getResult()).isNull();

    result.setResult("hello");

    assertThat(result.getResult()).isEqualTo("hello");
  }

  @Test
  public void onCompletion() throws Exception {
    final StringBuilder sb = new StringBuilder();

    DeferredResult<String> result = new DeferredResult<>();
    result.onCompletion(() -> sb.append("completion event"));

    result.createInterceptor().afterCompletion(null, null);

    assertThat(result.isSetOrExpired()).isTrue();
    assertThat(sb.toString()).isEqualTo("completion event");
  }

  @Test
  public void onTimeout() throws Exception {
    final StringBuilder sb = new StringBuilder();

    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>(null, "timeout result");
    result.setResultHandler(handler);
    result.onTimeout(() -> sb.append("timeout event"));

    result.createInterceptor().handleTimeout(null, null);

    assertThat(sb.toString()).isEqualTo("timeout event");
    assertThat(result.setResult("hello")).as("Should not be able to set result a second time").isFalse();
    verify(handler).handleResult("timeout result");
  }

  @Test
  public void onError() throws Exception {
    final StringBuilder sb = new StringBuilder();

    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>(null, "error result");
    result.setResultHandler(handler);
    Exception e = new Exception();
    result.onError(t -> sb.append("error event"));

    result.createInterceptor().handleError(null, null, e);

    assertThat(sb.toString()).isEqualTo("error event");
    assertThat(result.setResult("hello")).as("Should not be able to set result a second time").isFalse();
    verify(handler).handleResult(e);
  }

}
