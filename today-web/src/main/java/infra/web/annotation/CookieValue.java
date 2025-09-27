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

package infra.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;
import infra.http.HttpCookie;
import infra.lang.Constant;

/**
 * Annotation which indicates that a method parameter should be bound to an HTTP
 * cookie.
 * <p>The method parameter may be declared as type {@link HttpCookie}
 * or as cookie value type (String, int, etc.).
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestMapping
 * @see RequestParam
 * @see RequestHeader
 * @since 2018-07-01 14:10:04
 */
@Documented
@RequestParam
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CookieValue {

  /**
   * Alias for {@link #name}.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "value")
  String value() default "";

  /**
   * The name of the cookie to bind to.
   *
   * @since 4.0
   */
  @AliasFor(annotation = RequestParam.class, attribute = "name")
  String name() default "";

  /**
   * Whether the cookie is required.
   * <p>Defaults to {@code true}, leading to an exception being thrown
   * if the cookie is missing in the request. Switch this to
   * {@code false} if you prefer a {@code null} value if the cookie is
   * not present in the request.
   * <p>Alternatively, provide a {@link #defaultValue}, which implicitly
   * sets this flag to {@code false}.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "required")
  boolean required() default true;

  /**
   * The default value to use as a fallback.
   * <p>Supplying a default value implicitly sets {@link #required} to
   * {@code false}.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "defaultValue")
  String defaultValue() default Constant.DEFAULT_NONE;

}
