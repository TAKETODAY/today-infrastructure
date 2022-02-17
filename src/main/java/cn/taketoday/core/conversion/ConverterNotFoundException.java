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

package cn.taketoday.core.conversion;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Nullable;

/**
 * Exception to be thrown when a suitable converter could not be found
 * in a given conversion service.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY 2021/3/22 12:08
 * @since 3.0
 */
public class ConverterNotFoundException extends ConversionException {

  @Nullable
  private final TypeDescriptor sourceType;

  private final TypeDescriptor targetType;

  /**
   * Create a new conversion executor not found exception.
   *
   * @param sourceType the source type requested to convert from
   * @param targetType the target type requested to convert to
   */
  public ConverterNotFoundException(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
    super("No converter found capable of converting from type [" + sourceType + "] to type [" + targetType + "]");
    this.sourceType = sourceType;
    this.targetType = targetType;
  }

  /**
   * Return the source type that was requested to convert from.
   */
  @Nullable
  public TypeDescriptor getSourceType() {
    return this.sourceType;
  }

  /**
   * Return the target type that was requested to convert to.
   */
  public TypeDescriptor getTargetType() {
    return this.targetType;
  }

}
