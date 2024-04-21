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
import java.util.List;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.handler.HandlerAdapters;
import cn.taketoday.web.handler.SimpleNotFoundHandler;
import cn.taketoday.web.handler.function.support.HandlerFunctionAdapter;
import cn.taketoday.web.handler.method.RequestMappingHandlerAdapter;

/**
 * MVC framework SPI, allowing parameterization of the core MVC workflow.
 *
 * <p>
 * Interface that must be implemented for each handler type to handle a request.
 * This interface is used to allow the
 * {@link DispatcherHandler DispatcherHandler} to be
 * indefinitely extensible. The {@code DispatcherHandler} accesses all installed
 * handlers through this interface, meaning that it does not contain code
 * specific to any handler type.
 *
 * <p>
 * Note that a handler can be of type {@code Object}. This is to enable handlers
 * from other frameworks to be integrated with this framework without custom
 * coding, as well as to allow for annotation-driven handler objects that do not
 * obey any specific Java interface.
 *
 * <p>
 * This interface is not intended for application developers. It is available to
 * handlers who want to develop their own web workflow.
 *
 * <p>
 * Note: {@code HandlerAdapter} implementors may implement the
 * {@link cn.taketoday.core.Ordered Ordered} interface to be able to specify
 * a sorting order (and thus a priority) for getting applied by the
 * {@code DispatcherHandler}. Non-Ordered instances get treated as lowest
 * priority.
 *
 * <p>
 * <b>Note:</b> This framework allows use
 * {@link HandlerAdapterProvider HandlerAdapterCapable}
 * to specific a HandlerAdapter at startup time
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HandlerAdapterProvider
 * @see SimpleNotFoundHandler
 * @since 2019-12-08 20:23
 */
public interface HandlerAdapter {

  /**
   * Well-known name for the HandlerAdapter object in the bean factory for this namespace.
   * Only used when "detectAllHandlerAdapters" is turned off.
   *
   * @see DispatcherHandler#setDetectAllHandlerAdapters
   */
  String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";

  /**
   * This value indicates that the handler did not return a value, or the result
   * has been processed
   */
  Object NONE_RETURN_VALUE = HttpRequestHandler.NONE_RETURN_VALUE;

  /**
   * Given a handler instance, return whether support or not this
   * {@code RequestHandlerAdapter} can support it. Typical RequestHandlerAdapters
   * will base the decision on the handler type. RequestHandlerAdapters will
   * usually only support one handler type each.
   * <p>
   * A typical implementation:
   * <p>
   * {@code
   * return (handler instanceof MyHandler);
   * }
   *
   * @param handler handler object to check
   * @return whether support or not this object can use the given handler
   */
  boolean supports(Object handler);

  /**
   * Use the given handler to handle this request. The workflow that is required
   * may vary widely.
   * <p>
   * this result will handle by {@link ReturnValueHandler}
   * </p>
   *
   * @param context current HTTP request context
   * @param handler handler to use. This object must have previously been passed to
   * the {@code supports} method of this interface, which must have
   * returned {@code true}.
   * @return an object with the name of the view and the required model data, or
   * {@code null} if the request has been handled directly
   * @throws Throwable in case of errors
   * @see #NONE_RETURN_VALUE
   * @see ReturnValueHandler
   */
  @Nullable
  Object handle(RequestContext context, Object handler) throws Throwable;

  // static factory method

  static HandlerAdapter of(List<HandlerAdapter> handlerAdapters) {
    return new HandlerAdapters(handlerAdapters.toArray(new HandlerAdapter[0]));
  }

  static HandlerAdapter find(ApplicationContext context) {
    return find(context, true);
  }

  static HandlerAdapter find(ApplicationContext context, boolean detectAllHandlerAdapters) {
    if (detectAllHandlerAdapters) {
      // Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.
      var matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
              context, HandlerAdapter.class, true, false);
      if (!matchingBeans.isEmpty()) {
        var handlerAdapters = new ArrayList<>(matchingBeans.values());
        // We keep HandlerAdapters in sorted order.
        AnnotationAwareOrderComparator.sort(handlerAdapters);
        return new HandlerAdapters(handlerAdapters.toArray(new HandlerAdapter[0]));
      }
    }
    else {
      HandlerAdapter handlerAdapter = BeanFactoryUtils.find(
              context, HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
      if (handlerAdapter != null) {
        return handlerAdapter;
      }
    }

    // Ensure we have at least some HandlerAdapters, by registering
    // default HandlerAdapters if no other adapters are found.
    var handlerAdapter = context.getAutowireCapableBeanFactory()
            .createBean(RequestMappingHandlerAdapter.class);
    return new HandlerAdapters(
            new HandlerAdapter[] {
                    handlerAdapter,
                    new HandlerFunctionAdapter(),
            }
    );
  }

}
