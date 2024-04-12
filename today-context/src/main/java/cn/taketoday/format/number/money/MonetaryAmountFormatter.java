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

package cn.taketoday.format.number.money;

import java.util.Locale;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;

import cn.taketoday.format.Formatter;
import cn.taketoday.lang.Nullable;

/**
 * Formatter for JSR-354 {@link javax.money.MonetaryAmount} values,
 * delegating to {@link javax.money.format.MonetaryAmountFormat#format}
 * and {@link javax.money.format.MonetaryAmountFormat#parse}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getMonetaryAmountFormat
 * @since 4.0
 */
public class MonetaryAmountFormatter implements Formatter<MonetaryAmount> {

  @Nullable
  private String formatName;

  /**
   * Create a locale-driven MonetaryAmountFormatter.
   */
  public MonetaryAmountFormatter() { }

  /**
   * Create a new MonetaryAmountFormatter for the given format name.
   *
   * @param formatName the format name, to be resolved by the JSR-354
   * provider at runtime
   */
  public MonetaryAmountFormatter(String formatName) {
    this.formatName = formatName;
  }

  /**
   * Specify the format name, to be resolved by the JSR-354 provider
   * at runtime.
   * <p>Default is none, obtaining a {@link MonetaryAmountFormat}
   * based on the current locale.
   */
  public void setFormatName(String formatName) {
    this.formatName = formatName;
  }

  @Override
  public String print(MonetaryAmount object, Locale locale) {
    return getMonetaryAmountFormat(locale).format(object);
  }

  @Override
  public MonetaryAmount parse(String text, Locale locale) {
    return getMonetaryAmountFormat(locale).parse(text);
  }

  /**
   * Obtain a MonetaryAmountFormat for the given locale.
   * <p>The default implementation simply calls
   * {@link javax.money.format.MonetaryFormats#getAmountFormat}
   * with either the configured format name or the given locale.
   *
   * @param locale the current locale
   * @return the MonetaryAmountFormat (never {@code null})
   * @see #setFormatName
   */
  protected MonetaryAmountFormat getMonetaryAmountFormat(Locale locale) {
    if (this.formatName != null) {
      return MonetaryFormats.getAmountFormat(this.formatName);
    }
    else {
      return MonetaryFormats.getAmountFormat(locale);
    }
  }

}
