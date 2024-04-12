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

import cn.taketoday.lang.Nullable;

/**
 * A general-purpose number formatter using NumberFormat's number style.
 *
 * <p>Delegates to {@link NumberFormat#getInstance(Locale)}.
 * Configures BigDecimal parsing so there is no loss in precision.
 * Allows configuration over the decimal number pattern.
 * The {@link #parse(String, Locale)} routine always returns a BigDecimal.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setPattern
 * @see #setLenient
 * @since 4.0
 */
public class NumberStyleFormatter extends AbstractNumberFormatter {

  @Nullable
  private String pattern;

  /**
   * Create a new NumberStyleFormatter without a pattern.
   */
  public NumberStyleFormatter() { }

  /**
   * Create a new NumberStyleFormatter with the specified pattern.
   *
   * @param pattern the format pattern
   * @see #setPattern
   */
  public NumberStyleFormatter(String pattern) {
    this.pattern = pattern;
  }

  /**
   * Specify the pattern to use to format number values.
   * If not specified, the default DecimalFormat pattern is used.
   *
   * @see DecimalFormat#applyPattern(String)
   */
  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  @Override
  public NumberFormat getNumberFormat(Locale locale) {
    NumberFormat format = NumberFormat.getInstance(locale);
    if (!(format instanceof DecimalFormat decimalFormat)) {
      if (this.pattern != null) {
        throw new IllegalStateException("Cannot support pattern for non-DecimalFormat: " + format);
      }
      return format;
    }
    decimalFormat.setParseBigDecimal(true);
    if (this.pattern != null) {
      decimalFormat.applyPattern(this.pattern);
    }
    return decimalFormat;
  }

}
