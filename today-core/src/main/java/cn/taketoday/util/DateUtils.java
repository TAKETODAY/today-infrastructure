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

package cn.taketoday.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;

import cn.taketoday.lang.Assert;

/**
 * From hutool
 *
 * @author TODAY 2021/2/23 21:05
 * @since 3.0
 */
public class DateUtils {

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

  /**
   * 格式化日期时间为指定格式
   *
   * @param time {@link TemporalAccessor}
   * @param formatter 日期格式化器，预定义的格式见：{@link DateTimeFormatter}
   * @return 格式化后的字符串
   */
  public static String format(TemporalAccessor time, DateTimeFormatter formatter) {
    if (null == time) {
      return null;
    }

    if (null == formatter) {
      formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    }

    try {
      return formatter.format(time);
    }
    catch (UnsupportedTemporalTypeException e) {
      if (time instanceof LocalDate && e.getMessage().contains("HourOfDay")) {
        // 用户传入LocalDate，但是要求格式化带有时间部分，转换为LocalDateTime重试
        return formatter.format(((LocalDate) time).atStartOfDay());
      }
      else if (time instanceof LocalTime && e.getMessage().contains("YearOfEra")) {
        // 用户传入LocalTime，但是要求格式化带有日期部分，转换为LocalDateTime重试
        return formatter.format(((LocalTime) time).atDate(LocalDate.now()));
      }
      throw e;
    }
  }

  /**
   * 格式化日期时间为指定格式
   *
   * @param time {@link TemporalAccessor}
   * @param format 日期格式
   * @return 格式化后的字符串
   */
  public static String format(TemporalAccessor time, String format) {
    if (null == time) {
      return null;
    }

    final DateTimeFormatter formatter = StringUtils.isEmpty(format)
                                        ? null : DateTimeFormatter.ofPattern(format);

    return format(time, formatter);
  }

  /**
   * {@link TemporalAccessor}转换为 时间戳（从1970-01-01T00:00:00Z开始的毫秒数）
   *
   * @param temporalAccessor Date对象
   * @return {@link Instant}对象
   */
  public static long toEpochMilli(TemporalAccessor temporalAccessor) {
    Assert.notNull(temporalAccessor, "temporalAccessor is required");
    return toInstant(temporalAccessor).toEpochMilli();
  }

  /**
   * {@link TemporalAccessor}转换为 {@link Instant}对象
   *
   * @param temporalAccessor Date对象
   * @return {@link Instant}对象
   */
  public static Instant toInstant(TemporalAccessor temporalAccessor) {
    if (null == temporalAccessor) {
      return null;
    }

    Instant result;
    if (temporalAccessor instanceof Instant) {
      result = (Instant) temporalAccessor;
    }
    else if (temporalAccessor instanceof LocalDateTime) {
      result = ((LocalDateTime) temporalAccessor).atZone(ZoneId.systemDefault()).toInstant();
    }
    else if (temporalAccessor instanceof ZonedDateTime) {
      result = ((ZonedDateTime) temporalAccessor).toInstant();
    }
    else if (temporalAccessor instanceof OffsetDateTime) {
      result = ((OffsetDateTime) temporalAccessor).toInstant();
    }
    else if (temporalAccessor instanceof LocalDate) {
      result = ((LocalDate) temporalAccessor).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
    else if (temporalAccessor instanceof LocalTime) {
      // 指定本地时间转换 为Instant，取当天日期
      result = ((LocalTime) temporalAccessor).atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant();
    }
    else if (temporalAccessor instanceof OffsetTime) {
      // 指定本地时间转换 为Instant，取当天日期
      result = ((OffsetTime) temporalAccessor).atDate(LocalDate.now()).toInstant();
    }
    else {
      result = Instant.from(temporalAccessor);
    }

    return result;
  }
}
