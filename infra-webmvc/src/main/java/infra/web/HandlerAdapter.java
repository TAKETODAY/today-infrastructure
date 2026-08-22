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

import java.util.ArrayList;
import java.util.List;

import infra.beans.factory.BeanFactoryUtils;
import infra.context.ApplicationContext;
import infra.core.Ordered;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.web.handler.HandlerAdapters;
import infra.web.handler.function.support.HandlerFunctionAdapter;
import infra.web.handler.method.RequestMappingHandlerAdapter;

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
 * {@link Ordered Ordered} interface to be able to specify
 * a sorting order (and thus a priority) for getting applied by the
 * {@code DispatcherHandler}. Non-Ordered instances get treated as lowest
 * priority.
 *
 * <p>
 * <b>Note:</b> A handler may specify a dedicated {@link HandlerAdapter} at
 * startup time through {@link HandlerAdapterProvider}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HandlerAdapterProvider
 * @see HandlerAdapters
 * @see RequestMappingHandlerAdapter
 * @see HandlerFunctionAdapter
 * @since 2019-12-08 20:23
 */
public interface HandlerAdapter {

  /**
   * Well-known name for the HandlerAdapter object in the bean factory for this namespace.
   * Only used when "detectAllHandlerAdapters" is turned off.
   *
   * @see DispatcherHandler#setDetectAllHandlerAdapters(boolean)
   */
  String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";

  /**
   * Sentinel value indicating that the handler did not return a value, or the
   * result has already been processed and no further rendering is required.
   *
   * @see #handle(HttpContext, Object)
   */
  Object NONE_RETURN_VALUE = HttpRequestHandler.NONE_RETURN_VALUE;

  /**
   * Given a handler instance, return whether this adapter can support it.
   * Typical adapters will base the decision on the handler type, and usually
   * only support one handler type each.
   *
   * <p>A typical implementation:
   * <pre>{@code
   * return (handler instanceof MyHandler);
   * }</pre>
   *
   * @param handler the handler object to check
   * @return {@code true} if this adapter can use the given handler
   */
  boolean supports(Object handler);

  /**
   * Use the given handler to handle this request. The workflow that is required
   * may vary widely.
   *
   * <p>The result is handled by a {@link ReturnValueHandler} unless it equals
   * {@link #NONE_RETURN_VALUE}.
   *
   * @param context current HTTP request context
   * @param handler the handler to use. This object must have previously been
   * passed to the {@link #supports(Object)} method of this interface, which
   * must have returned {@code true}.
   * @return an object to be rendered by a {@link ReturnValueHandler}, or
   * {@code null} if the request has been handled directly, or
   * {@link #NONE_RETURN_VALUE} if the result has already been processed
   * @throws Exception in case of errors
   * @see ReturnValueHandler
   */
  @Nullable
  Object handle(HttpContext context, Object handler) throws Exception;

  // static factory method

  /**
   * Create a composite {@link HandlerAdapter} that delegates to the given
   * adapters in order, returning the first non-{@code null} result.
   *
   * @param handlerAdapters the adapters to delegate to
   * @return a composite adapter, never {@code null}
   * @see HandlerAdapters
   */
  static HandlerAdapter of(List<HandlerAdapter> handlerAdapters) {
    return new HandlerAdapters(handlerAdapters.toArray(new HandlerAdapter[0]));
  }

  /**
   * Find a {@link HandlerAdapter} from the given application context,
   * delegating to {@link #find(ApplicationContext, boolean)} with
   * {@code detectAllHandlerAdapters} set to {@code true}.
   *
   * @param context the application context to search
   * @return a handler adapter, never {@code null}
   */
  static HandlerAdapter find(ApplicationContext context) {
    return find(context, true);
  }

  /**
   * Find a {@link HandlerAdapter} from the given application context.
   *
   * <p>When {@code detectAllHandlerAdapters} is {@code true}, all
   * {@code HandlerAdapter} beans (including those in ancestor contexts) are
   * collected and composed into a {@link HandlerAdapters} instance ordered by
   * {@link AnnotationAwareOrderComparator}. When {@code false}, only the bean
   * named {@link #HANDLER_ADAPTER_BEAN_NAME} is considered.
   *
   * <p>If no bean is found, a default composite consisting of a
   * {@link RequestMappingHandlerAdapter} and a {@link HandlerFunctionAdapter}
   * is created, ensuring at least some adapters are available.
   *
   * @param context the application context to search
   * @param detectAllHandlerAdapters whether to detect all beans, or only the
   * bean named {@link #HANDLER_ADAPTER_BEAN_NAME}
   * @return a handler adapter, never {@code null}
   */
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
