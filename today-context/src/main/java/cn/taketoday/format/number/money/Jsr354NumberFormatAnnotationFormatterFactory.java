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

import java.text.ParseException;
import java.util.Collections;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;

import cn.taketoday.context.support.EmbeddedValueResolutionSupport;
import cn.taketoday.format.AnnotationFormatterFactory;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.Parser;
import cn.taketoday.format.Printer;
import cn.taketoday.format.annotation.NumberFormat;
import cn.taketoday.format.annotation.NumberFormat.Style;
import cn.taketoday.format.number.CurrencyStyleFormatter;
import cn.taketoday.format.number.NumberStyleFormatter;
import cn.taketoday.format.number.PercentStyleFormatter;
import cn.taketoday.util.StringUtils;

/**
 * Formats {@link javax.money.MonetaryAmount} fields annotated
 * with Framework's common {@link NumberFormat} annotation.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NumberFormat
 * @since 4.0
 */
public class Jsr354NumberFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
        implements AnnotationFormatterFactory<NumberFormat> {

  private static final String CURRENCY_CODE_PATTERN = "\u00A4\u00A4";

  @Override
  public Set<Class<?>> getFieldTypes() {
    return Collections.singleton(MonetaryAmount.class);
  }

  @Override
  public Printer<MonetaryAmount> getPrinter(NumberFormat annotation, Class<?> fieldType) {
    return configureFormatterFrom(annotation);
  }

  @Override
  public Parser<MonetaryAmount> getParser(NumberFormat annotation, Class<?> fieldType) {
    return configureFormatterFrom(annotation);
  }

  private Formatter<MonetaryAmount> configureFormatterFrom(NumberFormat annotation) {
    String pattern = resolveEmbeddedValue(annotation.pattern());
    if (StringUtils.isNotEmpty(pattern)) {
      return new PatternDecoratingFormatter(pattern);
    }
    else {
      Style style = annotation.style();
      if (style == Style.NUMBER) {
        return new NumberDecoratingFormatter(new NumberStyleFormatter());
      }
      else if (style == Style.PERCENT) {
        return new NumberDecoratingFormatter(new PercentStyleFormatter());
      }
      else {
        return new NumberDecoratingFormatter(new CurrencyStyleFormatter());
      }
    }
  }

  private record NumberDecoratingFormatter(Formatter<Number> numberFormatter) implements Formatter<MonetaryAmount> {

    @Override
    public String print(MonetaryAmount object, Locale locale) {
      return this.numberFormatter.print(object.getNumber(), locale);
    }

    @Override
    public MonetaryAmount parse(String text, Locale locale) throws ParseException {
      CurrencyUnit currencyUnit = Monetary.getCurrency(locale);
      Number numberValue = this.numberFormatter.parse(text, locale);
      return Monetary.getDefaultAmountFactory().setNumber(numberValue).setCurrency(currencyUnit).create();
    }
  }

  private record PatternDecoratingFormatter(String pattern) implements Formatter<MonetaryAmount> {

    @Override
    public String print(MonetaryAmount object, Locale locale) {
      CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();
      formatter.setCurrency(Currency.getInstance(object.getCurrency().getCurrencyCode()));
      formatter.setPattern(this.pattern);
      return formatter.print(object.getNumber(), locale);
    }

    @Override
    public MonetaryAmount parse(String text, Locale locale) throws ParseException {
      CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();
      Currency currency = determineCurrency(text, locale);
      CurrencyUnit currencyUnit = Monetary.getCurrency(currency.getCurrencyCode());
      formatter.setCurrency(currency);
      formatter.setPattern(this.pattern);
      Number numberValue = formatter.parse(text, locale);
      return Monetary.getDefaultAmountFactory().setNumber(numberValue).setCurrency(currencyUnit).create();
    }

    private Currency determineCurrency(String text, Locale locale) {
      try {
        if (text.length() < 3) {
          // Could not possibly contain a currency code ->
          // try with locale and likely let it fail on parse.
          return Currency.getInstance(locale);
        }
        else if (this.pattern.startsWith(CURRENCY_CODE_PATTERN)) {
          return Currency.getInstance(text.substring(0, 3));
        }
        else if (this.pattern.endsWith(CURRENCY_CODE_PATTERN)) {
          return Currency.getInstance(text.substring(text.length() - 3));
        }
        else {
          // A pattern without a currency code...
          return Currency.getInstance(locale);
        }
      }
      catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("Cannot determine currency for number value [" + text + "]", ex);
      }
    }
  }

}
