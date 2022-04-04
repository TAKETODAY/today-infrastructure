/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.core.JavaVersion;

/**
 * {@link Conditional @Conditional} that matches based on the JVM version the application
 * is running on.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/4 12:22
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnJavaCondition.class)
public @interface ConditionalOnJava {

  /**
   * Configures whether the value configured in {@link #value()} shall be considered the
   * upper exclusive or lower inclusive boundary. Defaults to
   * {@link Range#EQUAL_OR_NEWER}.
   *
   * @return the range
   */
  Range range() default Range.EQUAL_OR_NEWER;

  /**
   * The {@link JavaVersion} to check for. Use {@link #range()} to specify whether the
   * configured value is an upper-exclusive or lower-inclusive boundary.
   *
   * @return the java version
   */
  JavaVersion value();

  /**
   * Range options.
   */
  enum Range {

    /**
     * Equal to, or newer than the specified {@link JavaVersion}.
     */
    EQUAL_OR_NEWER,

    /**
     * Older than the specified {@link JavaVersion}.
     */
    OLDER_THAN

  }

}
