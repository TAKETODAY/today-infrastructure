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

package cn.taketoday.core.conversion.support;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * @author TODAY 2021/1/6 23:34
 * @since 3.0
 */
public class FunctionNumberConverter extends NumberConverter {

  final UnaryOperator<Number> convertFunction;
  final Function<String, Number> stringFunction;

  public FunctionNumberConverter(Class<?> type,
                                 Function<String, Number> stringFunction,
                                 UnaryOperator<Number> convertFunction) {
    super(type);
    this.stringFunction = stringFunction;
    this.convertFunction = convertFunction;
  }

  @Override
  protected Number convertNumber(Number source) {
    return convertFunction.apply(source);
  }

  @Override
  protected Number convertString(String source) {
    final String stringVal = source.trim();
    if (stringVal.isEmpty()) {
      return convertNull();
    }
    return stringFunction.apply(stringVal);
  }

}
