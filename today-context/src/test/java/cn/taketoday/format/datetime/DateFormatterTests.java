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

package cn.taketoday.format.datetime;

import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import cn.taketoday.format.annotation.DateTimeFormat.ISO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link DateFormatter}.
 *
 * @author Keith Donald
 * @author Phillip Webb
 */
class DateFormatterTests {

  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

  @Test
  void shouldPrintAndParseDefault() throws Exception {
    DateFormatter formatter = new DateFormatter();
    formatter.setTimeZone(UTC);

    Date date = getDate(2009, Calendar.JUNE, 1);
    assertThat(formatter.print(date, Locale.US)).isEqualTo("Jun 1, 2009");
    assertThat(formatter.parse("Jun 1, 2009", Locale.US)).isEqualTo(date);
  }

  @Test
  void shouldPrintAndParseFromPattern() throws ParseException {
    DateFormatter formatter = new DateFormatter("yyyy-MM-dd");
    formatter.setTimeZone(UTC);

    Date date = getDate(2009, Calendar.JUNE, 1);
    assertThat(formatter.print(date, Locale.US)).isEqualTo("2009-06-01");
    assertThat(formatter.parse("2009-06-01", Locale.US)).isEqualTo(date);
  }

  @Test
  void shouldPrintAndParseShort() throws Exception {
    DateFormatter formatter = new DateFormatter();
    formatter.setTimeZone(UTC);
    formatter.setStyle(DateFormat.SHORT);

    Date date = getDate(2009, Calendar.JUNE, 1);
    assertThat(formatter.print(date, Locale.US)).isEqualTo("6/1/09");
    assertThat(formatter.parse("6/1/09", Locale.US)).isEqualTo(date);
  }

  @Test
  void shouldPrintAndParseMedium() throws Exception {
    DateFormatter formatter = new DateFormatter();
    formatter.setTimeZone(UTC);
    formatter.setStyle(DateFormat.MEDIUM);

    Date date = getDate(2009, Calendar.JUNE, 1);
    assertThat(formatter.print(date, Locale.US)).isEqualTo("Jun 1, 2009");
    assertThat(formatter.parse("Jun 1, 2009", Locale.US)).isEqualTo(date);
  }

  @Test
  void shouldPrintAndParseLong() throws Exception {
    DateFormatter formatter = new DateFormatter();
    formatter.setTimeZone(UTC);
    formatter.setStyle(DateFormat.LONG);

    Date date = getDate(2009, Calendar.JUNE, 1);
    assertThat(formatter.print(date, Locale.US)).isEqualTo("June 1, 2009");
    assertThat(formatter.parse("June 1, 2009", Locale.US)).isEqualTo(date);
  }

  @Test
  void shouldPrintAndParseFull() throws Exception {
    DateFormatter formatter = new DateFormatter();
    formatter.setTimeZone(UTC);
    formatter.setStyle(DateFormat.FULL);

    Date date = getDate(2009, Calendar.JUNE, 1);
    assertThat(formatter.print(date, Locale.US)).isEqualTo("Monday, June 1, 2009");
    assertThat(formatter.parse("Monday, June 1, 2009", Locale.US)).isEqualTo(date);
  }

  @Test
  void shouldPrintAndParseIsoDate() throws Exception {
    DateFormatter formatter = new DateFormatter();
    formatter.setTimeZone(UTC);
    formatter.setIso(ISO.DATE);

    Date date = getDate(2009, Calendar.JUNE, 1, 14, 23, 5, 3);
    assertThat(formatter.print(date, Locale.US)).isEqualTo("2009-06-01");
    assertThat(formatter.parse("2009-6-01", Locale.US))
            .isEqualTo(getDate(2009, Calendar.JUNE, 1));
  }

  @Test
  void shouldPrintAndParseIsoTime() throws Exception {
    DateFormatter formatter = new DateFormatter();
    formatter.setTimeZone(UTC);
    formatter.setIso(ISO.TIME);

    Date date = getDate(2009, Calendar.JANUARY, 1, 14, 23, 5, 3);
    assertThat(formatter.print(date, Locale.US)).isEqualTo("14:23:05.003Z");
    assertThat(formatter.parse("14:23:05.003Z", Locale.US))
            .isEqualTo(getDate(1970, Calendar.JANUARY, 1, 14, 23, 5, 3));

    date = getDate(2009, Calendar.JANUARY, 1, 14, 23, 5, 0);
    assertThat(formatter.print(date, Locale.US)).isEqualTo("14:23:05.000Z");
    assertThat(formatter.parse("14:23:05Z", Locale.US))
            .isEqualTo(getDate(1970, Calendar.JANUARY, 1, 14, 23, 5, 0).toInstant());
  }

  @Test
  void shouldPrintAndParseIsoDateTime() throws Exception {
    DateFormatter formatter = new DateFormatter();
    formatter.setTimeZone(UTC);
    formatter.setIso(ISO.DATE_TIME);

    Date date = getDate(2009, Calendar.JUNE, 1, 14, 23, 5, 3);
    assertThat(formatter.print(date, Locale.US)).isEqualTo("2009-06-01T14:23:05.003Z");
    assertThat(formatter.parse("2009-06-01T14:23:05.003Z", Locale.US)).isEqualTo(date);

    date = getDate(2009, Calendar.JUNE, 1, 14, 23, 5, 0);
    assertThat(formatter.print(date, Locale.US)).isEqualTo("2009-06-01T14:23:05.000Z");
    assertThat(formatter.parse("2009-06-01T14:23:05Z", Locale.US)).isEqualTo(date.toInstant());
  }

  @Test
  void shouldThrowOnUnsupportedStylePattern() {
    DateFormatter formatter = new DateFormatter();
    formatter.setStylePattern("OO");

    assertThatIllegalStateException().isThrownBy(() -> formatter.parse("2009", Locale.US))
            .withMessageContaining("Unsupported style pattern 'OO'");
  }

  @Test
  void shouldUseCorrectOrder() {
    DateFormatter formatter = new DateFormatter();
    formatter.setTimeZone(UTC);
    formatter.setStyle(DateFormat.SHORT);
    formatter.setStylePattern("L-");
    formatter.setIso(ISO.DATE_TIME);
    formatter.setPattern("yyyy");

    Date date = getDate(2009, Calendar.JUNE, 1, 14, 23, 5, 3);
    assertThat(formatter.print(date, Locale.US)).as("uses pattern").isEqualTo("2009");

    formatter.setPattern("");
    assertThat(formatter.print(date, Locale.US)).as("uses ISO").isEqualTo("2009-06-01T14:23:05.003Z");

    formatter.setIso(ISO.NONE);
    assertThat(formatter.print(date, Locale.US)).as("uses style pattern").isEqualTo("June 1, 2009");

    formatter.setStylePattern("");
    assertThat(formatter.print(date, Locale.US)).as("uses style").isEqualTo("6/1/09");
  }

  private Date getDate(int year, int month, int dayOfMonth) {
    return getDate(year, month, dayOfMonth, 0, 0, 0, 0);
  }

  private Date getDate(int year, int month, int dayOfMonth, int hour, int minute, int second, int millisecond) {
    Calendar cal = Calendar.getInstance(Locale.US);
    cal.setTimeZone(UTC);
    cal.clear();
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month);
    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    cal.set(Calendar.HOUR, hour);
    cal.set(Calendar.MINUTE, minute);
    cal.set(Calendar.SECOND, second);
    cal.set(Calendar.MILLISECOND, millisecond);
    return cal.getTime();
  }

}
