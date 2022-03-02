/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.bind.resolver;

import cn.taketoday.core.MethodParameter;

/**
 * Parameter format error like number-format error
 *
 * @author TODAY 2021/4/8 17:45
 * @since 3.0
 */
public class ParameterFormatException extends MethodParameterResolvingException {

  public ParameterFormatException(MethodParameter parameter) {
    super(parameter);
  }

  public ParameterFormatException(MethodParameter parameter, Throwable cause) {
    super(parameter, null, cause);
  }

  public ParameterFormatException(MethodParameter parameter, String message) {
    super(parameter, message, null);
  }

  public ParameterFormatException(MethodParameter parameter, String message, Throwable cause) {
    super(parameter, message, cause);
  }

}
