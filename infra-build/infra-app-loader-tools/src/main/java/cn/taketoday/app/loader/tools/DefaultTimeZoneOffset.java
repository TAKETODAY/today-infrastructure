/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools;

import java.nio.file.attribute.FileTime;
import java.util.TimeZone;
import java.util.zip.ZipEntry;

/**
 * Utility class that can be used to change a UTC time based on the
 * {@link java.util.TimeZone#getDefault() default TimeZone}. This is required because
 * {@link ZipEntry#setTime(long)} expects times in the default timezone and not UTC.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultTimeZoneOffset {

  static final DefaultTimeZoneOffset INSTANCE = new DefaultTimeZoneOffset(TimeZone.getDefault());

  private final TimeZone defaultTimeZone;

  DefaultTimeZoneOffset(TimeZone defaultTimeZone) {
    this.defaultTimeZone = defaultTimeZone;
  }

  /**
   * Remove the default offset from the given time.
   *
   * @param time the time to remove the default offset from
   * @return the time with the default offset removed
   */
  FileTime removeFrom(FileTime time) {
    return FileTime.fromMillis(removeFrom(time.toMillis()));
  }

  /**
   * Remove the default offset from the given time.
   *
   * @param time the time to remove the default offset from
   * @return the time with the default offset removed
   */
  long removeFrom(long time) {
    return time - this.defaultTimeZone.getOffset(time);
  }

}
