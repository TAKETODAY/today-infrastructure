/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.utils;

/**
 * A standard set of data size units.
 *
 * @author Stephane Nicoll
 * @author TODAY
 * @since 2.1.3
 */
public enum DataUnit {

  /** Bytes. */
  BYTES("B", DataSize.ofBytes(1)),
  /** Kilobytes. */
  KILOBYTES("KB", DataSize.ofKilobytes(1)),
  /** Megabytes. */
  MEGABYTES("MB", DataSize.ofMegabytes(1)),
  /** Gigabytes. */
  GIGABYTES("GB", DataSize.ofGigabytes(1)),
  /** Terabytes. */
  TERABYTES("TB", DataSize.ofTerabytes(1));

  private final String suffix;
  private final DataSize size;

  DataUnit(String suffix, DataSize size) {
    this.suffix = suffix;
    this.size = size;
  }

  DataSize size() {
    return this.size;
  }

  /**
   * Return the {@link DataUnit} matching the specified {@code suffix}.
   *
   * @param suffix
   *         one of the standard suffix
   *
   * @return the {@link DataUnit} matching the specified {@code suffix}
   *
   * @throws IllegalArgumentException
   *         if the suffix does not match any of this enum's constants
   */
  public static DataUnit fromSuffix(String suffix) {
    for (DataUnit candidate : values()) {
      if (candidate.suffix.equalsIgnoreCase(suffix)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unknown unit '" + suffix + "'");
  }

}
