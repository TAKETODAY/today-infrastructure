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

package cn.taketoday.test.annotation;

import java.lang.reflect.Method;

import cn.taketoday.core.annotation.AnnotatedElementUtils;

/**
 * Collection of utility methods for working with Spring's core testing annotations.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class TestAnnotationUtils {

  /**
   * Get the {@code timeout} configured via the {@link Timed @Timed}
   * annotation on the supplied {@code method}.
   * <p>Negative configured values will be converted to {@code 0}.
   *
   * @return the configured timeout, or {@code 0} if the method is not
   * annotated with {@code @Timed}
   */
  public static long getTimeout(Method method) {
    Timed timed = AnnotatedElementUtils.findMergedAnnotation(method, Timed.class);
    return (timed == null ? 0 : Math.max(0, timed.millis()));
  }

  /**
   * Get the repeat count configured via the {@link Repeat @Repeat}
   * annotation on the supplied {@code method}.
   * <p>Non-negative configured values will be converted to {@code 1}.
   *
   * @return the configured repeat count, or {@code 1} if the method is
   * not annotated with {@code @Repeat}
   */
  public static int getRepeatCount(Method method) {
    Repeat repeat = AnnotatedElementUtils.findMergedAnnotation(method, Repeat.class);
    if (repeat == null) {
      return 1;
    }
    return Math.max(1, repeat.value());
  }

}
