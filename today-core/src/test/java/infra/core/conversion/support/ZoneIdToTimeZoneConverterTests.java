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

import java.time.ZoneId;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/19 22:50
 */
class ZoneIdToTimeZoneConverterTests {

  @Test
  void convertZoneIdToTimeZone() {
    ZoneIdToTimeZoneConverter converter = new ZoneIdToTimeZoneConverter();
    ZoneId source = ZoneId.systemDefault();

    TimeZone result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result.toZoneId()).isEqualTo(source);
  }

  @Test
  void convertUtcZoneIdToTimeZone() {
    ZoneIdToTimeZoneConverter converter = new ZoneIdToTimeZoneConverter();
    ZoneId source = ZoneId.of("UTC");

    TimeZone result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result.getID()).isEqualTo("UTC");
    assertThat(result.toZoneId()).isEqualTo(source);
  }

  @Test
  void convertDifferentZoneIdToTimeZone() {
    ZoneIdToTimeZoneConverter converter = new ZoneIdToTimeZoneConverter();
    ZoneId source = ZoneId.of("America/New_York");

    TimeZone result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result.toZoneId()).isEqualTo(source);
  }

}