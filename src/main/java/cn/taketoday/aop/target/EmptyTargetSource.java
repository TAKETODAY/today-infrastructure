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

package cn.taketoday.aop.target;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.aop.TargetSource;

/**
 * Canonical {@code TargetSource} when there is no target
 * (or just the target class known), and behavior is supplied
 * by interfaces and advisors only.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 20:25
 */
public final class EmptyTargetSource implements TargetSource, Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  //---------------------------------------------------------------------
  // Instance implementation
  //---------------------------------------------------------------------

  private final Class<?> targetClass;

  private final boolean isStatic;

  /**
   * Create a new instance of the {@link EmptyTargetSource} class.
   * <p>This constructor is {@code private} to enforce the
   * Singleton pattern / factory method pattern.
   *
   * @param targetClass the target class to expose (may be {@code null})
   * @param isStatic whether the TargetSource is marked as static
   */
  private EmptyTargetSource(Class<?> targetClass, boolean isStatic) {
    this.targetClass = targetClass;
    this.isStatic = isStatic;
  }

  /**
   * Always returns the specified target Class, or {@code null} if none.
   */
  @Override
  public Class<?> getTargetClass() {
    return this.targetClass;
  }

  /**
   * Always returns {@code true}.
   */
  @Override
  public boolean isStatic() {
    return this.isStatic;
  }

  /**
   * Always returns {@code null}.
   */
  @Override
  public Object getTarget() {
    return null;
  }

  /**
   * Returns the canonical instance on deserialization in case
   * of no target class, thus protecting the Singleton pattern.
   */
  @Serial
  private Object readResolve() {
    return (this.targetClass == null && this.isStatic ? INSTANCE : this);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof EmptyTargetSource otherTs)) {
      return false;
    }
    return isStatic == otherTs.isStatic
            && Objects.equals(targetClass, otherTs.targetClass);
  }

  @Override
  public int hashCode() {
    return EmptyTargetSource.class.hashCode() * 13 + Objects.hashCode(targetClass);
  }

  @Override
  public String toString() {
    return "EmptyTargetSource: " + (targetClass != null ? "target class [" +
            targetClass.getName() + "]" : "no target class") +
            ", " + (isStatic ? "static" : "dynamic");
  }

  //---------------------------------------------------------------------
  // Static factory methods
  //---------------------------------------------------------------------

  /**
   * The canonical (Singleton) instance of this {@link EmptyTargetSource}.
   */
  public static final EmptyTargetSource INSTANCE = new EmptyTargetSource(null, true);

  /**
   * Return an EmptyTargetSource for the given target Class.
   *
   * @param targetClass the target Class (may be {@code null})
   * @see #getTargetClass()
   */
  public static EmptyTargetSource forClass(Class<?> targetClass) {
    return forClass(targetClass, true);
  }

  /**
   * Return an EmptyTargetSource for the given target Class.
   *
   * @param targetClass the target Class (may be {@code null})
   * @param isStatic whether the TargetSource should be marked as static
   * @see #getTargetClass()
   */
  public static EmptyTargetSource forClass(Class<?> targetClass, boolean isStatic) {
    return (targetClass == null && isStatic ? INSTANCE : new EmptyTargetSource(targetClass, isStatic));
  }

}
