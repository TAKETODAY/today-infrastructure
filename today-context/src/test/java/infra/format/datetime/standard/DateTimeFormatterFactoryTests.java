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

package infra.format.datetime.standard;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.TimeZone;

import infra.format.annotation.DateTimeFormat.ISO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 * @author Sam Brannen
 */
class DateTimeFormatterFactoryTests {

  // Potential test timezone, both have daylight savings on October 21st
  private static final TimeZone ZURICH = TimeZone.getTimeZone("Europe/Zurich");
  private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

  // Ensure that we are testing against a timezone other than the default.
  private static final TimeZone TEST_TIMEZONE = ZURICH.equals(TimeZone.getDefault()) ? NEW_YORK : ZURICH;

  private DateTimeFormatterFactory factory = new DateTimeFormatterFactory();

  private LocalDateTime dateTime = LocalDateTime.of(2009, 10, 21, 12, 10, 00, 00);

  @Test
  void createDateTimeFormatter() {
    assertThat(factory.createDateTimeFormatter().toString()).isEqualTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).toString());
  }

  @Test
  void createDateTimeFormatterWithPattern() {
    factory = new DateTimeFormatterFactory("yyyyMMddHHmmss");
    DateTimeFormatter formatter = factory.createDateTimeFormatter();
    assertThat(formatter.format(dateTime)).isEqualTo("20091021121000");
  }

  @Test
  void createDateTimeFormatterWithNullFallback() {
    DateTimeFormatter formatter = factory.createDateTimeFormatter(null);
    assertThat(formatter).isNull();
  }

  @Test
  void createDateTimeFormatterWithFallback() {
    DateTimeFormatter fallback = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG);
    DateTimeFormatter formatter = factory.createDateTimeFormatter(fallback);
    assertThat(formatter).isSameAs(fallback);
  }

  @Test
  void createDateTimeFormatterInOrderOfPropertyPriority() {
    factory.setStylePattern("SS");
    String value = applyLocale(factory.createDateTimeFormatter()).format(dateTime);
    // \p{Zs} matches any Unicode space character
    assertThat(value).startsWith("10/21/09").matches(".+?12:10\\p{Zs}PM");

    factory.setIso(ISO.DATE);
    assertThat(applyLocale(factory.createDateTimeFormatter()).format(dateTime)).isEqualTo("2009-10-21");

    factory.setPattern("yyyyMMddHHmmss");
    assertThat(factory.createDateTimeFormatter().format(dateTime)).isEqualTo("20091021121000");
  }

  @Test
  void createDateTimeFormatterWithTimeZone() {
    factory.setPattern("yyyyMMddHHmmss Z");
    factory.setTimeZone(TEST_TIMEZONE);
    ZoneId dateTimeZone = TEST_TIMEZONE.toZoneId();
    ZonedDateTime dateTime = ZonedDateTime.of(2009, 10, 21, 12, 10, 00, 00, dateTimeZone);
    String offset = (TEST_TIMEZONE.equals(NEW_YORK) ? "-0400" : "+0200");
    assertThat(factory.createDateTimeFormatter().format(dateTime)).isEqualTo("20091021121000 " + offset);
  }

  private DateTimeFormatter applyLocale(DateTimeFormatter dateTimeFormatter) {
    return dateTimeFormatter.withLocale(Locale.US);
  }

}
