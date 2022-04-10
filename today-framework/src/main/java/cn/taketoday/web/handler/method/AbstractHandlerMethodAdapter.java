/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.method;

import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebContentGenerator;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.view.ModelAndView;

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
  public final ModelAndView handle(RequestContext request, Object handler)
          throws Throwable {

    return handleInternal(request, (HandlerMethod) handler);
  }

  /**
   * Use the given handler method to handle the request.
   *
   * @param request current HTTP request
   * @param handlerMethod handler method to use. This object must have previously been passed to the
   * {@link #supportsInternal(HandlerMethod)} this interface, which must have returned {@code true}.
   * @return a ModelAndView object with the name of the view and the required model data,
   * or {@code null} if the request has been handled directly
   * @throws Exception in case of errors
   */
  @Nullable
  protected abstract ModelAndView handleInternal(
          RequestContext request, HandlerMethod handlerMethod) throws Throwable;

}
