/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web;

import org.jspecify.annotations.Nullable;

import infra.web.handler.method.HandlerMethod;
import infra.web.handler.result.HandlerMethodReturnValueHandler;
import infra.web.handler.result.SmartReturnValueHandler;

/**
 * An interface for handling return values from HTTP request handlers.
 * Implementations of this interface are responsible for determining whether
 * they can support a given handler or its return value and for processing
 * the return value appropriately.
 *
 * <p>This interface provides methods to check support for handlers and return
 * values, as well as a method to process the return value. It also includes
 * a static utility method for selecting an appropriate {@link ReturnValueHandler}
 * from a list based on the handler and return value.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public class MyReturnValueHandler implements ReturnValueHandler {
 *
 *   @Override
 *   public boolean supportsHandler(Object handler) {
 *     // Check if the handler is supported
 *     return handler instanceof MyCustomHandler;
 *   }
 *
 *   @Override
 *   public boolean supportsReturnValue(Object returnValue) {
 *     // Check if the return value is supported
 *     return returnValue instanceof MyCustomResult;
 *   }
 *
 *   @Override
 *   public void handleReturnValue(RequestContext context, Object handler, Object returnValue)
 *           throws Exception {
 *     // Process the return value
 *     if (returnValue instanceof MyCustomResult result) {
 *       context.getResponse().write(result.toString());
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p><strong>SmartReturnValueHandler Example:</strong>
 * <pre>{@code
 * public class SmartHandler implements SmartReturnValueHandler {
 *
 *   @Override
 *   public boolean supportsHandler(Object handler, Object returnValue) {
 *     // Support both handler and return value together
 *     return handler instanceof MyCustomHandler && returnValue instanceof MyCustomResult;
 *   }
 *
 *   @Override
 *   public void handleReturnValue(RequestContext context, Object handler, Object returnValue)
 *           throws Exception {
 *     // Handle the return value
 *     context.getResponse().write("Handled by SmartHandler");
 *   }
 * }
 * }</pre>
 *
 * <p><strong>Handler Selection Example:</strong>
 * <pre>{@code
 * List<ReturnValueHandler> handlers = new ArrayList<>();
 * handlers.add(new MyReturnValueHandler());
 * handlers.add(new SmartHandler());
 *
 * Object handler = new MyCustomHandler();
 * Object returnValue = new MyCustomResult();
 *
 * ReturnValueHandler selectedHandler = ReturnValueHandler.select(handlers, handler, returnValue);
 * if (selectedHandler != null) {
 *   selectedHandler.handleReturnValue(context, handler, returnValue);
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see SmartReturnValueHandler
 * @see HandlerMethodReturnValueHandler
 * @see HandlerExceptionHandler
 * @see HandlerMethod
 * @since 4.0 2019-07-10 19:22
 */
public interface ReturnValueHandler {

  /**
   * A special constant value used to indicate that a handler has fully processed
   * the request and no further result handling is required. This value is typically
   * used in conjunction with {@link HttpRequestHandler} and {@link ReturnValueHandler}.
   *
   * <p>This value is returned by handlers when they have completed all necessary
   * processing, signaling to the framework that no additional {@link ReturnValueHandler}
   * should be invoked.
   *
   * <p><b>Usage Examples:</b>
   *
   * <pre>{@code
   *  // Example 1: Returning NONE_RETURN_VALUE to indicate no further processing
   *  HttpRequestHandler handler = request -> {
   *    if (request.getParameter("action").equals("logout")) {
   *      request.getSession().invalidate();
   *      return ReturnValueHandler.NONE_RETURN_VALUE; // No further processing needed
   *    }
   *    return null; // Let the framework handle the result
   *  };
   * }</pre>
   *
   * <pre>{@code
   *  // Example 2: Using NONE_RETURN_VALUE in ReturnValueHandler selection logic
   *  ReturnValueHandler selectedHandler = ReturnValueHandler.select(
   *      handlers,
   *      handler,
   *      ReturnValueHandler.NONE_RETURN_VALUE
   *  );
   *  if (selectedHandler == null) {
   *    System.out.println("No handler matched or no further processing required.");
   *  }
   * }</pre>
   *
   * <p><b>Note:</b> This value is a singleton instance of {@link Object} and should
   * only be referenced directly, not instantiated or compared using {@code ==}.
   *
   * @see HttpRequestHandler#NONE_RETURN_VALUE
   * @see ReturnValueHandler#supportsReturnValue(Object)
   * @see ReturnValueHandler#select(Iterable, Object, Object)
   */
  Object NONE_RETURN_VALUE = HttpRequestHandler.NONE_RETURN_VALUE;

