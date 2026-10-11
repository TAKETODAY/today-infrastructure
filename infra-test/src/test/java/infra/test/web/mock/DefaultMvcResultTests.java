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

import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

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
    return new DefaultMvcResult(request, response, new MockHttpContext(request, response));
  }

  MockRequest request = new MockRequest();

  MockResponse response = new MockResponse();

  private final DefaultMvcResult mvcResult = new DefaultMvcResult(
          request, response, new MockHttpContext(request, response));

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

}
