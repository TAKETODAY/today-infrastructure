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

package infra.core.conversion.support;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/19 22:49
 */
class ZonedDateTimeToCalendarConverterTests {

  @Test
  void convertZonedDateTimeToCalendar() {
    ZonedDateTimeToCalendarConverter converter = new ZonedDateTimeToCalendarConverter();
    ZonedDateTime source = ZonedDateTime.now();

    Calendar result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(GregorianCalendar.class);
    assertThat(result.getTimeInMillis()).isEqualTo(source.toInstant().toEpochMilli());
    assertThat(result.getTimeZone().toZoneId()).isEqualTo(source.getZone());
  }

  @Test
  void convertZonedDateTimeToCalendarWithDifferentTimeZone() {
    ZonedDateTimeToCalendarConverter converter = new ZonedDateTimeToCalendarConverter();
    ZonedDateTime source = ZonedDateTime.of(2023, 6, 15, 10, 30, 0, 0,
            java.time.ZoneId.of("America/New_York"));

    Calendar result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result.getTimeZone().toZoneId()).isEqualTo(source.getZone());
    assertThat(result.get(java.util.Calendar.YEAR)).isEqualTo(source.getYear());
    assertThat(result.get(java.util.Calendar.MONTH)).isEqualTo(source.getMonthValue() - 1);
    assertThat(result.get(java.util.Calendar.DAY_OF_MONTH)).isEqualTo(source.getDayOfMonth());
  }

}