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

package infra.web.handler;

import org.jspecify.annotations.Nullable;

import infra.web.HandlerExceptionHandler;
import infra.web.RequestContext;
import infra.web.handler.function.HandlerFunction;
import infra.web.handler.method.HandlerMethod;

/**
 * Abstract base class for {@link HandlerExceptionHandler HandlerExceptionHandler}
 * implementations that support handling exceptions from handlers of type
 * {@link HandlerMethod}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 21:53
 */
public abstract class AbstractHandlerMethodExceptionHandler extends AbstractHandlerExceptionHandler {

  /**
   * Checks if the handler is a {@link HandlerMethod} and then delegates to the
   * base class implementation of {@code #shouldApplyTo(RequestContext, Object)}
   * passing the bean of the {@code HandlerMethod}. Otherwise returns {@code false}.
   */
  @Override
  protected boolean shouldApplyTo(RequestContext request, @Nullable Object handler) {
    if (handler == null) {
      return super.shouldApplyTo(request, null);
    }

    // unwrap HandlerExecutionChain
    if (handler instanceof HandlerWrapper chain) {
      handler = chain.getRawHandler();
    }

    if (handler instanceof HandlerMethod handlerMethod) {
      handler = handlerMethod.getBean();
      return super.shouldApplyTo(request, handler);
    }

    if (handler instanceof HandlerFunction<?> handlerFunction) {
      return super.shouldApplyTo(request, handlerFunction);
    }

    if (hasGlobalExceptionHandlers() && hasHandlerMappings()) {
      return super.shouldApplyTo(request, handler);
    }
    return false;
  }

  /**
   * Whether this handler has global exception handlers, e.g. not declared in
   * the same class as the {@code HandlerMethod} that raised the exception and
   * therefore can apply to any handler.
   */
  protected boolean hasGlobalExceptionHandlers() {
    return false;
  }

  @Nullable
  @Override
  protected Object handleInternal(RequestContext request, @Nullable Object handler, Throwable ex) throws Exception {
    if (handler instanceof HandlerExecutionChain chain) {
      handler = chain.getRawHandler();
    }

    if (handler instanceof HandlerMethod handlerMethod) {
      return handleInternal(request, handlerMethod, ex);
    }
    else {
      return handleInternal(request, null, ex);
    }
  }

  /**
   * Actually resolve the given exception that got thrown during on handler execution,
   * returning a view(result) that represents a specific error page if appropriate.
   * <p>May be overridden in subclasses, in order to apply specific exception checks.
   * Note that this template method will be invoked <i>after</i> checking whether this
   * resolved applies ("mappedHandlers" etc), so an implementation may simply proceed
   * with its actual exception handling.
   *
   * @param request current HTTP request
   * @param handlerMethod the executed handler method, or {@code null} if none chosen at the time
   * of the exception (for example, if multipart resolution failed)
   * @param ex the exception that got thrown during handler execution
   * @return a corresponding ModelAndView to forward to, or {@code null} for default processing
   */
  @Nullable
  protected abstract Object handleInternal(
          RequestContext request, @Nullable HandlerMethod handlerMethod, Throwable ex) throws Exception;

}
