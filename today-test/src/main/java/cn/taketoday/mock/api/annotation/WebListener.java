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

package cn.taketoday.mock.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.mock.api.MockContextAttributeListener;
import cn.taketoday.mock.api.MockContextListener;
import cn.taketoday.mock.api.MockRequestAttributeListener;
import cn.taketoday.mock.api.MockRequestListener;
import cn.taketoday.mock.api.http.HttpSessionAttributeListener;
import cn.taketoday.mock.api.http.HttpSessionIdListener;
import cn.taketoday.mock.api.http.HttpSessionListener;

/**
 * This annotation is used to declare a WebListener.
 *
 * Any class annotated with WebListener must implement one or more of the
 * {@link MockContextListener}, {@link MockContextAttributeListener},
 * {@link MockRequestListener}, {@link MockRequestAttributeListener},
 * {@link HttpSessionListener}, or {@link HttpSessionAttributeListener}, or
 * {@link HttpSessionIdListener} interfaces.
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
