/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.format.number;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * A formatter for number values in percent style.
 *
 * <p>Delegates to {@link NumberFormat#getPercentInstance(Locale)}.
 * Configures BigDecimal parsing so there is no loss in precision.
 * The {@link #parse(String, Locale)} routine always returns a BigDecimal.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setLenient
 * @since 4.0
 */
public class PercentStyleFormatter extends AbstractNumberFormatter {

  @Override
  protected NumberFormat getNumberFormat(Locale locale) {
    NumberFormat format = NumberFormat.getPercentInstance(locale);
    if (format instanceof DecimalFormat) {
      ((DecimalFormat) format).setParseBigDecimal(true);
    }
    return format;
  }

}
