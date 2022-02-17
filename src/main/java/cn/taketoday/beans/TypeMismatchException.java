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

import java.beans.PropertyChangeEvent;

import cn.taketoday.lang.Assert;
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
public class TypeMismatchException extends PropertyAccessException {

  /**
   * Error code that a type mismatch error will be registered with.
   */
  public static final String ERROR_CODE = "typeMismatch";

  @Nullable
  private String propertyName;

  @Nullable
  private final transient Object value;

  @Nullable
  private final Class<?> requiredType;

  /**
   * Create a new {@code TypeMismatchException}.
   *
   * @param propertyChangeEvent the PropertyChangeEvent that resulted in the problem
   * @param requiredType the required target type
   */
  public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, Class<?> requiredType) {
    this(propertyChangeEvent, requiredType, null);
  }

  /**
   * Create a new {@code TypeMismatchException}.
   *
   * @param propertyChangeEvent the PropertyChangeEvent that resulted in the problem
   * @param requiredType the required target type (or {@code null} if not known)
   * @param cause the root cause (may be {@code null})
   */
  public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, @Nullable Class<?> requiredType,
                               @Nullable Throwable cause) {

    super(propertyChangeEvent,
            "Failed to convert property value of type '" +
                    ClassUtils.getDescriptiveType(propertyChangeEvent.getNewValue()) + "'" +
                    (requiredType != null ?
                     " to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : "") +
                    (propertyChangeEvent.getPropertyName() != null ?
                     " for property '" + propertyChangeEvent.getPropertyName() + "'" : ""),
            cause);
    this.propertyName = propertyChangeEvent.getPropertyName();
    this.value = propertyChangeEvent.getNewValue();
    this.requiredType = requiredType;
  }

  /**
   * Create a new {@code TypeMismatchException} without a {@code PropertyChangeEvent}.
   *
   * @param value the offending value that couldn't be converted (may be {@code null})
   * @param requiredType the required target type (or {@code null} if not known)
   * @see #initPropertyName
   */
  public TypeMismatchException(@Nullable Object value, @Nullable Class<?> requiredType) {
    this(value, requiredType, null);
  }

  /**
   * Create a new {@code TypeMismatchException} without a {@code PropertyChangeEvent}.
   *
   * @param value the offending value that couldn't be converted (may be {@code null})
   * @param requiredType the required target type (or {@code null} if not known)
   * @param cause the root cause (may be {@code null})
   * @see #initPropertyName
   */
  public TypeMismatchException(@Nullable Object value, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
    super("Failed to convert value of type '" + ClassUtils.getDescriptiveType(value) + "'" +
                    (requiredType != null ? " to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : ""),
            cause);
    this.value = value;
    this.requiredType = requiredType;
  }

  /**
   * Initialize this exception's property name for exposure through {@link #getPropertyName()},
   * as an alternative to having it initialized via a {@link PropertyChangeEvent}.
   *
   * @param propertyName the property name to expose
   * @see #TypeMismatchException(Object, Class)
   * @see #TypeMismatchException(Object, Class, Throwable)
   */
  public void initPropertyName(String propertyName) {
    Assert.state(this.propertyName == null, "Property name already initialized");
    this.propertyName = propertyName;
  }

  /**
   * Return the name of the affected property, if available.
   */
  @Override
  @Nullable
  public String getPropertyName() {
    return this.propertyName;
  }

  /**
   * Return the offending value (may be {@code null}).
   */
  @Override
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

  @Override
  public String getErrorCode() {
    return ERROR_CODE;
  }

}
