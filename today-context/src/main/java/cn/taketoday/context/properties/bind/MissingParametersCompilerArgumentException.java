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

package cn.taketoday.context.properties.bind;

import java.util.Set;

/**
 * Exception thrown to indicate that a class has not been compiled with
 * {@code -parameters}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class MissingParametersCompilerArgumentException extends RuntimeException {

  MissingParametersCompilerArgumentException(Set<Class<?>> faultyClasses) {
    super(message(faultyClasses));
  }

  private static String message(Set<Class<?>> faultyClasses) {
    StringBuilder message = new StringBuilder(String.format(
            "Constructor binding in a native image requires compilation with -parameters but the following classes were compiled without it:%n"));
    for (Class<?> faultyClass : faultyClasses) {
      message.append(String.format("\t%s%n", faultyClass.getName()));
    }
    return message.toString();
  }

}
