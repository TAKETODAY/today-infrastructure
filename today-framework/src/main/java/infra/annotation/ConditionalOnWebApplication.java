/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches when the application is a web
 * application. By default, any application will match, but it can be narrowed using
 * the {@link #type()} attribute.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/17 14:23
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Conditional(OnWebApplicationCondition.class)
public @interface ConditionalOnWebApplication {

  /**
   * The required type of the web application.
   *
   * @return the required web application type
   */
  Type type() default Type.ANY;

  /**
   * Available application types.
   */
  enum Type {

    /**
     * Any web application will match.
     */
    ANY,

    /**
     * Only netty-based web application will match.
     */
    NETTY,

    /**
     * Only reactive-based web application will match.
     */
    REACTIVE

  }

}
