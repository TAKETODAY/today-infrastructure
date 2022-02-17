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

package cn.taketoday.format.number.money;

import java.util.Locale;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import cn.taketoday.format.Formatter;

/**
 * Formatter for JSR-354 {@link javax.money.CurrencyUnit} values,
 * from and to currency code Strings.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class CurrencyUnitFormatter implements Formatter<CurrencyUnit> {

  @Override
  public String print(CurrencyUnit object, Locale locale) {
    return object.getCurrencyCode();
  }

  @Override
  public CurrencyUnit parse(String text, Locale locale) {
    return Monetary.getCurrency(text);
  }

}
