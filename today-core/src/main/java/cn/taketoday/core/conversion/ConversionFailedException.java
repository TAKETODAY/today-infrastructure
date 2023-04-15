/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.conversion;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Exception to be thrown when an actual type conversion attempt fails.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY 2021/3/22 12:11
 * @since 3.0
 */
public class ConversionFailedException extends ConversionException {

  @Nullable
  private final TypeDescriptor sourceType;

  private final TypeDescriptor targetType;

  @Nullable
  private final Object value;

  /**
   * Create a new conversion exception.
   *
   * @param sourceType the value's original type
   * @param targetType the value's target type
   * @param value the value we tried to convert
   * @param cause the cause of the conversion failure
   */
  public ConversionFailedException(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType,
                                   @Nullable Object value, Throwable cause) {

    super("Failed to convert from type [" + sourceType + "] to type [" + targetType +
            "] for value '" + ObjectUtils.nullSafeConciseToString(value) + "'", cause);
    this.sourceType = sourceType;
    this.targetType = targetType;
    this.value = value;
  }

  /**
   * Return the source type we tried to convert the value from.
   */
  @Nullable
  public TypeDescriptor getSourceType() {
    return this.sourceType;
  }

  /**
   * Return the target type we tried to convert the value to.
   */
  public TypeDescriptor getTargetType() {
    return this.targetType;
  }

  /**
   * Return the offending value.
   */
  @Nullable
  public Object getValue() {
    return this.value;
  }

}
