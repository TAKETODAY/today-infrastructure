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

package infra.web.context.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import infra.mock.web.MockAsyncContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.StandardMockAsyncWebRequest;
import infra.mock.api.AsyncEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 19:01
 */
class StandardMockAsyncWebRequestTests {

  private StandardMockAsyncWebRequest asyncRequest;

  private HttpMockRequestImpl request;

  private MockHttpResponseImpl response;

  @BeforeEach
  public void setup() {
    this.request = new HttpMockRequestImpl();
    this.request.setAsyncSupported(true);
    this.response = new MockHttpResponseImpl();
    this.asyncRequest = new StandardMockAsyncWebRequest(this.request, this.response);
    this.asyncRequest.setTimeout(44 * 1000L);
  }

  @Test
  public void isAsyncStarted() throws Exception {
    assertThat(this.asyncRequest.isAsyncStarted()).isFalse();
    this.asyncRequest.startAsync();
    assertThat(this.asyncRequest.isAsyncStarted()).isTrue();
  }

  @Test
  public void startAsync() throws Exception {
    this.asyncRequest.startAsync();

    MockAsyncContext context = (MockAsyncContext) this.request.getAsyncContext();
    assertThat(context).isNotNull();
    assertThat(context.getTimeout()).as("Timeout value not set").isEqualTo((44 * 1000));
    assertThat(context.getListeners().size()).isEqualTo(1);
    assertThat(context.getListeners().get(0)).isSameAs(this.asyncRequest);
  }

  @Test
  public void startAsyncMultipleTimes() throws Exception {
    this.asyncRequest.startAsync();
    this.asyncRequest.startAsync();
    this.asyncRequest.startAsync();
    this.asyncRequest.startAsync();  // idempotent

    MockAsyncContext context = (MockAsyncContext) this.request.getAsyncContext();
    assertThat(context).isNotNull();
    assertThat(context.getListeners().size()).isEqualTo(1);
  }

  @Test
  public void startAsyncNotSupported() throws Exception {
    this.request.setAsyncSupported(false);
    assertThatIllegalStateException().isThrownBy(
                    this.asyncRequest::startAsync)
            .withMessageContaining("Async support must be enabled");
  }

  @Test
  public void startAsyncAfterCompleted() throws Exception {
    this.asyncRequest.onComplete(new AsyncEvent(new MockAsyncContext(this.request, this.response)));
    assertThatIllegalStateException().isThrownBy(
                    this.asyncRequest::startAsync)
            .withMessage("Async processing has already completed");
  }

  @Test
  public void onTimeoutDefaultBehavior() throws Exception {
    this.asyncRequest.onTimeout(new AsyncEvent(new MockAsyncContext(this.request, this.response)));
    assertThat(this.response.getStatus()).isEqualTo(200);
  }

  @Test
  public void onTimeoutHandler() throws Exception {
    Runnable timeoutHandler = mock(Runnable.class);
    this.asyncRequest.addTimeoutHandler(timeoutHandler);
    this.asyncRequest.onTimeout(new AsyncEvent(new MockAsyncContext(this.request, this.response)));
    verify(timeoutHandler).run();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onErrorHandler() throws Exception {
    Consumer<Throwable> errorHandler = mock(Consumer.class);
    this.asyncRequest.addErrorHandler(errorHandler);
    Exception e = new Exception();
    this.asyncRequest.onError(new AsyncEvent(new MockAsyncContext(this.request, this.response), e));
    verify(errorHandler).accept(e);
  }

  @Test
  public void setTimeoutDuringConcurrentHandling() {
    this.asyncRequest.startAsync();
    assertThatIllegalStateException().isThrownBy(() ->
            this.asyncRequest.setTimeout(25L));
  }

  @Test
  public void onCompletionHandler() throws Exception {
    Runnable handler = mock(Runnable.class);
    this.asyncRequest.addCompletionHandler(handler);

    this.asyncRequest.startAsync();
    this.asyncRequest.onComplete(new AsyncEvent(this.request.getAsyncContext()));

    verify(handler).run();
    assertThat(this.asyncRequest.isAsyncComplete()).isTrue();
  }

  // SPR-13292

  @SuppressWarnings("unchecked")
  @Test
  public void onErrorHandlerAfterOnErrorEvent() throws Exception {
    Consumer<Throwable> handler = mock(Consumer.class);
    this.asyncRequest.addErrorHandler(handler);

    this.asyncRequest.startAsync();
    Exception e = new Exception();
    this.asyncRequest.onError(new AsyncEvent(this.request.getAsyncContext(), e));

    verify(handler).accept(e);
  }

  @Test
  public void onCompletionHandlerAfterOnCompleteEvent() throws Exception {
    Runnable handler = mock(Runnable.class);
    this.asyncRequest.addCompletionHandler(handler);

    this.asyncRequest.startAsync();
    this.asyncRequest.onComplete(new AsyncEvent(this.request.getAsyncContext()));

    verify(handler).run();
    assertThat(this.asyncRequest.isAsyncComplete()).isTrue();
  }
}