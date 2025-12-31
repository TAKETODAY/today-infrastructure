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

package infra.web.config.format;

import org.junit.jupiter.api.Test;

import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 17:51
 */
class DateTimeFormattersTests {
  @Test
  void defaultConstructorCreatesInstanceWithNullFormatters() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    assertThat(formatters.getDateFormatter()).isNull();
    assertThat(formatters.getDatePattern()).isNull();
    assertThat(formatters.getTimeFormatter()).isNull();
    assertThat(formatters.getDateTimeFormatter()).isNull();
    assertThat(formatters.isCustomized()).isFalse();
  }

  @Test
  void dateFormatWithIsoPatternSetsIsoLocalDate() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    DateTimeFormatters result = formatters.dateFormat("iso");

    assertThat(result.getDateFormatter()).isEqualTo(DateTimeFormatter.ISO_LOCAL_DATE);
    assertThat(result.getDatePattern()).isEqualTo("yyyy-MM-dd");
    assertThat(result.isCustomized()).isTrue();
  }

  @Test
  void dateFormatWithIsoUpperCasePatternSetsIsoLocalDate() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    DateTimeFormatters result = formatters.dateFormat("ISO");

    assertThat(result.getDateFormatter()).isEqualTo(DateTimeFormatter.ISO_LOCAL_DATE);
    assertThat(result.getDatePattern()).isEqualTo("yyyy-MM-dd");
    assertThat(result.isCustomized()).isTrue();
  }

  @Test
  void dateFormatWithCustomPatternSetsCustomFormatter() {
    DateTimeFormatters formatters = new DateTimeFormatters();
    String pattern = "dd/MM/yyyy";

    DateTimeFormatters result = formatters.dateFormat(pattern);

    assertThat(result.getDateFormatter()).isNotNull();
    assertThat(result.getDatePattern()).isEqualTo(pattern);
    assertThat(result.isCustomized()).isTrue();
  }

  @Test
  void dateFormatWithEmptyPatternSetsNullFormatter() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    DateTimeFormatters result = formatters.dateFormat("");

    assertThat(result.getDateFormatter()).isNull();
    assertThat(result.getDatePattern()).isEqualTo("");
    assertThat(result.isCustomized()).isFalse();
  }

  @Test
  void timeFormatWithIsoPatternSetsIsoLocalTime() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    DateTimeFormatters result = formatters.timeFormat("iso");

    assertThat(result.getTimeFormatter()).isEqualTo(DateTimeFormatter.ISO_LOCAL_TIME);
    assertThat(result.isCustomized()).isTrue();
  }

  @Test
  void timeFormatWithIsoOffsetPatternSetsIsoOffsetTime() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    DateTimeFormatters result = formatters.timeFormat("isooffset");

    assertThat(result.getTimeFormatter()).isEqualTo(DateTimeFormatter.ISO_OFFSET_TIME);
    assertThat(result.isCustomized()).isTrue();
  }

  @Test
  void timeFormatWithIsoOffsetHyphenPatternSetsIsoOffsetTime() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    DateTimeFormatters result = formatters.timeFormat("iso-offset");

    assertThat(result.getTimeFormatter()).isEqualTo(DateTimeFormatter.ISO_OFFSET_TIME);
    assertThat(result.isCustomized()).isTrue();
  }

  @Test
  void timeFormatWithCustomPatternSetsCustomFormatter() {
    DateTimeFormatters formatters = new DateTimeFormatters();
    String pattern = "HH:mm:ss";

    DateTimeFormatters result = formatters.timeFormat(pattern);

    assertThat(result.getTimeFormatter()).isNotNull();
    assertThat(result.isCustomized()).isTrue();
  }

  @Test
  void dateTimeFormatWithIsoPatternSetsIsoLocalDateTime() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    DateTimeFormatters result = formatters.dateTimeFormat("iso");

    assertThat(result.getDateTimeFormatter()).isEqualTo(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    assertThat(result.isCustomized()).isTrue();
  }

  @Test
  void dateTimeFormatWithIsoOffsetPatternSetsIsoOffsetDateTime() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    DateTimeFormatters result = formatters.dateTimeFormat("isooffset");

    assertThat(result.getDateTimeFormatter()).isEqualTo(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    assertThat(result.isCustomized()).isTrue();
  }

  @Test
  void dateTimeFormatWithIsoOffsetHyphenPatternSetsIsoOffsetDateTime() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    DateTimeFormatters result = formatters.dateTimeFormat("iso-offset");

    assertThat(result.getDateTimeFormatter()).isEqualTo(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    assertThat(result.isCustomized()).isTrue();
  }

  @Test
  void dateTimeFormatWithCustomPatternSetsCustomFormatter() {
    DateTimeFormatters formatters = new DateTimeFormatters();
    String pattern = "dd/MM/yyyy HH:mm:ss";

    DateTimeFormatters result = formatters.dateTimeFormat(pattern);

    assertThat(result.getDateTimeFormatter()).isNotNull();
    assertThat(result.isCustomized()).isTrue();
  }

  @Test
  void isCustomizedReturnsTrueWhenAnyFormatterIsSet() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    formatters.dateFormat("dd/MM/yyyy");

    assertThat(formatters.isCustomized()).isTrue();
  }

  @Test
  void isCustomizedReturnsTrueWhenTimeFormatterIsSet() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    formatters.timeFormat("HH:mm");

    assertThat(formatters.isCustomized()).isTrue();
  }

  @Test
  void isCustomizedReturnsTrueWhenDateTimeFormatterIsSet() {
    DateTimeFormatters formatters = new DateTimeFormatters();

    formatters.dateTimeFormat("dd/MM/yyyy HH:mm");

    assertThat(formatters.isCustomized()).isTrue();
  }

  @Test
  void formatterMethodReturnsNullForEmptyPattern() {
    DateTimeFormatter result = DateTimeFormatters.formatter("");

    assertThat(result).isNull();
  }

  @Test
  void formatterMethodReturnsNullForNullPattern() {
    DateTimeFormatter result = DateTimeFormatters.formatter(null);

    assertThat(result).isNull();
  }

  @Test
  void formatterMethodReturnsFormatterForValidPattern() {
    DateTimeFormatter result = DateTimeFormatters.formatter("yyyy-MM-dd");

    assertThat(result).isNotNull();
  }

  @Test
  void isIsoReturnsTrueForIso() {
    boolean result = DateTimeFormatters.isIso("iso");

    assertThat(result).isTrue();
  }

  @Test
  void isIsoReturnsTrueForIsoUpperCase() {
    boolean result = DateTimeFormatters.isIso("ISO");

    assertThat(result).isTrue();
  }

  @Test
  void isIsoReturnsFalseForOtherValues() {
    boolean result = DateTimeFormatters.isIso("custom");

    assertThat(result).isFalse();
  }

  @Test
  void isIsoOffsetReturnsTrueForIsooffset() {
    boolean result = DateTimeFormatters.isIsoOffset("isooffset");

    assertThat(result).isTrue();
  }

  @Test
  void isIsoOffsetReturnsTrueForIsoOffset() {
    boolean result = DateTimeFormatters.isIsoOffset("iso-offset");

    assertThat(result).isTrue();
  }

  @Test
  void isIsoOffsetReturnsFalseForOtherValues() {
    boolean result = DateTimeFormatters.isIsoOffset("custom");

    assertThat(result).isFalse();
  }

}