/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.agent;

import java.util.Objects;

import cn.taketoday.lang.Nullable;

/**
 * Reference to a Java method, identified by its owner class and the method name.
 *
 * <p>This implementation is ignoring parameters on purpose, as the goal here is
 * to inform developers on invocations requiring additional
 * {@link cn.taketoday.aot.hint.RuntimeHints} configuration, not
 * precisely identifying a method.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public final class MethodReference {

  private final String className;

  private final String methodName;

  private MethodReference(String className, String methodName) {
    this.className = className;
    this.methodName = methodName;
  }

  public static MethodReference of(Class<?> klass, String methodName) {
    return new MethodReference(klass.getCanonicalName(), methodName);
  }

  /**
   * Return the declaring class for this method.
   *
   * @return the declaring class name
   */
  public String getClassName() {
    return this.className;
  }

  /**
   * Return the name of the method.
   *
   * @return the method name
   */
  public String getMethodName() {
    return this.methodName;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MethodReference that = (MethodReference) o;
    return this.className.equals(that.className) && this.methodName.equals(that.methodName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.className, this.methodName);
  }

  @Override
  public String toString() {
    return this.className + '#' + this.methodName;
  }
}
