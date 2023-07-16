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

import java.util.Arrays;
import java.util.stream.IntStream;

import cn.taketoday.util.StringUtils;

/**
 * A set of API Version numbers comprised of major and minor values.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ApiVersions {

  /**
   * The platform API versions supported by this release.
   */
  static final ApiVersions SUPPORTED_PLATFORMS = ApiVersions.of(0, IntStream.rangeClosed(3, 11));

  private final ApiVersion[] apiVersions;

  private ApiVersions(ApiVersion... versions) {
    this.apiVersions = versions;
  }

  /**
   * Find the latest version among the specified versions that is supported by these API
   * versions.
   *
   * @param others the versions to check against
   * @return the version
   */
  ApiVersion findLatestSupported(String... others) {
    for (int versionsIndex = this.apiVersions.length - 1; versionsIndex >= 0; versionsIndex--) {
      ApiVersion apiVersion = this.apiVersions[versionsIndex];
      for (int otherIndex = others.length - 1; otherIndex >= 0; otherIndex--) {
        ApiVersion other = ApiVersion.parse(others[otherIndex]);
        if (apiVersion.supports(other)) {
          return apiVersion;
        }
      }
    }
    throw new IllegalStateException(
            "Detected platform API versions '" + StringUtils.arrayToCommaDelimitedString(others)
                    + "' are not included in supported versions '" + this + "'");
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ApiVersions other = (ApiVersions) obj;
    return Arrays.equals(this.apiVersions, other.apiVersions);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(this.apiVersions);
  }

  @Override
  public String toString() {
    return StringUtils.arrayToCommaDelimitedString(this.apiVersions);
  }

  /**
   * Factory method to parse strings into an {@link ApiVersions} instance.
   *
   * @param values the values to parse.
   * @return the corresponding {@link ApiVersions}
   * @throws IllegalArgumentException if any values could not be parsed
   */
  static ApiVersions parse(String... values) {
    return new ApiVersions(Arrays.stream(values).map(ApiVersion::parse).toArray(ApiVersion[]::new));
  }

  static ApiVersions of(int major, IntStream minorsInclusive) {
    return new ApiVersions(
            minorsInclusive.mapToObj((minor) -> ApiVersion.of(major, minor)).toArray(ApiVersion[]::new));
  }

}
