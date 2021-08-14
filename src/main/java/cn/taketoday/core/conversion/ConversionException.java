/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.core.utils.GenericDescriptor;

/**
 * Base class for exceptions thrown by the conversion system.
 *
 * @author TODAY <br>
 * 2018-06-28 17:05:34
 */
public class ConversionException extends NestedRuntimeException {

  private static final long serialVersionUID = 1L;

  final Object source;
  final GenericDescriptor targetType;

  public ConversionException() {
    this(null, null, null, null);
  }

  public ConversionException(Throwable cause) {
    this(null, cause, null, null);
  }

  public ConversionException(String message, Throwable cause) {
    this(message, cause, null, null);
  }

  public ConversionException(String message) {
    this(message, null, null, null);
  }

  public ConversionException(String message, Throwable cause, Object source, GenericDescriptor targetType) {
    super(message, cause);
    this.source = source;
    this.targetType = targetType;
  }

  public Object getSource() {
    return source;
  }

  public GenericDescriptor getTargetType() {
    return targetType;
  }

}
