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

/**
 * Annotation used to declare a servlet.
 *
 * <p>
 * This annotation is processed by the container at deployment time, and the corresponding servlet made available at the
 * specified URL patterns.
 *
 * @see cn.taketoday.web.mock.Servlet
 * @since Servlet 3.0
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebServlet {

  /**
   * The name of the servlet
   *
   * @return the name of the servlet
   */
  String name() default "";

  /**
   * The URL patterns of the servlet
   *
   * @return the URL patterns of the servlet
   */
  String[] value() default {};

  /**
   * The URL patterns of the servlet
   *
   * @return the URL patterns of the servlet
   */
  String[] urlPatterns() default {};

  /**
   * The load-on-startup order of the servlet
   *
   * @return the load-on-startup order of the servlet
   */
  int loadOnStartup() default -1;

  /**
   * The init parameters of the servlet
   *
   * @return the init parameters of the servlet
   */
  WebInitParam[] initParams() default {};

  /**
   * Declares whether the servlet supports asynchronous operation mode.
   *
   * @return {@code true} if the servlet supports asynchronous operation mode
   * @see cn.taketoday.web.mock.ServletRequest#startAsync
   * @see cn.taketoday.web.mock.ServletRequest#startAsync(cn.taketoday.web.mock.ServletRequest, cn.taketoday.web.mock.ServletResponse)
   */
  boolean asyncSupported() default false;

  /**
   * The small-icon of the servlet
   *
   * @return the small-icon of the servlet
   */
  String smallIcon() default "";

  /**
   * The large-icon of the servlet
   *
   * @return the large-icon of the servlet
   */
  String largeIcon() default "";

  /**
   * The description of the servlet
   *
   * @return the description of the servlet
   */
  String description() default "";

  /**
   * The display name of the servlet
   *
   * @return the display name of the servlet
   */
  String displayName() default "";

}
