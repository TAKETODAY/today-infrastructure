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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import infra.core.Ordered;
import infra.web.HandlerAdapter;
import infra.web.RequestContext;
import infra.web.WebContentGenerator;

/**
 * Abstract base class for {@link HandlerAdapter} implementations that support
 * handlers of type {@link HandlerMethod}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 22:46
 */
public abstract class AbstractHandlerMethodAdapter extends WebContentGenerator implements HandlerAdapter, Ordered {

  private int order = Ordered.LOWEST_PRECEDENCE;

  public AbstractHandlerMethodAdapter() {
    // no restriction of HTTP methods by default
    super(false);
  }

  /**
   * Specify the order value for this HandlerAdapter bean.
   * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
   *
   * @see Ordered#getOrder()
   */
  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  /**
   * This implementation expects the handler to be an {@link HandlerMethod}.
   *
   * @param handler the handler instance to check
   * @return whether this adapter can adapt the given handler
   */
  @Override
  public final boolean supports(Object handler) {
    return handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler);
  }

  /**
   * Given a handler method, return whether this adapter can support it.
   *
   * @param handlerMethod the handler method to check
   * @return whether this adapter can adapt the given method
   */
  protected abstract boolean supportsInternal(HandlerMethod handlerMethod);

  /**
   * This implementation expects the handler to be an {@link HandlerMethod}.
   */
  @Override
  @Nullable
  public final Object handle(RequestContext request, Object handler)
          throws Throwable {

    return handleInternal(request, (HandlerMethod) handler);
  }

  /**
   * Use the given handler method to handle the request.
   *
   * @param request current HTTP request
   * @param handlerMethod handler method to use. This object must have previously been passed to the
   * {@link #supportsInternal(HandlerMethod)} this interface, which must have returned {@code true}.
   * @return an object to render, or {@code null}
   * @throws Exception in case of errors
   */
  @Nullable
  protected abstract Object handleInternal(
          RequestContext request, HandlerMethod handlerMethod) throws Throwable;

}
