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

import java.text.ParseException;
import java.util.Locale;

/**
 * Parses text strings to produce instances of T.
 *
 * @param <T> the type of object this Parser produces
 * @author Keith Donald
 * @since 4.0
 */
@FunctionalInterface
public interface Parser<T> {

  /**
   * Parse a text String to produce a T.
   *
   * @param text the text string
   * @param locale the current user locale
   * @return an instance of T
   * @throws ParseException when a parse exception occurs in a java.text parsing library
   * @throws IllegalArgumentException when a parse exception occurs
   */
  T parse(String text, Locale locale) throws ParseException;

}
