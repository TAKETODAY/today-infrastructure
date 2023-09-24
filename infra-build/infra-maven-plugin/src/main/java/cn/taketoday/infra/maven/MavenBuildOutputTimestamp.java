/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.infra.maven;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import cn.taketoday.util.StringUtils;

/**
 * Parse output timestamp configured for Reproducible Builds' archive entries.
 * <p>
 * Either as {@link java.time.format.DateTimeFormatter#ISO_OFFSET_DATE_TIME} or as a
 * number representing seconds since the epoch (like <a href=
 * "https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
 * Implementation inspired by <a href=
 * "https://github.com/apache/maven-archiver/blob/cc2f6a219f6563f450b0c00e8ccd651520b67406/src/main/java/org/apache/maven/archiver/MavenArchiver.java#L768">MavenArchiver</a>.
 *
 * @author Niels Basjes
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class MavenBuildOutputTimestamp {

  private static final Instant DATE_MIN = Instant.parse("1980-01-01T00:00:02Z");

  private static final Instant DATE_MAX = Instant.parse("2099-12-31T23:59:59Z");

  private final String timestamp;

  /**
   * Creates a new {@link MavenBuildOutputTimestamp}.
   *
   * @param timestamp timestamp or {@code null}
   */
  MavenBuildOutputTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Returns the parsed timestamp as an {@code FileTime}.
   *
   * @return the parsed timestamp as an {@code FileTime}, or {@code null}
   * @throws IllegalArgumentException if the outputTimestamp is neither ISO 8601 nor an
   * integer, or it's not within the valid range 1980-01-01T00:00:02Z to
   * 2099-12-31T23:59:59Z
   */
  FileTime toFileTime() {
    Instant instant = toInstant();
    if (instant == null) {
      return null;
    }
    return FileTime.from(instant);
  }

  /**
   * Returns the parsed timestamp as an {@code Instant}.
   *
   * @return the parsed timestamp as an {@code Instant}, or {@code null}
   * @throws IllegalArgumentException if the outputTimestamp is neither ISO 8601 nor an
   * integer, or it's not within the valid range 1980-01-01T00:00:02Z to
   * 2099-12-31T23:59:59Z
   */
  Instant toInstant() {
    if (StringUtils.isEmpty(this.timestamp)) {
      return null;
    }
    if (isNumeric(this.timestamp)) {
      return Instant.ofEpochSecond(Long.parseLong(this.timestamp));
    }
    if (this.timestamp.length() < 2) {
      return null;
    }
    try {
      Instant instant = OffsetDateTime.parse(this.timestamp)
              .withOffsetSameInstant(ZoneOffset.UTC)
              .truncatedTo(ChronoUnit.SECONDS)
              .toInstant();
      if (instant.isBefore(DATE_MIN) || instant.isAfter(DATE_MAX)) {
        throw new IllegalArgumentException(String
                .format(String.format("'%s' is not within the valid range %s to %s", instant, DATE_MIN, DATE_MAX)));
      }
      return instant;
    }
    catch (DateTimeParseException pe) {
      throw new IllegalArgumentException(String.format("Can't parse '%s' to instant", this.timestamp));
    }
  }

  private static boolean isNumeric(String str) {
    for (char c : str.toCharArray()) {
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }

}