  /**
   * Determines whether this {@link ReturnValueHandler} supports the given handler.
   * <p>
   * This method is typically used during application runtime to check if the current
   * {@link ReturnValueHandler} can handle the result or behavior of the specified handler.
   * <p>
   * For example, you might use this method to determine if a specific handler is compatible
   * with the current implementation:
   * <pre>{@code
   * ReturnValueHandler handler = ...;
   * Object targetHandler = ...;
   *
   * if (handler.supportsHandler(targetHandler)) {
   *   System.out.println("This ReturnValueHandler supports the target handler.");
   * }
   * else {
   *   System.out.println("This ReturnValueHandler does not support the target handler.");
   * }
   * }</pre>
   *
   * @param handler the target handler to be checked for support
   * @return true if this {@link ReturnValueHandler} supports the specified handler,
   * false otherwise
   */
  boolean supportsHandler(Object handler);

  /**
   * Determines whether this {@link ReturnValueHandler} supports the given return value.
   * <p>
   * This method is used to check if the current {@link ReturnValueHandler} can handle
   * the provided return value. It is typically invoked during the processing of a handler's
   * result to ensure compatibility with the return value type or content.
   *
   * @param returnValue the return value to be checked for support; may be {@code null}
   * @return {@code true} if this {@link ReturnValueHandler} supports the specified return value,
   * {@code false} otherwise
   */
  default boolean supportsReturnValue(@Nullable Object returnValue) {
    return false;
  }

  /**
   * Handles the return value produced by a request handler within the given context.
   * <p>
   * This method is responsible for processing the return value generated by a handler
   * in the context of a request. It ensures that the return value is appropriately
   * managed, transformed, or dispatched based on the implementation logic.
   *
   * @param context the {@link RequestContext} representing the current request context
   * @param handler the handler object associated with the request; may be {@code null}
   * @param returnValue the return value produced by the handler; may be {@code null}
   * @throws Exception if an error occurs while handling the return value
   */
  void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue)
          throws Exception;

  /**
   * Selects an appropriate {@link ReturnValueHandler} from the provided list based on the given
   * handler and return value. The selection process considers the following scenarios:
   * <ul>
   *   <li>If both handler and return value are provided, it attempts to match a handler that supports
   *       both.</li>
   *   <li>If only the return value is provided, it matches a handler that supports the return value.</li>
   *   <li>If only the handler is provided, it matches a handler that supports the handler.</li>
   * </ul>
   * <p>
   * If no matching handler is found, the method returns {@code null}.
   * <p>
   * Example usage:
   * <pre>{@code
   * List<ReturnValueHandler> handlers = ...;
   * Object handler = new MyHandler();
   * Object returnValue = "example";
   *
   * ReturnValueHandler selectedHandler = ReturnValueHandler.select(handlers, handler, returnValue);
   * if (selectedHandler != null) {
   *   System.out.println("Selected handler: " + selectedHandler);
   * }
   * else {
   *   System.out.println("No suitable handler found.");
   * }
   * }</pre>
   * <p>
   * In this example, the method will attempt to find a handler that supports both the
   * {@code MyHandler} instance and the string return value.
   *
   * @param handlers the Iterable of {@link ReturnValueHandler} candidates to evaluate; must not be null
   * @param handler the target handler to be checked for support; may be null
   * @param returnValue the return value to be checked for support; may be null
   * @return the first {@link ReturnValueHandler} that matches the criteria, or {@code null}
   * if no suitable handler is found
   */
  @Nullable
  static ReturnValueHandler select(Iterable<ReturnValueHandler> handlers,
          @Nullable Object handler, @Nullable Object returnValue) {
    if (returnValue != NONE_RETURN_VALUE) {
      if (handler != null) {
        // match handler and return-value
        for (ReturnValueHandler returnValueHandler : handlers) {
          if (returnValueHandler instanceof SmartReturnValueHandler smartHandler) {
            // smart handler
            if (smartHandler.supportsHandler(handler, returnValue)) {
              return returnValueHandler;
            }
          }
          else {
            if (returnValueHandler.supportsHandler(handler)
                    || returnValueHandler.supportsReturnValue(returnValue)) {
              return returnValueHandler;
            }
          }
        }
      }
      else {
        // match return-value only
        for (ReturnValueHandler returnValueHandler : handlers) {
          if (returnValueHandler.supportsReturnValue(returnValue)) {
            return returnValueHandler;
          }
        }
      }
    }
    else if (handler != null) {
      // match handler only
      for (ReturnValueHandler returnValueHandler : handlers) {
        if (returnValueHandler.supportsHandler(handler)) {
          return returnValueHandler;
        }
      }
    }
    return null;
  }

}
