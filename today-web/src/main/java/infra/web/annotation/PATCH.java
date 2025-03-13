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
import infra.http.HttpMethod;

/**
 * Annotation for mapping HTTP {@code PATCH} requests onto specific handler
 * methods.
 *
 * <p>Specifically, {@code @PATCH} is a <em>composed annotation</em> that
 * acts as a shortcut for {@code @RequestMapping(method = HttpMethod.PATCH)}.
 *
 * @author TODAY 2018-11-17 21:24
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping(method = HttpMethod.PATCH)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface PATCH {

  /**
   * Alias for {@link RequestMapping#name}.
   */
  @AliasFor(annotation = RequestMapping.class)
  String name() default "";

  /**
   * Alias for {@link RequestMapping#value}.
   */
  @AliasFor(annotation = RequestMapping.class)
  String[] value() default {};

  /**
   * Alias for {@link RequestMapping#path}.
   */
  @AliasFor(annotation = RequestMapping.class)
  String[] path() default {};

  /**
   * Combine this condition with another such as conditions from a
   * type-level and method-level {@code @RequestMapping} annotation.
   */
  @AliasFor(annotation = RequestMapping.class)
  boolean combine() default true;

  /**
   * Alias for {@link RequestMapping#params}.
   */
  @AliasFor(annotation = RequestMapping.class)
  String[] params() default {};

  /**
   * Alias for {@link RequestMapping#headers}.
   */
  @AliasFor(annotation = RequestMapping.class)
  String[] headers() default {};

  /**
   * Alias for {@link RequestMapping#consumes}.
   */
  @AliasFor(annotation = RequestMapping.class)
  String[] consumes() default {};

  /**
   * Alias for {@link RequestMapping#produces}.
   */
  @AliasFor(annotation = RequestMapping.class)
  String[] produces() default {};

  /**
   * Alias for {@link RequestMapping#version()}.
   */
  @AliasFor(annotation = RequestMapping.class)
  String version() default "";

}
