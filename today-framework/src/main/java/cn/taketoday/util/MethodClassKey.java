/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.util;

import java.lang.reflect.Method;
import java.util.Objects;

import cn.taketoday.lang.Nullable;

/**
 * A common key class for a method against a specific target class,
 * including {@link #toString()} representation and {@link Comparable}
 * support (as suggested for custom {@code HashMap} keys as of Java 8).
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/9/11 18:11
 * @since 4.0
 */
public final class MethodClassKey implements Comparable<MethodClassKey> {

  private final Method method;

  @Nullable
  private final Class<?> targetClass;

  /**
   * Create a key object for the given method and target class.
   *
   * @param method the method to wrap (must not be {@code null})
   * @param targetClass the target class that the method will be invoked
   * on (may be {@code null} if identical to the declaring class)
   */
  public MethodClassKey(Method method, @Nullable Class<?> targetClass) {
    this.method = method;
    this.targetClass = targetClass;
  }

  @Override
  public int compareTo(MethodClassKey other) {
    int result = this.method.getName().compareTo(other.method.getName());
    if (result == 0) {
      result = this.method.toString().compareTo(other.method.toString());
      if (result == 0 && this.targetClass != null && other.targetClass != null) {
        result = this.targetClass.getName().compareTo(other.targetClass.getName());
      }
    }
    return result;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MethodClassKey otherKey)) {
      return false;
    }
    return (this.method.equals(otherKey.method) &&
            Objects.equals(this.targetClass, otherKey.targetClass));
  }

  @Override
  public int hashCode() {
    return this.method.hashCode() + (this.targetClass != null ? this.targetClass.hashCode() * 29 : 0);
  }

  @Override
  public String toString() {
    return this.method + (this.targetClass != null ? " on " + this.targetClass : "");
  }

}
