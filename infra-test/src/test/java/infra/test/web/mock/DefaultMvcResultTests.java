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

package infra.test.web.mock;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import infra.web.mock.MockServerHttpExchange;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.BindingContext;
import infra.web.view.ModelAndView;
import infra.web.view.View;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link DefaultMvcResult}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultMvcResultTests {

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.api.Timeout(5)
  @org.junit.jupiter.params.provider.ValueSource(longs = { -5, -1, 0 })
  void getAsyncResultWithInfiniteAsyncRequestTimeout(long timeout) {
    DefaultMvcResult result = createMvcResultWithAsyncRequestTimeout(timeout);
    CountDownLatch latch = new CountDownLatch(1);
    result.setAsyncDispatchLatch(latch);
    java.util.concurrent.CompletableFuture.runAsync(() -> {
      result.setAsyncResult("Foo");
      latch.countDown();
    }, java.util.concurrent.CompletableFuture.delayedExecutor(100, java.util.concurrent.TimeUnit.MILLISECONDS));
    assertThat(result.getAsyncResult()).isEqualTo("Foo");
  }

  @Test
  @org.junit.jupiter.api.Timeout(5)
  void getAsyncResultWithExplicitTimeToWaitOfZeroDoesNotWait() {
    DefaultMvcResult result = createMvcResultWithAsyncRequestTimeout(0);
    result.setAsyncDispatchLatch(new CountDownLatch(1));
    assertThatIllegalStateException().isThrownBy(() -> result.getAsyncResult(0))
            .withMessageEndingWith("was not set during the specified timeToWait=0");
  }

  @Test
  void getAsyncResultWithAsyncRequestTimeoutExceeded() {
    DefaultMvcResult result = createMvcResultWithAsyncRequestTimeout(50);
    result.setAsyncDispatchLatch(new CountDownLatch(1));
    assertThatIllegalStateException().isThrownBy(result::getAsyncResult)
            .withMessageEndingWith("was not set during the specified timeToWait=50");
  }

  private static DefaultMvcResult createMvcResultWithAsyncRequestTimeout(long timeout) {
    MockRequest request = new MockRequest();
    request.setAsyncSupported(true);
    request.startAsync().setTimeout(timeout);
    MockResponse response = new MockResponse();
    return new DefaultMvcResult(request, response, new MockServerHttpExchange(request, response));
  }

  MockRequest request = new MockRequest();

  MockResponse response = new MockResponse();

  private final DefaultMvcResult mvcResult = new DefaultMvcResult(
          request, response, new MockServerHttpExchange(request, response));

  @Test
  public void getAsyncResultSuccess() {
    this.mvcResult.setAsyncResult("Foo");
    this.mvcResult.setAsyncDispatchLatch(new CountDownLatch(0));
    this.mvcResult.getAsyncResult();
  }

  @Test
  public void getAsyncResultFailure() {
    assertThatIllegalStateException().isThrownBy(() ->
            this.mvcResult.getAsyncResult(0));
  }

  @Test
  void getAsyncResultRestoresInterruptStatusWhenInterrupted() {
    this.mvcResult.setAsyncDispatchLatch(new CountDownLatch(1));
    Thread.currentThread().interrupt();
    try {
      assertThatIllegalStateException().isThrownBy(() ->
              this.mvcResult.getAsyncResult(1000));
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
    finally {
      // Clear the interrupt status so it does not leak to other tests.
      Thread.interrupted();
    }
  }

  @Test
  void resultFindsExplicitAndBindingContextModelAndViews() {
    var context = mvcResult.getExchange();
    var explicit = new ModelAndView("explicit");
    context.setAttribute(MvcResult.MODEL_AND_VIEW_ATTRIBUTE, explicit);
    assertThat(mvcResult.getModelAndView()).isSameAs(explicit);
    assertThat(mvcResult.getModelAndView()).isSameAs(explicit);
    assertThat(DefaultMvcResult.forContext(context)).isSameAs(mvcResult);
    assertThatIllegalStateException().isThrownBy(() -> DefaultMvcResult.forContext(new MockServerHttpExchange()))
            .withMessage("No DefaultMvcResult");
  }

  @Test
  void resultBuildsModelAndViewFromBindingModelAndViewAttributes() {
    for (Object view : new Object[] { "view-name", mock(View.class), new Object() }) {
      var context = new MockServerHttpExchange();
      var binding = new BindingContext();
      BindingContext.set(context, binding);
      binding.getModel().addAttribute("key", "value");
      if (view instanceof String name) {
        context.setAttribute(MvcResult.VIEW_NAME_ATTRIBUTE, name);
      }
      else if (view instanceof View) {
        context.setAttribute(MvcResult.VIEW_ATTRIBUTE, view);
      }
      var result = new DefaultMvcResult(context.getRequest(), context.getResponse(), context);
      assertThat(result.getModelAndView().getModel()).containsEntry("key", "value");
      assertThat(result.getModelAndView()).isSameAs(result.getModelAndView());
    }
  }

  @Test
  void redirectModelsAreCreatedOnceAndMissingAsyncResultsAreRejected() {
    assertThat((Object) mvcResult.getOutputRedirectModel()).isSameAs(mvcResult.getOutputRedirectModel());
    mvcResult.setAsyncDispatchLatch(new CountDownLatch(0));
    assertThatIllegalStateException().isThrownBy(() -> mvcResult.getAsyncResult(0))
            .withMessageContaining("was not set");
  }

}
