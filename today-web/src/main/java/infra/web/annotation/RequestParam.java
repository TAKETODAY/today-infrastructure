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
import infra.lang.Constant;

/**
 * Annotation which indicates that a method parameter should be bound to a web request parameter.
 * <p>
 * this Annotation provides {@code NamedValueInfo}
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see infra.web.handler.method.NamedValueInfo
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
public @interface RequestParam {

  /**
   * Alias for {@link #name}.
   */
  @AliasFor("name")
  String value() default "";

  /**
   * The name of the request parameter to bind to.
   *
   * @since 4.0
   */
  @AliasFor("value")
  String name() default "";

  /**
   * Whether the parameter is required.
   * <p>Defaults to {@code false}, leading to an exception being thrown
   * if the parameter is missing in the request. Switch this to
   * {@code false} if you prefer a {@code null} value if the parameter is
   * not present in the request.
   * <p>Alternatively, provide a {@link #defaultValue}, which implicitly
   * sets this flag to {@code false}.
   * If required == true when request parameter is null, will be throws exception
   */
  boolean required() default true;

  /**
   * The default value to use as a fallback when the request parameter is
   * not provided or has an empty value.
   * <p>Supplying a default value implicitly sets {@link #required} to
   * {@code false}.
   */
  String defaultValue() default Constant.DEFAULT_NONE;

}
