/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.lang.Constant;

/**
 * Annotation which indicates that a method parameter should be bound to an HTTP
 * cookie.
 * <p>The method parameter may be declared as type {@link jakarta.servlet.http.Cookie}
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
  @AliasFor("name")
  String value() default "";

  /**
   * The name of the cookie to bind to.
   *
   * @since 4.0
   */
  @AliasFor("value")
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
  boolean required() default true;

  /**
   * The default value to use as a fallback.
   * <p>Supplying a default value implicitly sets {@link #required} to
   * {@code false}.
   */
  String defaultValue() default Constant.DEFAULT_NONE;

}
