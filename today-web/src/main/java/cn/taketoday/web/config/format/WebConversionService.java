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

package cn.taketoday.web.config.format;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.taketoday.format.datetime.DateFormatter;
import cn.taketoday.format.datetime.DateFormatterRegistrar;
import cn.taketoday.format.datetime.standard.DateTimeFormatterRegistrar;
import cn.taketoday.format.number.NumberFormatAnnotationFormatterFactory;
import cn.taketoday.format.number.money.CurrencyUnitFormatter;
import cn.taketoday.format.number.money.Jsr354NumberFormatAnnotationFormatterFactory;
import cn.taketoday.format.number.money.MonetaryAmountFormatter;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.config.EnableWebMvc;

/**
 * {@link cn.taketoday.format.support.FormattingConversionService} dedicated to web
 * applications for formatting and converting values to/from the web.
 * <p>
 * This service replaces the default implementations provided by
 * {@link EnableWebMvc @EnableWebMvc}
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/15 13:00
 */
public class WebConversionService extends DefaultFormattingConversionService {

  /**
   * Create a new WebConversionService that configures formatters with the provided
   * date, time, and date-time formats, or registers the default if no custom format is
   * provided.
   *
   * @param dateTimeFormatters the formatters to use for date, time, and date-time
   * formatting
   */
  public WebConversionService(DateTimeFormatters dateTimeFormatters) {
    super(false);
    if (dateTimeFormatters.isCustomized()) {
      addFormatters(dateTimeFormatters);
    }
    else {
      addDefaultFormatters(this);
    }
  }

  private void addFormatters(DateTimeFormatters dateTimeFormatters) {
    addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());
    if (jsr354Present) {
      addFormatter(new CurrencyUnitFormatter());
      addFormatter(new MonetaryAmountFormatter());
      addFormatterForFieldAnnotation(new Jsr354NumberFormatAnnotationFormatterFactory());
    }
    registerJsr310(dateTimeFormatters);
    registerJavaDate(dateTimeFormatters);
  }

  private void registerJsr310(DateTimeFormatters dateTimeFormatters) {
    DateTimeFormatterRegistrar dateTime = new DateTimeFormatterRegistrar();
    configure(dateTimeFormatters::getDateFormatter, dateTime::setDateFormatter);
    configure(dateTimeFormatters::getTimeFormatter, dateTime::setTimeFormatter);
    configure(dateTimeFormatters::getDateTimeFormatter, dateTime::setDateTimeFormatter);
    dateTime.registerFormatters(this);
  }

  private void configure(Supplier<DateTimeFormatter> supplier, Consumer<DateTimeFormatter> consumer) {
    DateTimeFormatter formatter = supplier.get();
    if (formatter != null) {
      consumer.accept(formatter);
    }
  }

  private void registerJavaDate(DateTimeFormatters dateTimeFormatters) {
    DateFormatterRegistrar dateFormatterRegistrar = new DateFormatterRegistrar();
    String datePattern = dateTimeFormatters.getDatePattern();
    if (datePattern != null) {
      DateFormatter dateFormatter = new DateFormatter(datePattern);
      dateFormatterRegistrar.setFormatter(dateFormatter);
    }
    dateFormatterRegistrar.registerFormatters(this);
  }

}
