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

package cn.taketoday.beans;

import java.beans.PropertyChangeEvent;

import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when no suitable editor or converter can be found for a bean property.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 18:06
 */
public class ConversionNotSupportedException extends TypeMismatchException {

  /**
   * Create a new ConversionNotSupportedException.
   *
   * @param propertyChangeEvent the PropertyChangeEvent that resulted in the problem
   * @param requiredType the required target type (or {@code null} if not known)
   * @param cause the root cause (may be {@code null})
   */
  public ConversionNotSupportedException(PropertyChangeEvent propertyChangeEvent, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
    super(propertyChangeEvent, requiredType, cause);
  }

  /**
   * Create a new ConversionNotSupportedException.
   *
   * @param value the offending value that couldn't be converted (may be {@code null})
   * @param requiredType the required target type (or {@code null} if not known)
   * @param cause the root cause (may be {@code null})
   */
  public ConversionNotSupportedException(@Nullable Object value, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
    super(value, requiredType, cause);
  }

}

