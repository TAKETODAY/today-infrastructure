/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.format.number;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

/**
 * A BigDecimal formatter for number values in currency style.
 *
 * <p>Delegates to {@link NumberFormat#getCurrencyInstance(Locale)}.
 * Configures BigDecimal parsing so there is no loss of precision.
 * Can apply a specified {@link RoundingMode} to parsed values.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setLenient
 * @see #setRoundingMode
 * @since 4.0
 */
public class CurrencyStyleFormatter extends AbstractNumberFormatter {

  private int fractionDigits = 2;

  @Nullable
  private RoundingMode roundingMode;

  @Nullable
  private Currency currency;

  @Nullable
  private String pattern;

  /**
   * Specify the desired number of fraction digits.
   * Default is 2.
   */
  public void setFractionDigits(int fractionDigits) {
    this.fractionDigits = fractionDigits;
  }

  /**
   * Specify the rounding mode to use for decimal parsing.
   * Default is {@link RoundingMode#UNNECESSARY}.
   */
  public void setRoundingMode(@Nullable RoundingMode roundingMode) {
    this.roundingMode = roundingMode;
  }

  /**
   * Specify the currency, if known.
   */
  public void setCurrency(@Nullable Currency currency) {
    this.currency = currency;
  }

  /**
   * Specify the pattern to use to format number values.
   * If not specified, the default DecimalFormat pattern is used.
   *
   * @see DecimalFormat#applyPattern(String)
   */
  public void setPattern(@Nullable String pattern) {
    this.pattern = pattern;
  }

  @Override
  public BigDecimal parse(String text, Locale locale) throws ParseException {
    BigDecimal decimal = (BigDecimal) super.parse(text, locale);
    if (this.roundingMode != null) {
      decimal = decimal.setScale(this.fractionDigits, this.roundingMode);
    }
    else {
      decimal = decimal.setScale(this.fractionDigits);
    }
    return decimal;
  }

  @Override
  protected NumberFormat getNumberFormat(Locale locale) {
    DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
    format.setParseBigDecimal(true);
    format.setMaximumFractionDigits(this.fractionDigits);
    format.setMinimumFractionDigits(this.fractionDigits);
    if (this.roundingMode != null) {
      format.setRoundingMode(this.roundingMode);
    }
    if (this.currency != null) {
      format.setCurrency(this.currency);
    }
    if (this.pattern != null) {
      format.applyPattern(this.pattern);
    }
    return format;
  }

}
