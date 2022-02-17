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

package cn.taketoday.format.number;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import cn.taketoday.format.Formatter;

/**
 * Abstract formatter for Numbers,
 * providing a {@link #getNumberFormat(Locale)} template method.
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @since 4.0
 */
public abstract class AbstractNumberFormatter implements Formatter<Number> {

  private boolean lenient = false;

  /**
   * Specify whether or not parsing is to be lenient. Default is false.
   * <p>With lenient parsing, the parser may allow inputs that do not precisely match the format.
   * With strict parsing, inputs must match the format exactly.
   */
  public void setLenient(boolean lenient) {
    this.lenient = lenient;
  }

  @Override
  public String print(Number number, Locale locale) {
    return getNumberFormat(locale).format(number);
  }

  @Override
  public Number parse(String text, Locale locale) throws ParseException {
    NumberFormat format = getNumberFormat(locale);
    ParsePosition position = new ParsePosition(0);
    Number number = format.parse(text, position);
    if (position.getErrorIndex() != -1) {
      throw new ParseException(text, position.getIndex());
    }
    if (!this.lenient) {
      if (text.length() != position.getIndex()) {
        // indicates a part of the string that was not parsed
        throw new ParseException(text, position.getIndex());
      }
    }
    return number;
  }

  /**
   * Obtain a concrete NumberFormat for the specified locale.
   *
   * @param locale the current locale
   * @return the NumberFormat instance (never {@code null})
   */
  protected abstract NumberFormat getNumberFormat(Locale locale);

}
