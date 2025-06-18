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

import infra.lang.Nullable;
import infra.web.handler.method.HandlerMethod;

/**
 * A HandlerInterceptor provides callback methods for pre-processing and post-processing
 * web requests in a web application. It is typically used to apply cross-cutting
 * concerns such as logging, security checks, modifying request/response data,
 * or handling CORS (Cross-Origin Resource Sharing).
 *
 * <p>Implementations of this interface can intercept requests before and after the
 * execution of a handler (e.g., a controller method) and optionally modify the
 * behavior of the handler or its result.
 *
 * <h3>Usage Examples</h3>
 *
 * <p>Here are some examples of how to implement and use a {@code HandlerInterceptor}:
 *
 * <h4>Example 1: CORS Interceptor</h4>
 * <pre>{@code
 * public class CorsInterceptor implements HandlerInterceptor {
 *   private final CorsConfigurationSource configSource;
 *   private CorsProcessor processor = new DefaultCorsProcessor();
 *
 *   public CorsInterceptor(CorsConfigurationSource configSource) {
 *     Assert.notNull(configSource, "CorsConfigurationSource is required");
 *     this.configSource = configSource;
 *   }
 *
 *   @Override
 *   public boolean beforeProcess(RequestContext request, Object handler) throws Throwable {
 *     CorsConfiguration corsConfiguration = configSource.getCorsConfiguration(request);
 *     return processor.process(corsConfiguration, request)
 *             && !request.isPreFlightRequest();
 *   }
 * }
 * }</pre>
 *
 * <h4>Example 2: Modifying Response Data</h4>
 * <pre>{@code
 * static class ModifyResult implements HandlerInterceptor {
 *
 *   @Override
 *   public void afterProcess(RequestContext request, Object handler, Object result) {
 *     if (result instanceof List list) {
 *       list.add(new TestBean("add"));
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h4>Example 3: Locale Change Interceptor</h4>
 * <pre>{@code
 * public class LocaleChangeInterceptor implements HandlerInterceptor {
 *
 *   private String paramName = "locale";
 *
 *   @Override
 *   public boolean beforeProcess(RequestContext request, Object handler) {
 *     String localeValue = request.getParameter(paramName);
 *     if (localeValue != null) {
 *       Locale locale = parseLocaleValue(localeValue);
 *       if (locale != null) {
 *         LocaleResolver localeResolver = getLocaleResolver();
 *         if (localeResolver != null) {
 *           localeResolver.setLocale(request, locale);
 *         }
 *       }
 *     }
 *     return true;
 *   }
 *
 *   protected Locale parseLocaleValue(String localeValue) {
 *     return StringUtils.parseLocale(localeValue);
 *   }
 * }
 * }</pre>
 *
 * <h3>Key Methods</h3>
 *
 * <p>{@link #beforeProcess(RequestContext, Object)}: Called before the handler processes
 * the request. Return {@code true} to allow the handler to execute, or {@code false}
 * to prevent further processing.
 *
 * <p>{@link #afterProcess(RequestContext, Object, Object)}: Called after the handler has
 * processed the request. Use this method to modify the result or perform cleanup.
 *
 * <p>{@link #intercept(RequestContext, InterceptorChain)}: Provides a hook for
 * implementing custom interception logic around the handler execution.
 *
 * <h3>Constants</h3>
 *
 * <p>{@link #EMPTY_ARRAY}: An empty array of {@code HandlerInterceptor}, useful for
 * initializing interceptor chains.
 *
 * <p>{@link #NONE_RETURN_VALUE}: A constant representing a "no-op" return value,
 * typically used when a handler should not proceed further.
 *
 * <h3>Integration with Framework</h3>
 *
 * <p>{@code HandlerInterceptor} is often used in conjunction with other components
 * like {@code MappedInterceptor} to apply interceptors conditionally based on URL
 * patterns or other criteria.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HandlerMethod#unwrap(Object)
 * @since 2018-06-25 20:06:11
 */
public interface HandlerInterceptor {

  /**
   * empty HandlerInterceptor array
   */
  HandlerInterceptor[] EMPTY_ARRAY = {};

  /**
   * NONE_RETURN_VALUE
   */
  Object NONE_RETURN_VALUE = HttpRequestHandler.NONE_RETURN_VALUE;

