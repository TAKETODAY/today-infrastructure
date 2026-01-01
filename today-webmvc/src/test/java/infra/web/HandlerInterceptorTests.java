/*
 * Copyright 2017 - 2025 the original author or authors.
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
  void defaultBeforeProcessReturnsTrue() throws Throwable {
    HandlerInterceptor interceptor = new HandlerInterceptor() { };
    RequestContext request = mock(RequestContext.class);
    Object handler = new Object();

    boolean result = interceptor.beforeProcess(request, handler);

    assertThat(result).isTrue();
  }

  @Test
  void defaultAfterProcessDoesNotThrowException() throws Throwable {
    HandlerInterceptor interceptor = new HandlerInterceptor() { };
    RequestContext request = mock(RequestContext.class);
    Object handler = new Object();
    Object result = new Object();

    assertThatCode(() -> interceptor.afterProcess(request, handler, result)).doesNotThrowAnyException();
  }

  @Test
  void defaultInterceptProceedsWithChainWhenBeforeProcessReturnsTrue() throws Throwable {
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
  void defaultInterceptReturnsNoneReturnValueWhenBeforeProcessReturnsFalse() throws Throwable {
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
  void interceptCallsAfterProcessWithCorrectParameters() throws Throwable {
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
  void interceptDoesNotCallAfterProcessWhenBeforeProcessReturnsFalse() throws Throwable {
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
    public boolean beforeProcess(RequestContext request, Object handler) throws Throwable {
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
    public boolean beforeProcess(RequestContext request, Object handler) throws Throwable {
      return beforeProcessResult;
    }

    @Override
    public void afterProcess(RequestContext request, Object handler, Object result) throws Throwable {
      this.capturedRequest = request;
      this.capturedHandler = handler;
      this.capturedResult = result;
    }
  }

}