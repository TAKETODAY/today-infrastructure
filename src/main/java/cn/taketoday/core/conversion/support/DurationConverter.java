/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.conversion.support;

import java.time.Duration;

import cn.taketoday.lang.Assert;

/**
 * @author TODAY 2021/3/21 11:18
 * @since 3.0
 */
public class DurationConverter implements cn.taketoday.core.conversion.Converter<String, Duration> {

  @Override
  public Duration convert(String source) {
    return parseDuration(source);
  }

  /**
   * Convert a string to {@link Duration}
   *
   * @param value Input string
   */
  public static Duration parseDuration(String value) {
    Assert.notNull(value, "Input string must not be null");
    if (value.endsWith("ns")) {
      return Duration.ofNanos(getDuration(value, 2));
    }
    if (value.endsWith("ms")) {
      return Duration.ofMillis(getDuration(value, 2));
    }
    if (value.endsWith("min")) {
      return Duration.ofMinutes(getDuration(value, 3));
    }
    if (value.endsWith("s")) {
      return Duration.ofSeconds(getDuration(value, 1));
    }
    if (value.endsWith("h")) {
      return Duration.ofHours(getDuration(value, 1));
    }
    if (value.endsWith("d")) {
      return Duration.ofDays(getDuration(value, 1));
    }

    return Duration.parse(value);
  }

  private static long getDuration(String value, int i) {
    return Long.parseLong(value.substring(0, value.length() - i));
  }
}
