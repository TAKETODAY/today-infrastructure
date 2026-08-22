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

import java.util.ArrayList;

import infra.beans.factory.BeanFactoryUtils;
import infra.context.ApplicationContext;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.util.CollectionUtils;
import infra.web.handler.CompositeHandlerExceptionHandler;
import infra.web.handler.SimpleHandlerExceptionHandler;
import infra.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;

/**
 * Strategy interface for handling exceptions thrown by handlers during request
 * processing, such as exceptions raised while invoking a controller method.
 *
 * <p>Implementations may resolve the exception to a view or response result, or
 * return {@code null} to signal that the exception remains unresolved and should
 * be propagated to the caller.
 *
 * <p>When no {@code HandlerExceptionHandler} bean is registered, {@link #find(ApplicationContext)}
 * composes an {@link ExceptionHandlerAnnotationExceptionHandler} (for
 * {@code @ExceptionHandler} annotated methods, including {@code @ControllerAdvice}
 * beans) with a {@link SimpleHandlerExceptionHandler} (for well-known HTTP
 * exceptions such as 404 Not Found).
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DispatcherHandler#processHandlerException(HttpContext, Object, Throwable)
 * @see CompositeHandlerExceptionHandler
 * @see ExceptionHandlerAnnotationExceptionHandler
 * @see SimpleHandlerExceptionHandler
 * @since 2020-03-29 20:52
 */
public interface HandlerExceptionHandler {

  /**
   * Well-known name for the {@code HandlerExceptionHandler} object in the bean
   * factory for this namespace. Only used when
   * {@code "detectAllHandlerExceptionHandlers"} is turned off.
   *
   * @see DispatcherHandler#setDetectAllHandlerExceptionHandlers(boolean)
   */
  String BEAN_NAME = "handlerExceptionHandler";

  /**
   * Sentinel value indicating that the exception was fully handled and no
   * further view resolution or result rendering is required.
   *
   * <p>Unlike {@code null}, this value signals a definitive outcome of the
   * resolution chain rather than a delegate to subsequent handlers.
   *
   * @see DispatcherHandler#processHandlerException(HttpContext, Object, Throwable)
   */
  Object NONE_RETURN_VALUE = HttpRequestHandler.NONE_RETURN_VALUE;

  /**
   * Resolve the given exception raised during handler execution.
   *
   * @param context the current HTTP request/response context
   * @param exception the exception thrown during handler execution
   * @param handler the handler that was executing at the time of the exception,
   * or {@code null} if no handler had been selected yet
   * @return a result to render, or {@link #NONE_RETURN_VALUE} if the exception
   * was fully handled without a renderable result, or {@code null} if this
   * handler cannot resolve the exception
   * @throws Exception if resolution fails and the exception must be propagated
   */
  @Nullable
  Object handleException(HttpContext context, Throwable exception, @Nullable Object handler)
          throws Exception;

  // static factory method

  /**
   * Find a {@link HandlerExceptionHandler} from the given application context,
   * delegating to {@link #find(ApplicationContext, boolean)} with
   * {@code detectAllHandlerExceptionHandlers} set to {@code true}.
   *
   * @param context the application context to search
   * @return a handler exception handler, never {@code null}
   */
  static HandlerExceptionHandler find(ApplicationContext context) {
    return find(context, true);
  }

  /**
   * Find a {@link HandlerExceptionHandler} from the given application context.
   *
   * <p>When {@code detectAllHandlerExceptionHandlers} is {@code true}, all
   * {@code HandlerExceptionHandler} beans (including those in ancestor
   * contexts) are collected and, if more than one is found, composed into a
   * {@link CompositeHandlerExceptionHandler} ordered by
   * {@link AnnotationAwareOrderComparator}. When {@code false}, only the bean
   * named {@link #BEAN_NAME} is considered.
   *
   * <p>If no bean is found, a default composite consisting of an
   * {@link ExceptionHandlerAnnotationExceptionHandler} and a
   * {@link SimpleHandlerExceptionHandler} is created and initialized.
   *
   * @param context the application context to search
   * @param detectAllHandlerExceptionHandlers whether to detect all beans, or
   * only the bean named {@link #BEAN_NAME}
   * @return a handler exception handler, never {@code null}
   */
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
