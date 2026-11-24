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

package infra.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;
import infra.http.HttpStatus;
import infra.lang.Constant;

/**
 * Marks a handler method, controller class, or exception class with the HTTP
 * status {@link #value} and optional {@link #reason} to apply to the response.
 *
 * <p>When declared on a handler method or controller class, the status code is
 * applied to the HTTP response when the handler method is invoked and overrides
 * status information set by other means, like {@code ResponseEntity} or
 * {@code "redirect:"}. When declared on an exception class, the status is
 * applied when the exception is resolved by the MVC exception handling chain.
 *
 * <p><strong>Warning</strong>: when using this annotation on an exception
 * class, or when setting a non-empty {@code reason}, one of the
 * {@link infra.web.HttpContext#sendError(int)} methods will be used. This
 * delegates to the configured error handling mechanism, which determines how
 * the error is ultimately rendered.
 *
 * <p>For a handler method, a non-empty {@code reason} also means that the
 * method's return value is ignored; it is not written as the normal response
 * body. The configured error handling mechanism may render an HTML error page,
 * a JSON error document, or another representation. Consequently,
 * {@code reason} should not be used to define a REST response body or a
 * machine-readable error format. For those cases, use an explicit
 * {@link infra.http.ResponseEntity} or an exception handler instead.
 *
 * <p>Note that a controller class may also be annotated with
 * {@code @ResponseStatus} and is then inherited by all {@code @RequestMapping}
 * methods.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-12-08 15:10
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface ResponseStatus {

  /**
   * Alias for {@link #code}.
   */
  @AliasFor("code")
  HttpStatus value() default HttpStatus.INTERNAL_SERVER_ERROR;

  /**
   * The status <em>code</em> to use for the response.
   * <p>Default is {@link HttpStatus#INTERNAL_SERVER_ERROR}, which should
   * typically be changed to something more appropriate.
   *
   * @see infra.web.HttpContext#setStatus(int)
   * @see infra.web.HttpContext#sendError(int)
   * @since 3.0
   */
  @AliasFor("value")
  HttpStatus code() default HttpStatus.INTERNAL_SERVER_ERROR;

  /**
   * The optional <em>reason</em> associated with the response status.
   * <p>Defaults to an empty string, which is ignored. When set to a non-empty
   * value, the framework invokes
   * {@link infra.web.HttpContext#sendError(int, String)} instead of merely
   * setting the status. The reason is passed to the configured error handling
   * mechanism and is not itself a response body. For a handler method, the
   * method's return value is ignored in this case.
   *
   * <p>The value may be used as a message code when a message source is
   * configured; resolution is performed for the current request when supported
   * by the MVC error handling path.
   *
   * @see infra.web.HttpContext#sendError(int, String)
   * @since 3.0
   */
  String reason() default Constant.BLANK;

}
