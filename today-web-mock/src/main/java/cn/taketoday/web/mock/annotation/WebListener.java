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

import cn.taketoday.web.mock.http.HttpSessionAttributeListener;
import cn.taketoday.web.mock.http.HttpSessionIdListener;
import cn.taketoday.web.mock.http.HttpSessionListener;

/**
 * This annotation is used to declare a WebListener.
 *
 * Any class annotated with WebListener must implement one or more of the
 * {@link cn.taketoday.web.mock.ServletContextListener}, {@link cn.taketoday.web.mock.ServletContextAttributeListener},
 * {@link cn.taketoday.web.mock.ServletRequestListener}, {@link cn.taketoday.web.mock.ServletRequestAttributeListener},
 * {@link HttpSessionListener}, or {@link HttpSessionAttributeListener}, or
 * {@link HttpSessionIdListener} interfaces.
 *
 * @since Servlet 3.0
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebListener {
  /**
   * Description of the listener
   *
   * @return description of the listener
   */
  String value() default "";
}
