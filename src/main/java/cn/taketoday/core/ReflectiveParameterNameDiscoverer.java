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

package cn.taketoday.core;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

import cn.taketoday.lang.Constant;

/**
 * {@link ParameterNameDiscoverer} implementation which uses JDK 8's reflection facilities
 * for introspecting parameter names (based on the "-parameters" compiler flag).
 *
 * @author TODAY 2021/9/10 22:44
 * @see Parameter#getName()
 * @since 4.0
 */
public class ReflectiveParameterNameDiscoverer extends ParameterNameDiscoverer {

  @Override
  public String[] getParameterNames(Executable executable) {
    final Parameter[] parameters = executable.getParameters();
    if (parameters.length == 0) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    int i = 0;
    String[] parameterNames = null;
    for (final Parameter parameter : parameters) {
      if (parameter.isNamePresent()) {
        if (parameterNames == null) {
          parameterNames = new String[parameters.length];
        }
        parameterNames[i++] = parameter.getName();
      }
      else {
        return null;
      }
    }
    return parameterNames;
  }

}
