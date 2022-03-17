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

package cn.taketoday.format;

import java.util.Locale;

/**
 * Prints objects of type T for display.
 *
 * @param <T> the type of object this Printer prints
 * @author Keith Donald
 * @since 4.0
 */
@FunctionalInterface
public interface Printer<T> {

  /**
   * Print the object of type T for display.
   *
   * @param object the instance to print
   * @param locale the current user locale
   * @return the printed text string
   */
  String print(T object, Locale locale);

}
