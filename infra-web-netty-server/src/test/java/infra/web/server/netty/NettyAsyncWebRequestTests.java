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

package infra.web.server.netty;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 13:14
 */
class NettyAsyncWebRequestTests {

  @Test
  void dispatchIgnoresLateResultsAndTimeouts() throws Exception {
    NettyHttpContext request = mock(NettyHttpContext.class);
    var asyncRequest = new NettyAsyncWebRequest(request);
    var completions = new AtomicInteger();
    var timeouts = new AtomicInteger();
    asyncRequest.addCompletionHandler(completions::incrementAndGet);
    asyncRequest.addTimeoutHandler(timeouts::incrementAndGet);
    asyncRequest.startAsync();

    asyncRequest.dispatch("first result");
    asyncRequest.dispatch("late result");
    asyncRequest.dispatch(new AssertionError("late filter failure"));
    asyncRequest.checkTimeout();

    assertThat(asyncRequest.isAsyncStarted()).isFalse();
    assertThat(asyncRequest.isAsyncComplete()).isTrue();
    assertThat(completions).hasValue(1);
    assertThat(timeouts).hasValue(0);
    assertThatIllegalStateException().isThrownBy(asyncRequest::startAsync);
    verify(request, times(1)).dispatchConcurrentResult("first result");
    verifyNoMoreInteractions(request);
  }

  @Test
  void constructorShouldInitializeFields() {
    NettyHttpContext request = mock(NettyHttpContext.class);
    NettyAsyncWebRequest asyncWebRequest = new NettyAsyncWebRequest(request);

    assertThat(asyncWebRequest.isAsyncStarted()).isFalse();
  }

  @Test
  void startAsyncShouldSetAsyncStartedFlag() {
    NettyHttpContext request = mock(NettyHttpContext.class);
    NettyAsyncWebRequest asyncWebRequest = new NettyAsyncWebRequest(request);

    asyncWebRequest.startAsync();

    assertThat(asyncWebRequest.isAsyncStarted()).isTrue();
  }

  @Test
  void isAsyncStartedShouldReturnFalseByDefault() {
    NettyHttpContext request = mock(NettyHttpContext.class);
    NettyAsyncWebRequest asyncWebRequest = new NettyAsyncWebRequest(request);

    assertThat(asyncWebRequest.isAsyncStarted()).isFalse();
  }

}
