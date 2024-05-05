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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.mock.api.MockContainerInitializer;

/**
 * This annotation is used to declare the class types that a {@link MockContainerInitializer
 * ServletContainerInitializer} can handle.
 *
 * @see MockContainerInitializer
 * @since Servlet 3.0
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface HandlesTypes {

  /**
   * The classes in which a {@link MockContainerInitializer ServletContainerInitializer} has expressed
   * interest.
   *
   * <p>
   * If an implementation of <tt>ServletContainerInitializer</tt> specifies this annotation, the Servlet container must
   * pass the <tt>Set</tt> of application classes that extend, implement, or have been annotated with the class types
   * listed by this annotation to the {@link MockContainerInitializer#onStartup} method of the
   * ServletContainerInitializer (if no matching classes are found, <tt>null</tt> must be passed instead)
   *
   * @return the classes in which {@link MockContainerInitializer ServletContainerInitializer} has
   * expressed interest
   */
  Class<?>[] value();
}
