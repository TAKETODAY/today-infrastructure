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

package cn.taketoday.web.bind.resolver.date;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;

/**
 * From hutool
 *
 * @author TODAY 2021/2/23 21:05
 * @since 3.0
 */
abstract class DateUtils {

  public static LocalDateTime ofDateTime(TemporalAccessor temporalAccessor) {
    if (null == temporalAccessor) {
      return null;
    }

    if (temporalAccessor instanceof LocalDateTime) {
      return (LocalDateTime) temporalAccessor;
    }
    return LocalDateTime.of(
            LocalDate.of(
                    get(temporalAccessor, ChronoField.YEAR),
                    get(temporalAccessor, ChronoField.MONTH_OF_YEAR),
                    get(temporalAccessor, ChronoField.DAY_OF_MONTH)
            ),
            LocalTime.of(
                    get(temporalAccessor, ChronoField.HOUR_OF_DAY),
                    get(temporalAccessor, ChronoField.MINUTE_OF_HOUR)
            )
    );
  }

  public static LocalTime ofTime(TemporalAccessor temporalAccessor) {
    if (null == temporalAccessor) {
      return null;
    }

    if (temporalAccessor instanceof LocalDateTime) {
      return ((LocalDateTime) temporalAccessor).toLocalTime();
    }

    return LocalTime.of(
            get(temporalAccessor, ChronoField.HOUR_OF_DAY),
            get(temporalAccessor, ChronoField.MINUTE_OF_HOUR)
    );
  }

  /**
   * {@link TemporalAccessor}转{@link LocalDate}，使用默认时区
   *
   * @param temporalAccessor {@link TemporalAccessor}
   * @return {@link LocalDate}
   */
  public static LocalDate ofDate(TemporalAccessor temporalAccessor) {
    if (null == temporalAccessor) {
      return null;
    }

    if (temporalAccessor instanceof LocalDateTime) {
      return ((LocalDateTime) temporalAccessor).toLocalDate();
    }

    return LocalDate.of(
            get(temporalAccessor, ChronoField.YEAR),
            get(temporalAccessor, ChronoField.MONTH_OF_YEAR),
            get(temporalAccessor, ChronoField.DAY_OF_MONTH)
    );
  }

  /**
   * 安全获取时间的某个属性，属性不存在返回0
   *
   * @param temporalAccessor 需要获取的时间对象
   * @param field 需要获取的属性
   * @return 时间的值，如果无法获取则默认为 0
   */
  public static int get(TemporalAccessor temporalAccessor, TemporalField field) {
    if (temporalAccessor.isSupported(field)) {
      return temporalAccessor.get(field);
    }

    return (int) field.range().getMinimum();
  }

}
