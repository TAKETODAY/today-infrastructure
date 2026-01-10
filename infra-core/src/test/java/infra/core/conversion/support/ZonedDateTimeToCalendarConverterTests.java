/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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