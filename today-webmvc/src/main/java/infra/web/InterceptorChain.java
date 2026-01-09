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

import infra.web.handler.HandlerWrapper;
import infra.web.handler.method.HandlerMethod;

/**
 * HandlerInterceptor execution chain
 *
 * @author TODAY 2021/8/8 14:57
 * @since 4.0
 */
public abstract class InterceptorChain {

  private int currentIndex = 0;

  private final Object handler;

  private final int interceptorLength;

  private final HandlerInterceptor[] interceptors;

  protected InterceptorChain(HandlerInterceptor[] interceptors, Object handler) {
    this.interceptorLength = interceptors.length;
    this.interceptors = interceptors;
    this.handler = handler;
  }

  /**
   * Execute next interceptor
   *
   * @param context current request context
   * @return interceptor or handler result, this will handle by {@link infra.web.ReturnValueHandler}
   * @throws Throwable if interceptor throw exception
   * @see infra.web.ReturnValueHandler
   */
  @Nullable
  public final Object proceed(RequestContext context) throws Throwable {
    if (currentIndex < interceptorLength) {
      return interceptors[currentIndex++].intercept(context, this);
    }
    return invokeHandler(context, handler);
  }

  /**
   * process target handler
   *
   * @param context current context
   * @param handler this context request handler
   * @return handle result
   */
  @Nullable
  protected abstract Object invokeHandler(RequestContext context, Object handler) throws Throwable;

  /**
   * Get interceptors
   */
  public HandlerInterceptor[] getInterceptors() {
    return interceptors;
  }

  /**
   * Get current interceptor's index
   */
  public int getCurrentIndex() {
    return currentIndex;
  }

  /**
   * target handler, maybe a
   * {@link infra.web.handler.method.HandlerMethod#unwrap(Object) HandlerMethod}
   *
   * @see HandlerMethod#unwrap(Object)
   * @see HandlerWrapper
   */
  public Object getHandler() {
    return handler;
  }

  /**
   * Handler maybe a
   * {@link infra.web.handler.method.HandlerMethod#unwrap(Object) HandlerMethod}
   * or {@link HandlerWrapper}
   *
   * @see HandlerMethod#unwrap(Object)
   * @see HandlerWrapper
   */
  public Object unwrapHandler() {
    return HandlerWrapper.unwrap(handler);
  }

}
