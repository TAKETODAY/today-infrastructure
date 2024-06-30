/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web;

import java.util.ArrayList;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.handler.CompositeHandlerExceptionHandler;
import cn.taketoday.web.handler.SimpleHandlerExceptionHandler;
import cn.taketoday.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;

/**
 * Handle Exception from handler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-03-29 20:52
 */
public interface HandlerExceptionHandler {

  /**
   * Well-known name for the HandlerExceptionHandler object in the bean factory for this namespace.
   * Only used when "detectAllHandlerExceptionHandlers" is turned off.
   *
   * @see DispatcherHandler#setDetectAllHandlerExceptionHandlers(boolean)
   */
  String BEAN_NAME = "handlerExceptionHandler";

  /**
   * This value indicates that the handler did not return a value, or the result
   * has been processed
   */
  Object NONE_RETURN_VALUE = HttpRequestHandler.NONE_RETURN_VALUE;

  /**
   * Handle exception
   *
   * @param exception The exception occurred
   * @param handler Current handler
   * @return a corresponding view result to write to,
   * or {@code null} for default processing in the resolution chain
   * @throws Exception error handle failed
   */
  @Nullable
  Object handleException(RequestContext context, Throwable exception, @Nullable Object handler)
          throws Exception;

  // static factory method

  static HandlerExceptionHandler find(ApplicationContext context) {
    return find(context, true);
  }

  static HandlerExceptionHandler find(ApplicationContext context, boolean detectAllHandlerExceptionHandlers) {
    if (detectAllHandlerExceptionHandlers) {
      // Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.
      var matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
              context, HandlerExceptionHandler.class, true, false);
      if (!matchingBeans.isEmpty()) {
        var handlers = new ArrayList<>(matchingBeans.values());
        // at least one exception-handler
        if (handlers.size() == 1) {
          return handlers.get(0);
        }
        else {
          // We keep HandlerExceptionHandlers in sorted order.
          AnnotationAwareOrderComparator.sort(handlers);
          return new CompositeHandlerExceptionHandler(handlers);
        }
      }
    }
    else {
      var exceptionHandler = BeanFactoryUtils.find(context, BEAN_NAME, HandlerExceptionHandler.class);
      if (exceptionHandler != null) {
        return exceptionHandler;
      }
    }

    var exceptionHandler = new ExceptionHandlerAnnotationExceptionHandler();
    exceptionHandler.setApplicationContext(context);
    exceptionHandler.afterPropertiesSet();
    return new CompositeHandlerExceptionHandler(CollectionUtils.newArrayList(
            exceptionHandler, new SimpleHandlerExceptionHandler()
    ));
  }

}
