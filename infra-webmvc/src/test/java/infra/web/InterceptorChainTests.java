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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 15:25
 */
class InterceptorChainTests {
  @Test
  void constructorInitializesFieldsCorrectly() {
    HandlerInterceptor[] interceptors = new HandlerInterceptor[] { mock(HandlerInterceptor.class) };
    Object handler = new Object();

    TestInterceptorChain chain = new TestInterceptorChain(interceptors, handler);

    assertThat(chain.getInterceptors()).isSameAs(interceptors);
    assertThat(chain.getHandler()).isSameAs(handler);
    assertThat(chain.getCurrentIndex()).isEqualTo(0);
  }

  @Test
  void proceedExecutesInterceptorsInOrder() throws Throwable {
    HandlerInterceptor interceptor1 = mock(HandlerInterceptor.class);
    HandlerInterceptor interceptor2 = mock(HandlerInterceptor.class);
    HandlerInterceptor[] interceptors = new HandlerInterceptor[] { interceptor1, interceptor2 };
    Object handler = new Object();

    TestInterceptorChain chain = new TestInterceptorChain(interceptors, handler);
    RequestContext context = mock(RequestContext.class);

    when(interceptor1.intercept(context, chain)).thenReturn("result1");

    Object result = chain.proceed(context);

    assertThat(result).isEqualTo("result1");
    assertThat(chain.getCurrentIndex()).isEqualTo(1);
    verify(interceptor1).intercept(context, chain);
  }

  @Test
  void proceedMultipleTimesExecutesInterceptorsSequentially() throws Throwable {
    HandlerInterceptor interceptor1 = mock(HandlerInterceptor.class);
    HandlerInterceptor interceptor2 = mock(HandlerInterceptor.class);
    HandlerInterceptor[] interceptors = new HandlerInterceptor[] { interceptor1, interceptor2 };
    Object handler = new Object();

    TestInterceptorChain chain = new TestInterceptorChain(interceptors, handler);
    RequestContext context = mock(RequestContext.class);

    when(interceptor1.intercept(context, chain)).thenReturn(null);
    when(interceptor2.intercept(context, chain)).thenReturn("interceptor2Result");

    Object result1 = chain.proceed(context);
    Object result2 = chain.proceed(context);

    assertThat(result1).isNull();
    assertThat(result2).isEqualTo("interceptor2Result");
    assertThat(chain.getCurrentIndex()).isEqualTo(2);
  }

  @Test
  void proceedReturnsHandlerResultAfterAllInterceptors() throws Throwable {
    HandlerInterceptor[] interceptors = HandlerInterceptor.EMPTY_ARRAY;
    Object handler = new Object();

    TestInterceptorChain chain = new TestInterceptorChain(interceptors, handler);
    RequestContext context = mock(RequestContext.class);
    chain.testResult = "finalResult";

    Object result = chain.proceed(context);

    assertThat(result).isEqualTo("finalResult");
    assertThat(chain.invokeHandlerCalled).isTrue();
  }

  @Test
  void unwrapHandlerReturnsUnwrappedHandler() {
    Object rawHandler = new Object();
    infra.web.handler.HandlerWrapper handler = () -> rawHandler;
    HandlerInterceptor[] interceptors = new HandlerInterceptor[0];

    TestInterceptorChain chain = new TestInterceptorChain(interceptors, handler);

    Object result = chain.unwrapHandler();

    assertThat(result).isSameAs(rawHandler);
  }

  @Test
  void unwrapHandlerReturnsHandlerAsIsWhenNotWrapper() {
    Object handler = new Object();
    HandlerInterceptor[] interceptors = new HandlerInterceptor[0];

    TestInterceptorChain chain = new TestInterceptorChain(interceptors, handler);

    Object result = chain.unwrapHandler();

    assertThat(result).isSameAs(handler);
  }

  static class TestInterceptorChain extends InterceptorChain {
    boolean invokeHandlerCalled = false;
    RequestContext invokeHandlerContext;
    Object invokeHandlerHandler;
    Object testResult;

    public TestInterceptorChain(HandlerInterceptor[] interceptors, Object handler) {
      super(interceptors, handler);
    }

    @Nullable
    @Override
    protected Object invokeHandler(RequestContext context, Object handler) throws Throwable {
      invokeHandlerCalled = true;
      invokeHandlerContext = context;
      invokeHandlerHandler = handler;
      return testResult;
    }
  }

}