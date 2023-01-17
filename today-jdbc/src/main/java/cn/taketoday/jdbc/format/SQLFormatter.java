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

package cn.taketoday.jdbc.format;

/**
 * Formatter contract
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Steve Ebersole
 * @since 4.0 2022/9/12 19:20
 */
public interface SQLFormatter {
  String WHITESPACE = " \n\r\f\t";

  /**
   * Format the source SQL string.
   *
   * @param source The original SQL string
   * @return The formatted version
   */
  String format(String source);
}
