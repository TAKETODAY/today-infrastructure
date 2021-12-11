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

package cn.taketoday.beans;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Exception thrown on a type mismatch when trying to set a bean property.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/12 00:06
 */
@SuppressWarnings("serial")
public class TypeMismatchException extends PropertyException {

  @Nullable
  private final String propertyName;

  @Nullable
  private final transient Object value;

  @Nullable
  private final Class<?> requiredType;

  /**
   * Create a new {@code TypeMismatchException} without a {@code PropertyChangeEvent}.
   *
   * @param value the offending value that couldn't be converted (may be {@code null})
   * @param requiredType the required target type (or {@code null} if not known)
   */
  public TypeMismatchException(
          @Nullable String propertyName, @Nullable Object value, @Nullable Class<?> requiredType) {
    this(propertyName, value, requiredType, null);
  }

  /**
   * Create a new {@code TypeMismatchException} without a {@code PropertyChangeEvent}.
   *
   * @param value the offending value that couldn't be converted (may be {@code null})
   * @param requiredType the required target type (or {@code null} if not known)
   * @param cause the root cause (may be {@code null})
   */
  public TypeMismatchException(@Nullable String propertyName, @Nullable Object value, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
    super("Failed to convert value of type '" + ClassUtils.getDescriptiveType(value) + "'" +
                    (requiredType != null ? " to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : ""),
            cause);
    this.value = value;
    this.propertyName = propertyName;
    this.requiredType = requiredType;
  }

  /**
   * Return the name of the affected property, if available.
   */
  @Nullable
  public String getPropertyName() {
    return this.propertyName;
  }

  /**
   * Return the offending value (may be {@code null}).
   */
  @Nullable
  public Object getValue() {
    return this.value;
  }

  /**
   * Return the required target type, if any.
   */
  @Nullable
  public Class<?> getRequiredType() {
    return this.requiredType;
  }

}
