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

package cn.taketoday.web.mock.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.web.mock.DispatcherType;

/**
 * Annotation used to declare a servlet filter.
 *
 * <p>
 * This annotation is processed by the container at deployment time, and the corresponding filter applied to the
 * specified URL patterns, servlets, and dispatcher types.
 *
 * @see cn.taketoday.web.mock.Filter
 * @since Servlet 3.0
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebFilter {

  /**
   * The description of the filter
   *
   * @return the description of the filter
   */
  String description() default "";

  /**
   * The display name of the filter
   *
   * @return the display name of the filter
   */
  String displayName() default "";

  /**
   * The init parameters of the filter
   *
   * @return the init parameters of the filter
   */
  WebInitParam[] initParams() default {};

  /**
   * The name of the filter
   *
   * @return the name of the filter
   */
  String filterName() default "";

  /**
   * The small-icon of the filter
   *
   * @return the small-icon of the filter
   */
  String smallIcon() default "";

  /**
   * The large-icon of the filter
   *
   * @return the large-icon of the filter
   */
  String largeIcon() default "";

  /**
   * The names of the servlets to which the filter applies.
   *
   * @return the names of the servlets to which the filter applies
   */
  String[] servletNames() default {};

  /**
   * The URL patterns to which the filter applies The default value is an empty array.
   *
   * @return the URL patterns to which the filter applies
   */
  String[] value() default {};

  /**
   * The URL patterns to which the filter applies
   *
   * @return the URL patterns to which the filter applies
   */
  String[] urlPatterns() default {};

  /**
   * The dispatcher types to which the filter applies
   *
   * @return the dispatcher types to which the filter applies
   */
  DispatcherType[] dispatcherTypes() default { DispatcherType.REQUEST };

  /**
   * Declares whether the filter supports asynchronous operation mode.
   *
   * @return {@code true} if the filter supports asynchronous operation mode
   * @see cn.taketoday.web.mock.ServletRequest#startAsync
   * @see cn.taketoday.web.mock.ServletRequest#startAsync(cn.taketoday.web.mock.ServletRequest, cn.taketoday.web.mock.ServletResponse)
   */
  boolean asyncSupported() default false;

}
