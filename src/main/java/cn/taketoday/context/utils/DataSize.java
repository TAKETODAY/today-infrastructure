/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.context.Constant;

/**
 * A data size, such as '12MB'.
 *
 * <p>
 * This class models a size in terms of bytes and is immutable and thread-safe.
 *
 * @author Stephane Nicoll
 * @author TODAY
 * @since 2.1.3
 */
public final class DataSize implements Comparable<DataSize> {

  /** The pattern for parsing. */
  private static final Pattern PATTERN = Pattern.compile("^([+\\-]?\\d+)([a-zA-Z]{0,2})$");

  private final long bytes;

  private DataSize(long bytes) {
    this.bytes = bytes;
  }

  /**
   * Obtain a {@link DataSize} representing the specified number of bytes.
   *
   * @param bytes
   *         the number of bytes, positive or negative
   *
   * @return a {@link DataSize}
   */
  public static DataSize ofBytes(long bytes) {
    return new DataSize(bytes);
  }

  /**
   * Obtain a {@link DataSize} representing the specified number of kilobytes.
   *
   * @param kilobytes
   *         the number of kilobytes, positive or negative
   *
   * @return a {@link DataSize}
   */
  public static DataSize ofKilobytes(long kilobytes) {
    return new DataSize(Math.multiplyExact(kilobytes, Constant.BYTES_PER_KB));
  }

  /**
   * Obtain a {@link DataSize} representing the specified number of megabytes.
   *
   * @param megabytes
   *         the number of megabytes, positive or negative
   *
   * @return a {@link DataSize}
   */
  public static DataSize ofMegabytes(long megabytes) {
    return new DataSize(Math.multiplyExact(megabytes, Constant.BYTES_PER_MB));
  }

  /**
   * Obtain a {@link DataSize} representing the specified number of gigabytes.
   *
   * @param gigabytes
   *         the number of gigabytes, positive or negative
   *
   * @return a {@link DataSize}
   */
  public static DataSize ofGigabytes(long gigabytes) {
    return new DataSize(Math.multiplyExact(gigabytes, Constant.BYTES_PER_GB));
  }

  /**
   * Obtain a {@link DataSize} representing the specified number of terabytes.
   *
   * @param terabytes
   *         the number of terabytes, positive or negative
   *
   * @return a {@link DataSize}
   */
  public static DataSize ofTerabytes(long terabytes) {
    return new DataSize(Math.multiplyExact(terabytes, Constant.BYTES_PER_TB));
  }

  /**
   * Obtain a {@link DataSize} representing an amount in the specified
   * {@link DataUnit}.
   *
   * @param amount
   *         the amount of the size, measured in terms of the unit, positive or
   *         negative
   *
   * @return a corresponding {@link DataSize}
   *
   * @since 2.1.6
   */
  public static DataSize of(long amount) {
    return of(amount, DataUnit.BYTES);
  }

  /**
   * Obtain a {@link DataSize} representing an amount in the specified
   * {@link DataUnit}.
   *
   * @param amount
   *         the amount of the size, measured in terms of the unit, positive or
   *         negative
   *
   * @return a corresponding {@link DataSize}
   */
  public static DataSize of(long amount, DataUnit unit) {
    return new DataSize(Math.multiplyExact(amount, unit.size().toBytes()));
  }

  /**
   * Obtain a {@link DataSize} from a text string such as {@code 12MB} using
   * {@link DataUnit#BYTES} if no unit is specified.
   * <p>
   * Examples:
   *
   * <pre>
   * "12KB" -- parses as "12 kilobytes"
   * "5MB"  -- parses as "5 megabytes"
   * "20"   -- parses as "20 bytes"
   * </pre>
   *
   * @param text
   *         the text to parse
   *
   * @return the parsed {@link DataSize}
   *
   * @see #parse(CharSequence, DataUnit)
   */
  public static DataSize parse(CharSequence text) {
    return parse(text, DataUnit.BYTES);
  }

  /**
   * Obtain a {@link DataSize} from a text string such as {@code 12MB} using the
   * specified default {@link DataUnit} if no unit is specified.
   * <p>
   * The string starts with a number followed optionally by a unit matching one of
   * the supported {@link DataUnit suffixes}.
   * <p>
   * Examples:
   *
   * <pre>
   * "12KB" -- parses as "12 kilobytes"
   * "5MB"  -- parses as "5 megabytes"
   * "20"   -- parses as "20 kilobytes" (where the {@code defaultUnit} is {@link DataUnit#KILOBYTES})
   * </pre>
   *
   * @param text
   *         the text to parse
   *
   * @return the parsed {@link DataSize}
   */
  public static DataSize parse(CharSequence text, DataUnit defaultUnit) {
    try {

      final Matcher matcher = PATTERN.matcher(text);
      // fix matcher
      if (matcher.matches()) {
        final long amount = Long.parseLong(matcher.group(1));
        final DataUnit unit = determineDataUnit(matcher.group(2), defaultUnit);
        return DataSize.of(amount, unit);
      }
      return DataSize.of(Long.parseLong(text.toString()), DataUnit.BYTES);
    }
    catch (Exception ex) {
      throw new IllegalArgumentException("'" + text + "' is not a valid data size", ex);
    }
  }

  private static DataUnit determineDataUnit(String suffix, DataUnit defaultUnit) {
    return (StringUtils.isNotEmpty(suffix) ? DataUnit.fromSuffix(suffix) : Objects.requireNonNull(defaultUnit));
  }

  /**
   * Checks if this size is negative, excluding zero.
   *
   * @return true if this size has a size less than zero bytes
   */
  public boolean isNegative() {
    return this.bytes < 0;
  }

  /**
   * Return the number of bytes in this instance.
   *
   * @return the number of bytes
   */
  public long toBytes() {
    return this.bytes;
  }

  /**
   * Return the number of kilobytes in this instance.
   *
   * @return the number of kilobytes
   */
  public long toKilobytes() {
    return this.bytes / Constant.BYTES_PER_KB;
  }

  /**
   * Return the number of megabytes in this instance.
   *
   * @return the number of megabytes
   */
  public long toMegabytes() {
    return this.bytes / Constant.BYTES_PER_MB;
  }

  /**
   * Return the number of gigabytes in this instance.
   *
   * @return the number of gigabytes
   */
  public long toGigabytes() {
    return this.bytes / Constant.BYTES_PER_GB;
  }

  /**
   * Return the number of terabytes in this instance.
   *
   * @return the number of terabytes
   */
  public long toTerabytes() {
    return this.bytes / Constant.BYTES_PER_TB;
  }

  @Override
  public int compareTo(DataSize other) {
    return Long.compare(this.bytes, other.bytes);
  }

  @Override
  public String toString() {
    return String.format("%dB", this.bytes);
  }

  @Override
  public boolean equals(Object other) {
    return this == other || (other instanceof DataSize && this.bytes == ((DataSize) other).bytes);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.bytes);
  }

}
