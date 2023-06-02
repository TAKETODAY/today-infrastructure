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
import cn.taketoday.web.HandlerInterceptor;

/**
 * Declarative interceptor configuration
 *
 * @author TODAY
 * @since 2018-11-17 21:23
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Interceptor {

  /**
   * configure Interceptors
   * <p>
   * The order of interceptors execution is related to the position of the interceptor
   */
  @AliasFor(attribute = "include")
  Class<? extends HandlerInterceptor>[] value() default {};

  /**
   * configure Interceptors
   * <p>
   * The order of interceptors execution is related to the position of the interceptor
   */
  @AliasFor(attribute = "value")
  Class<? extends HandlerInterceptor>[] include() default {};

  /**
   * configure Interceptors, use bean's name
   * <p>
   * The order of interceptors execution is related to the position of the interceptor
   *
   * <p>
   * this config add after {@link #include()}
   */
  String[] includeNames() default {};

  /**
   * Exclude {@link HandlerInterceptor}
   */
  Class<? extends HandlerInterceptor>[] exclude() default {};

  /**
   * Exclude HandlerInterceptor from bean's name
   */
  String[] excludeNames() default {};

}
