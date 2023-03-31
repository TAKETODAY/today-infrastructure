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

/**
 * Annotation to bind a method parameter to a request attribute.
 *
 * <p>The main motivation is to provide convenient access to request attributes
 * from a controller method with an optional/required check and a cast to the
 * target method parameter type.
 *
 * @author Rossen Stoyanchev
 * @author TODAY
 * @see RequestMapping
 * @see SessionAttribute
 * @since 2019-02-16 11:34
 */
@Documented
@RequestParam
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestAttribute {

  /**
   * Alias for {@link #name}.
   */
  @AliasFor("name")
  String value() default "";

  /**
   * The name of the request attribute to bind to.
   * <p>The default name is inferred from the method parameter name.
   */
  @AliasFor("value")
  String name() default "";

  /**
   * Whether the request attribute is required.
   * <p>Defaults to {@code true}, leading to an exception being thrown if
   * the attribute is missing. Switch this to {@code false} if you prefer
   * a {@code null} or Java 8 {@code java.util.Optional} if the attribute
   * doesn't exist.
   */
  boolean required() default true;

}
