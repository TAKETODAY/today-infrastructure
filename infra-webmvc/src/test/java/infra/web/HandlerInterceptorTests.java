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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 15:28
 */
class HandlerInterceptorTests {

  @Test
  void emptyArrayConstantIsNotNull() {
    assertThat(HandlerInterceptor.EMPTY_ARRAY).isNotNull();
    assertThat(HandlerInterceptor.EMPTY_ARRAY).isEmpty();
  }

  @Test
  void noneReturnValueConstantIsNotNull() {
    assertThat(HandlerInterceptor.NONE_RETURN_VALUE).isNotNull();
  }

  @Test
  void defaultPreProcessingReturnsTrue() throws Throwable {
    HandlerInterceptor interceptor = new HandlerInterceptor() { };
    RequestContext request = mock(RequestContext.class);
    Object handler = new Object();

    boolean result = interceptor.preProcessing(request, handler);

    assertThat(result).isTrue();
  }

  @Test
  void defaultPostProcessingDoesNotThrowException() throws Throwable {
    HandlerInterceptor interceptor = new HandlerInterceptor() { };
    RequestContext request = mock(RequestContext.class);
    Object handler = new Object();
    Object result = new Object();

    assertThatCode(() -> interceptor.postProcessing(request, handler, result)).doesNotThrowAnyException();
  }

  @Test
  void defaultInterceptProceedsWithChainWhenPreProcessingReturnsTrue() throws Throwable {
    HandlerInterceptor interceptor = new HandlerInterceptor() { };
    RequestContext request = mock(RequestContext.class);
    InterceptorChain chain = mock(InterceptorChain.class);
    Object handler = new Object();
    Object expectedResult = new Object();

    when(chain.getHandler()).thenReturn(handler);
    when(chain.proceed(request)).thenReturn(expectedResult);

    Object result = interceptor.intercept(request, chain);

    assertThat(result).isSameAs(expectedResult);
    verify(chain).getHandler();
    verify(chain).proceed(request);
  }

  @Test
  void defaultInterceptReturnsNoneReturnValueWhenPreProcessingReturnsFalse() throws Throwable {
    HandlerInterceptor interceptor = new TestHandlerInterceptor(false);
    RequestContext request = mock(RequestContext.class);
    InterceptorChain chain = mock(InterceptorChain.class);
    Object handler = new Object();

    when(chain.getHandler()).thenReturn(handler);

    Object result = interceptor.intercept(request, chain);

    assertThat(result).isSameAs(HandlerInterceptor.NONE_RETURN_VALUE);
    verify(chain).getHandler();
    verify(chain, never()).proceed(request);
  }

  @Test
  void interceptCallsPostProcessingWithCorrectParameters() throws Throwable {
    TestHandlerInterceptorWithAfterProcess interceptor = new TestHandlerInterceptorWithAfterProcess();
    RequestContext request = mock(RequestContext.class);
    InterceptorChain chain = mock(InterceptorChain.class);
    Object handler = new Object();
    Object expectedResult = new Object();

    when(chain.getHandler()).thenReturn(handler);
    when(chain.proceed(request)).thenReturn(expectedResult);

    Object result = interceptor.intercept(request, chain);

    assertThat(result).isSameAs(expectedResult);
    assertThat(interceptor.capturedRequest).isSameAs(request);
    assertThat(interceptor.capturedHandler).isSameAs(handler);
    assertThat(interceptor.capturedResult).isSameAs(expectedResult);
  }

  @Test
  void interceptDoesNotCallPostProcessingWhenPreProcessingReturnsFalse() throws Throwable {
    TestHandlerInterceptorWithAfterProcess interceptor = new TestHandlerInterceptorWithAfterProcess(false);
    RequestContext request = mock(RequestContext.class);
    InterceptorChain chain = mock(InterceptorChain.class);
    Object handler = new Object();

    when(chain.getHandler()).thenReturn(handler);

    Object result = interceptor.intercept(request, chain);

    assertThat(result).isSameAs(HandlerInterceptor.NONE_RETURN_VALUE);
    assertThat(interceptor.capturedRequest).isNull();
    assertThat(interceptor.capturedHandler).isNull();
    assertThat(interceptor.capturedResult).isNull();
  }

  static class TestHandlerInterceptor implements HandlerInterceptor {
    private final boolean returnValue;

    public TestHandlerInterceptor() {
      this(true);
    }

    public TestHandlerInterceptor(boolean returnValue) {
      this.returnValue = returnValue;
    }

    @Override
    public boolean preProcessing(RequestContext request, Object handler) throws Throwable {
      return returnValue;
    }
  }

  static class TestHandlerInterceptorWithAfterProcess implements HandlerInterceptor {
    private final boolean beforeProcessResult;
    RequestContext capturedRequest;
    Object capturedHandler;
    Object capturedResult;

    public TestHandlerInterceptorWithAfterProcess() {
      this(true);
    }

    public TestHandlerInterceptorWithAfterProcess(boolean beforeProcessResult) {
      this.beforeProcessResult = beforeProcessResult;
    }

    @Override
    public boolean preProcessing(RequestContext request, Object handler) throws Throwable {
      return beforeProcessResult;
    }

    @Override
    public void postProcessing(RequestContext request, Object handler, Object result) throws Throwable {
      this.capturedRequest = request;
      this.capturedHandler = handler;
      this.capturedResult = result;
    }
  }

}