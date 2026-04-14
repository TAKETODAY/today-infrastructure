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
 * Represents a chain of {@link HandlerInterceptor}s that are executed in sequence
 * before the target handler is invoked. This class manages the execution order
 * and allows interceptors to short-circuit the request processing.
 *
 * @author TODAY 2021/8/8 14:57
 * @since 4.0
 */
public abstract class InterceptorChain {

  private int currentIndex = 0;

  private final Object handler;

  private final int interceptorLength;

  private final HandlerInterceptor[] interceptors;

  /**
   * Constructs a new {@code InterceptorChain} with the specified interceptors and handler.
   *
   * @param interceptors the array of interceptors to be executed in sequence
   * @param handler the target handler to be invoked after all interceptors have been processed
   */
  protected InterceptorChain(HandlerInterceptor[] interceptors, Object handler) {
    this.interceptorLength = interceptors.length;
    this.interceptors = interceptors;
    this.handler = handler;
  }

  /**
   * Executes the next interceptor in the chain, or invokes the target
   * handler if all interceptors have been executed.
   * <p>
   * This method is called recursively by each interceptor to proceed
   * with the execution chain. If there are remaining interceptors,
   * the next one is invoked. Otherwise, the target handler is invoked.
   *
   * @param context the current request context
   * @return the result returned by the handler or an interceptor,
   * which will be processed by {@link infra.web.ReturnValueHandler}
   * @throws Throwable if any interceptor or the handler throws an exception
   * @see infra.web.ReturnValueHandler
   */
  public final @Nullable Object proceed(RequestContext context) throws Throwable {
    if (currentIndex < interceptorLength) {
      return interceptors[currentIndex++].intercept(context, this);
    }
    return invokeHandler(context, handler);
  }

  /**
   * Invokes the target handler after all interceptors in the chain have been executed.
   * <p>
   * This method is called by {@link #proceed(RequestContext)} when there are no more
   * interceptors to process. Subclasses must implement this method to handle the
   * actual invocation of the target handler, which may be a {@link HandlerMethod}
   * or another type of handler wrapped by {@link HandlerWrapper}.
   *
   * @param context the current request context containing request and response information
   * @param handler the target handler to invoke, which may need to be unwrapped using
   * {@link HandlerWrapper#unwrap(Object)} if it is a wrapped handler
   * @return the result returned by the handler, which will be processed by
   * {@link infra.web.ReturnValueHandler}
   * @throws Throwable if the handler invocation fails or throws an exception
   * @see HandlerMethod
   * @see HandlerWrapper
   */
  protected abstract @Nullable Object invokeHandler(RequestContext context, Object handler)
          throws Throwable;

  /**
   * Returns the array of interceptors in this chain.
   *
   * @return the interceptors
   */
  public HandlerInterceptor[] getInterceptors() {
    return interceptors;
  }

  /**
   * Returns the index of the current interceptor being executed.
   *
   * @return the current interceptor index
   */
  public int getCurrentIndex() {
    return currentIndex;
  }

  /**
   * Returns the target handler associated with this chain.
   * <p>
   * The returned handler may be a {@link HandlerMethod} or another object
   * wrapped by a {@link HandlerWrapper}. To obtain the actual handler instance,
   * consider using {@link #unwrapHandler()}.
   *
   * @return the target handler, which may need unwrapping
   * @see #unwrapHandler()
   * @see HandlerMethod
   * @see HandlerWrapper
   */
  public Object getHandler() {
    return handler;
  }

  /**
   * Unwraps the target handler if it is wrapped by a {@link HandlerWrapper}.
   * <p>
   * This method delegates to {@link HandlerWrapper#unwrap(Object)} to retrieve
   * the underlying handler instance. If the handler is not wrapped, it is
   * returned as-is.
   *
   * @return the unwrapped target handler
   * @see HandlerWrapper#unwrap(Object)
   * @see HandlerMethod
   */
  public Object unwrapHandler() {
    return HandlerWrapper.unwrap(handler);
  }

}
