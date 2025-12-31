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

package infra.web.handler.result;

import org.jspecify.annotations.Nullable;

import infra.web.ReturnValueHandler;

/**
 * An extension of {@link ReturnValueHandler} that provides smarter handling
 * of return values and handlers. This interface allows for more granular
 * control over the support and processing of return values based on both
 * the handler and its return value.
 *
 * <p>Implementations of this interface can determine whether they support
 * a specific handler, return value, or a combination of both. This makes it
 * particularly useful in scenarios where the handling logic depends on both
 * the handler and the result it produces.
 *
 * <h3>Usage Example</h3>
 *
 * Below is an example of how a custom implementation of
 * {@code SmartReturnValueHandler} can be used to handle {@code void} or
 * {@code Void} return types:
 *
 * <pre>{@code
 * public class VoidReturnValueHandler implements SmartReturnValueHandler {
 *   private final ViewReturnValueHandler delegate;
 *
 *   public VoidReturnValueHandler(ViewReturnValueHandler delegate) {
 *     Assert.notNull(delegate, "ViewReturnValueHandler is required");
 *     this.delegate = delegate;
 *   }
 *
 *   @Override
 *   public boolean supportsReturnValue(Object returnValue) {
 *     return returnValue == null;
 *   }
 *
 *   @Override
 *   public boolean supportsHandler(Object handler, @Nullable Object returnValue) {
 *     if (returnValue == null) {
 *       HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
 *       return handlerMethod == null || handlerMethod.isReturn(void.class) || handlerMethod.isReturn(Void.class);
 *     }
 *     return false;
 *   }
 *
 *   @Override
 *   public void handleReturnValue(RequestContext context,
 *           @Nullable Object handler, @Nullable Object returnValue) throws Exception {
 *     BindingContext binding = context.getBinding();
 *     if (binding != null && binding.hasModelAndView()) {
 *       ModelAndView modelAndView = binding.getModelAndView();
 *       delegate.renderView(context, modelAndView);
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h3>Default Behavior</h3>
 *
 * By default, the {@link #supportsHandler(Object, Object)} method returns
 * {@code true} if either {@link #supportsHandler(Object)} or
 * {@link #supportsReturnValue(Object)} returns {@code true}. This behavior
 * can be overridden by implementations to provide custom logic.
 *
 * <p>Note: This interface is designed to work seamlessly with frameworks
 * that support handler-based request processing, such as web MVC frameworks.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ReturnValueHandler
 * @see ReturnValueHandler#supportsHandler(Object)
 * @see ReturnValueHandler#supportsReturnValue(Object)
 * @since 4.0 2022/4/24 22:26
 */
public interface SmartReturnValueHandler extends ReturnValueHandler {

  /**
   * Determines whether this {@link SmartReturnValueHandler} supports the given handler
   * and return value combination.
   * <p>
   * This method checks if the provided handler or return value is supported by invoking
   * {@link #supportsHandler(Object)} and {@link #supportsReturnValue(Object)}. It returns
   * {@code true} if either of these methods indicates support.
   * <p>
   * This is useful in scenarios where a handler or its return value needs to be validated
   * for compatibility with the current implementation.
   *
   * <p>Example usage:
   * <pre>{@code
   * SmartReturnValueHandler handler = new VoidReturnValueHandler(new ViewReturnValueHandler());
   * Object handlerObject = new MyHandler();
   * Object returnValue = null;
   *
   * boolean isSupported = handler.supportsHandler(handlerObject, returnValue);
   * if (isSupported) {
   *   // Proceed with handling the return value
   * }
   * }</pre>
   *
   * @param handler the handler object to be checked for support; may be {@code null}
   * @param returnValue the return value to be checked for support; may be {@code null}
   * @return {@code true} if this {@link SmartReturnValueHandler} supports the specified
   * handler or return value, {@code false} otherwise
   */
  default boolean supportsHandler(@Nullable Object handler, @Nullable Object returnValue) {
    return supportsHandler(handler) || supportsReturnValue(returnValue);
  }

  @Override
  default boolean supportsHandler(@Nullable Object handler) {
    return false;
  }

}
