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
import infra.session.Session;

/**
 * Annotation to bind a method parameter to a session attribute.
 *
 * <p>The main motivation is to provide convenient access to existing, permanent
 * session attributes (e.g. user authentication object) with an optional/required
 * check and a cast to the target method parameter type.
 *
 * <p>For use cases that require adding or removing session attributes consider
 * injecting {@link Session} into the controller method.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestMapping
 * @see RequestAttribute
 * @since 2018-08-21 20:19
 */
@Documented
@RequestParam
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SessionAttribute {

  /**
   * Alias for {@link #name}.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "value")
  String value() default "";

  /**
   * The name of the session attribute to bind to.
   * <p>The default name is inferred from the method parameter name.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "name")
  String name() default "";

  /**
   * Whether the session attribute is required.
   * <p>Defaults to {@code true}, leading to an exception being thrown
   * if the attribute is missing in the session or there is no session.
   * Switch this to {@code false} if you prefer a {@code null} or Java 8
   * {@code java.util.Optional} if the attribute doesn't exist.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "required")
  boolean required() default true;

}
