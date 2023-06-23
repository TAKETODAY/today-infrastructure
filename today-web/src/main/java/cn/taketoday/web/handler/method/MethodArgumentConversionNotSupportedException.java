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

package cn.taketoday.web.handler.method;

import cn.taketoday.beans.ConversionNotSupportedException;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Nullable;

/**
 * A ConversionNotSupportedException raised while resolving a method argument.
 * Provides access to the target {@link MethodParameter MethodParameter}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/17 11:16
 */
@SuppressWarnings("serial")
public class MethodArgumentConversionNotSupportedException extends ConversionNotSupportedException {

  private final String name;

  private final MethodParameter parameter;

  public MethodArgumentConversionNotSupportedException(@Nullable Object value,
          @Nullable Class<?> requiredType, String name, MethodParameter param, Throwable cause) {

    super(value, requiredType, cause);
    this.name = name;
    this.parameter = param;
    initPropertyName(name);
  }

  /**
   * Return the name of the method argument.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the target method parameter.
   */
  public MethodParameter getParameter() {
    return this.parameter;
  }

}
