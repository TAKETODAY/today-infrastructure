/*
 *    Copyright 2009-2021 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.type;

import org.jspecify.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Locale;

import infra.lang.Assert;
import infra.lang.TodayStrategies;

/**
 * A configurable {@link java.time.Duration} type handler that supports multiple storage formats.
 * <p>
 * Supported storage formats and their corresponding database column types:
 * <ul>
 *   <li>{@link StorageFormat#NANOSECONDS} &rarr; Database {@code BIGINT},
 *   stores total nanoseconds (range approx. &plusmn;292 years)</li>
 *   <li>{@link StorageFormat#MILLISECONDS} &rarr; Database {@code BIGINT},
 *   stores total milliseconds (range approx. &plusmn;292 million years)</li>
 *   <li>{@link StorageFormat#SECONDS} &rarr; Database {@code BIGINT},
 *   stores total seconds (nanosecond precision lost, range approx. &plusmn;292 billion years)</li>
 *   <li>{@link StorageFormat#ISO_STRING} &rarr; Database {@code VARCHAR},
 *   stores ISO-8601 format (e.g., "PT10M5.123S")</li>
 * </ul>
 * <p>
 * Defaults to {@link StorageFormat#NANOSECONDS} to maintain compatibility with previous behavior.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/11 14:23
 */
public class DurationTypeHandler extends BasicTypeHandler<Duration> {

  public static final String DEFAULT_FORMAT_KEY = "jdbc.type-handler.duration-storage-format";

  private static final StorageFormat defaultFormat = findDefaultFormat();

  private final StorageFormat format;

  public enum StorageFormat {
    NANOSECONDS, MILLISECONDS, SECONDS, ISO_STRING
  }

  public DurationTypeHandler() {
    this(defaultFormat);
  }

  public DurationTypeHandler(StorageFormat format) {
    Assert.notNull(format, "format is required");
    this.format = format;
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int idx, Duration parameter) throws SQLException {
    switch (format) {
      case NANOSECONDS -> ps.setLong(idx, parameter.toNanos());
      case MILLISECONDS -> ps.setLong(idx, parameter.toMillis());
      case SECONDS -> ps.setLong(idx, parameter.getSeconds());
      case ISO_STRING -> ps.setString(idx, parameter.toString());
    }
  }

  @Override
  public @Nullable Duration getResult(ResultSet rs, String columnName) throws SQLException {
    return extractDuration(rs, columnName);
  }

  @Override
  public @Nullable Duration getResult(ResultSet rs, int columnIndex) throws SQLException {
    return extractDuration(rs, columnIndex);
  }

  @Override
  public @Nullable Duration getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return extractDuration(cs, columnIndex);
  }

  private @Nullable Duration extractDuration(ResultSet rs, String columnName) throws SQLException {
    return switch (format) {
      case NANOSECONDS -> {
        long nanos = rs.getLong(columnName);
        yield rs.wasNull() ? null : Duration.ofNanos(nanos);
      }
      case MILLISECONDS -> {
        long millis = rs.getLong(columnName);
        yield rs.wasNull() ? null : Duration.ofMillis(millis);
      }
      case SECONDS -> {
        long seconds = rs.getLong(columnName);
        yield rs.wasNull() ? null : Duration.ofSeconds(seconds);
      }
      case ISO_STRING -> {
        String str = rs.getString(columnName);
        yield str == null ? null : Duration.parse(str);
      }
    };
  }

  private @Nullable Duration extractDuration(ResultSet rs, int columnIndex) throws SQLException {
    return switch (format) {
      case NANOSECONDS -> {
        long nanos = rs.getLong(columnIndex);
        yield rs.wasNull() ? null : Duration.ofNanos(nanos);
      }
      case MILLISECONDS -> {
        long millis = rs.getLong(columnIndex);
        yield rs.wasNull() ? null : Duration.ofMillis(millis);
      }
      case SECONDS -> {
        long seconds = rs.getLong(columnIndex);
        yield rs.wasNull() ? null : Duration.ofSeconds(seconds);
      }
      case ISO_STRING -> {
        String str = rs.getString(columnIndex);
        yield str == null ? null : Duration.parse(str);
      }
    };
  }

  private @Nullable Duration extractDuration(CallableStatement cs, int columnIndex) throws SQLException {
    return switch (format) {
      case NANOSECONDS -> {
        long nanos = cs.getLong(columnIndex);
        yield cs.wasNull() ? null : Duration.ofNanos(nanos);
      }
      case MILLISECONDS -> {
        long millis = cs.getLong(columnIndex);
        yield cs.wasNull() ? null : Duration.ofMillis(millis);
      }
      case SECONDS -> {
        long seconds = cs.getLong(columnIndex);
        yield cs.wasNull() ? null : Duration.ofSeconds(seconds);
      }
      case ISO_STRING -> {
        String str = cs.getString(columnIndex);
        yield str == null ? null : Duration.parse(str);
      }
    };
  }

  private static StorageFormat findDefaultFormat() {
    String property = TodayStrategies.getProperty(DEFAULT_FORMAT_KEY);
    if (property != null) {
      try {
        return StorageFormat.valueOf(property.toUpperCase(Locale.ROOT));
      }
      catch (Throwable ignored) {
      }
    }
    return StorageFormat.NANOSECONDS;
  }

}