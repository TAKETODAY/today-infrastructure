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

package cn.taketoday.buildpack.platform.build;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.lang.Assert;

/**
 * API Version number comprised of a major and minor value.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ApiVersion {

  private static final Pattern PATTERN = Pattern.compile("^v?(\\d+)\\.(\\d*)$");

  private final int major;

  private final int minor;

  private ApiVersion(int major, int minor) {
    this.major = major;
    this.minor = minor;
  }

  /**
   * Return the major version number.
   *
   * @return the major version
   */
  int getMajor() {
    return this.major;
  }

  /**
   * Return the minor version number.
   *
   * @return the minor version
   */
  int getMinor() {
    return this.minor;
  }

  /**
   * Assert that this API version supports the specified version.
   *
   * @param other the version to check against
   * @see #supports(ApiVersion)
   */
  void assertSupports(ApiVersion other) {
    if (!supports(other)) {
      throw new IllegalStateException(
              "Detected platform API version '" + other + "' does not match supported version '" + this + "'");
    }
  }

  /**
   * Returns if this API version supports the given version. A {@code 0.x} matches only
   * the same version number. A 1.x or higher release matches when the versions have the
   * same major version and a minor that is equal or greater.
   *
   * @param other the version to check against
   * @return if the specified API version is supported
   * @see #assertSupports(ApiVersion)
   */
  boolean supports(ApiVersion other) {
    if (equals(other)) {
      return true;
    }
    if (this.major == 0 || this.major != other.major) {
      return false;
    }
    return this.minor >= other.minor;
  }

  /**
   * Returns if this API version supports any of the given versions.
   *
   * @param others the versions to check against
   * @return if any of the specified API versions are supported
   * @see #supports(ApiVersion)
   */
  boolean supportsAny(ApiVersion... others) {
    for (ApiVersion other : others) {
      if (supports(other)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ApiVersion other = (ApiVersion) obj;
    return (this.major == other.major) && (this.minor == other.minor);
  }

  @Override
  public int hashCode() {
    return this.major * 31 + this.minor;
  }

  @Override
  public String toString() {
    return this.major + "." + this.minor;
  }

  /**
   * Factory method to parse a string into an {@link ApiVersion} instance.
   *
   * @param value the value to parse.
   * @return the corresponding {@link ApiVersion}
   * @throws IllegalArgumentException if the value could not be parsed
   */
  static ApiVersion parse(String value) {
    Assert.hasText(value, "Value must not be empty");
    Matcher matcher = PATTERN.matcher(value);
    Assert.isTrue(matcher.matches(), () -> "Malformed version number '" + value + "'");
    try {
      int major = Integer.parseInt(matcher.group(1));
      int minor = Integer.parseInt(matcher.group(2));
      return new ApiVersion(major, minor);
    }
    catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Malformed version number '" + value + "'", ex);
    }
  }

  static ApiVersion of(int major, int minor) {
    return new ApiVersion(major, minor);
  }

}
