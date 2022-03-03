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

package cn.taketoday.web.handler;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * Abstract base class for {@link HandlerExceptionHandler HandlerExceptionHandler}
 * implementations that support handling exceptions from handlers of type
 * {@link cn.taketoday.web.handler.method.ActionMappingAnnotationHandler}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 21:53
 */
public abstract class AbstractActionMappingMethodExceptionHandler extends AbstractHandlerExceptionHandler {

  /**
   * Checks if the handler is a {@link HandlerMethod} and then delegates to the
   * base class implementation of {@code #shouldApplyTo(HttpServletRequest, Object)}
   * passing the bean of the {@code HandlerMethod}. Otherwise returns {@code false}.
   */
  @Override
  protected boolean shouldApplyTo(RequestContext request, @Nullable Object handler) {
    if (handler == null) {
      return super.shouldApplyTo(request, null);
    }
    else if (handler instanceof ActionMappingAnnotationHandler annotationHandler) {
      handler = annotationHandler.getHandlerObject();
      return super.shouldApplyTo(request, handler);
    }
    else if (hasGlobalExceptionHandlers() && hasHandlerMappings()) {
      return super.shouldApplyTo(request, handler);
    }
    else {
      return false;
    }
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
    ActionMappingAnnotationHandler annotationHandler = (handler instanceof ActionMappingAnnotationHandler ? (ActionMappingAnnotationHandler) handler : null);
    return handleInternal(request, annotationHandler, ex);
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
          RequestContext request, @Nullable ActionMappingAnnotationHandler handlerMethod, Throwable ex) throws Exception;

}
