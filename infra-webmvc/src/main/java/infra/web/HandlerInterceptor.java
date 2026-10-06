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

package infra.web;

import org.jspecify.annotations.Nullable;

import infra.web.handler.method.HandlerMethod;

/**
 * Intercepts HTTP request processing before and after a target handler is invoked.
 * Typical uses include logging, request validation, and updating the request or
 * response context.
 *
 * <p>The default {@link #intercept(HttpContext, InterceptorChain)} implementation
 * invokes {@link #preProcessing(HttpContext, Object)}, proceeds with the remaining
 * interceptor chain if allowed, and invokes
 * {@link #postProcessing(HttpContext, Object, Object)} when the chain returns
 * normally. Pre-processing callbacks run in chain order; post-processing callbacks
 * run in reverse order for interceptors that proceeded with the chain.
 *
 * <p>Returning {@code false} from {@code preProcessing} skips the remaining
 * interceptors and the target handler, and returns {@link #NONE_RETURN_VALUE}.
 * The interceptor is then responsible for handling the response.
 *
 * <p>Override {@code intercept} to replace the result, handle exceptions, or perform
 * cleanup in a {@code finally} block. {@code postProcessing} is not a completion
 * callback and is not invoked when pre-processing rejects the request or the
 * remaining chain throws an exception.
 *
 * <p>Use {@link infra.web.handler.MappedInterceptor} to apply an interceptor
 * conditionally based on request path patterns.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HandlerMethod#resolve(Object)
 * @see InterceptorChain
 * @since 2018-06-25 20:06:11
 */
public interface HandlerInterceptor {

  /**
   * An empty array of handler interceptors.
   */
  HandlerInterceptor[] EMPTY_ARRAY = {};

  /**
   * Sentinel indicating that no further return value handling is required.
   * This is the same instance as {@link HttpRequestHandler#NONE_RETURN_VALUE}.
   */
  Object NONE_RETURN_VALUE = HttpRequestHandler.NONE_RETURN_VALUE;

  /**
   * Apply pre-processing before the remaining interceptor chain and target handler.
   *
   * <p>With the default {@link #intercept(HttpContext, InterceptorChain)}
   * implementation, returning {@code false} skips the remaining chain and this
   * interceptor's post-processing callback. In that case, this interceptor is
   * responsible for handling the response.
   *
   * <p>The default implementation returns {@code true}.
   *
   * @param context the current HTTP request and response context
   * @param handler the target handler from {@link InterceptorChain#getHandler()},
   * potentially wrapped; use {@link HandlerMethod#resolve(Object)} to resolve a
   * handler method if applicable
   * @return {@code true} to proceed with the remaining chain, or {@code false}
   * to short-circuit processing
   * @throws Exception if pre-processing fails
   * @see HandlerMethod#resolve(Object)
   */
  default boolean preProcessing(HttpContext context, Object handler) throws Exception {
    return true;
  }

  /**
   * Apply post-processing after the remaining interceptor chain returns normally.
   *
   * <p>The result may originate from the target handler or a downstream interceptor,
   * including one that short-circuited the chain. This callback can update the
   * context or mutate a mutable result, but cannot replace the returned result.
   * Override {@link #intercept(HttpContext, InterceptorChain)} to return a different
   * result.
   *
   * <p>This callback is not invoked if this interceptor's pre-processing returns
   * {@code false} or the remaining chain throws an exception. For guaranteed
   * cleanup, override {@code intercept} and use a {@code finally} block.
   *
   * <p>The default implementation does nothing.
   *
   * @param context the current HTTP request and response context
   * @param handler the target handler from {@link InterceptorChain#getHandler()},
   * which may not have been invoked if a downstream interceptor short-circuited
   * @param result the result returned by the remaining chain, possibly {@code null}
   * or {@link #NONE_RETURN_VALUE}
   * @throws Exception if post-processing fails
   */
  default void postProcessing(HttpContext context, Object handler, @Nullable Object result) throws Exception {
  }

  /**
   * Intercept request processing around the remaining chain and target handler.
   *
   * <p>The default implementation obtains the target handler from the chain and
   * invokes {@link #preProcessing(HttpContext, Object)}. If it returns {@code true},
   * the implementation calls {@link InterceptorChain#proceed(HttpContext)}, invokes
   * {@link #postProcessing(HttpContext, Object, Object)} with the returned result,
   * and returns that result. Otherwise, it returns {@link #NONE_RETURN_VALUE}.
   * Exceptions propagate to the caller.
   *
   * <p>Custom implementations can delegate to {@code chain.proceed(context)} to
   * continue processing, or return a result directly to short-circuit the chain.
   * They can also replace the result, handle exceptions, or release resources in
   * a {@code finally} block.
   *
   * @param context the current HTTP request and response context
   * @param chain the chain used to invoke the next interceptor or target handler
   * @return the processing result, possibly {@code null}, or {@link #NONE_RETURN_VALUE}
   * if no further return value handling is required
   * @throws Exception if pre-processing, the remaining chain, or post-processing fails
   */
  default @Nullable Object intercept(HttpContext context, InterceptorChain chain) throws Exception {
    Object handler = chain.getHandler();
    if (preProcessing(context, handler)) {
      Object result = chain.proceed(context);
      postProcessing(context, handler, result);
      return result;
    }
    return NONE_RETURN_VALUE;
  }

}