  /**
   * Executes custom logic before the request is processed by the handler.
   * This method can be used to perform pre-processing tasks such as validation,
   * logging, or modifying the request context.
   *
   * <p>Example usage:
   * <pre>{@code
   * public class CustomInterceptor implements HandlerInterceptor {
   *   @Override
   *   public boolean beforeProcess(RequestContext request, Object handler) throws Throwable {
   *     // Log the incoming request
   *     System.out.println("Processing request: " + request.getRequestURI());
   *
   *     // Perform custom validation
   *     if (handler instanceof HandlerMethod) {
   *       HandlerMethod handlerMethod = (HandlerMethod) handler;
   *       System.out.println("Handler method: " + handlerMethod.getMethod().getName());
   *     }
   *
   *     // Return true to continue processing, or false to stop
   *     return true;
   *   }
   * }
   * }</pre>
   *
   * @param request Current request context containing details about the request
   * @param handler The handler object that will process the request. This could
   * be a {@link HandlerMethod} or another
   * type of handler
   * @return true if the request should proceed to the handler, false otherwise
   * @throws Throwable If any exception occurs during the execution of this method
   * @see HandlerMethod#unwrap(Object)
   */
  default boolean beforeProcess(RequestContext request, Object handler) throws Throwable {
    return true;
  }

  /**
   * Executes custom logic after the request has been processed by the handler.
   * This method can be used to perform post-processing tasks such as logging,
   * modifying the result, or cleaning up resources.
   *
   * <p>Example usage:
   * <pre>{@code
   * public class CustomInterceptor implements HandlerInterceptor {
   *   @Override
   *   public void afterProcess(RequestContext request, Object handler,
   *                            @Nullable Object result) throws Throwable {
   *     // Log the result of the request processing
   *     System.out.println("Request processed with result: " + result);
   *
   *     // Perform additional operations based on the result
   *     if (result instanceof String) {
   *       System.out.println("Result is a String: " + result);
   *     }
   *
   *     // Modify the result if necessary
   *     if (result != null) {
   *       request.setAttribute("modifiedResult", result.toString().toUpperCase());
   *     }
   *   }
   * }
   * }</pre>
   *
   * @param request Current request context containing details about the request
   * @param handler The handler object that processed the request. This could
   * be a {@link HandlerMethod} or another type of handler
   * @param result The result returned by the handler after processing the request.
   * This may be {@code null} if the handler does not return a value
   * @throws Throwable If any exception occurs during the execution of this method
   */
  default void afterProcess(RequestContext request, Object handler, @Nullable Object result) throws Throwable {
  }

  /**
   * Intercepts the processing of a request by executing custom logic before and after
   * the request is handled. This method allows for pre-processing, modifying, or stopping
   * the request flow, as well as post-processing the result returned by the handler.
   *
   * <p>Example usage:
   * <pre>{@code
   * public class CustomInterceptor implements HandlerInterceptor {
   *   @Override
   *   public boolean beforeProcess(RequestContext request, Object handler) throws Throwable {
   *     System.out.println("Pre-processing request: " + request.getRequestURI());
   *     return true; // Continue processing
   *   }
   *
   *   @Override
   *   public void afterProcess(RequestContext request, Object handler,
   *                             @Nullable Object result) throws Throwable {
   *     if (result instanceof String) {
   *       System.out.println("Post-processing result: " + result);
   *     }
   *   }
   *
   *   @Override
   *   @Nullable
   *   public Object intercept(RequestContext request, InterceptorChain chain) throws Throwable {
   *     return HandlerInterceptor.super.intercept(request, chain);
   *   }
   * }
   * }</pre>
   *
   * @param request The current request context containing details about the request
   * @param chain The interceptor chain that allows proceeding to the next interceptor
   * or the final handler
   * @return The result of the request processing if {@link #beforeProcess} returns true;
   * otherwise, {@link HandlerInterceptor#NONE_RETURN_VALUE}. May be {@code null}
   * if the handler does not return a value
   * @throws Throwable If any exception occurs during the execution of this method or
   * within the interceptor chain
   */
  @Nullable
  default Object intercept(RequestContext request, InterceptorChain chain) throws Throwable {
    Object handler = chain.getHandler();
    if (beforeProcess(request, handler)) {
      Object result = chain.proceed(request);
      afterProcess(request, handler, result);
      return result;
    }
    return NONE_RETURN_VALUE;
  }

}
