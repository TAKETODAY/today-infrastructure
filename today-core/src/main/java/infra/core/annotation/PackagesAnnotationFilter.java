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

package infra.core.annotation;

import java.util.Arrays;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.StringUtils;

/**
 * {@link AnnotationFilter} implementation used for
 * {@link AnnotationFilter#packages(String...)}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
final class PackagesAnnotationFilter implements AnnotationFilter {

  private final String[] prefixes;

  private final int hashCode;

  PackagesAnnotationFilter(String... packages) {
    Assert.notNull(packages, "Packages array is required");
    this.prefixes = new String[packages.length];
    for (int i = 0; i < packages.length; i++) {
      String pkg = packages[i];
      Assert.hasText(pkg, "Packages array must not have empty elements");
      this.prefixes[i] = pkg + ".";
    }
    Arrays.sort(this.prefixes);
    this.hashCode = Arrays.hashCode(this.prefixes);
  }

  @Override
  public boolean matches(String annotationType) {
    for (String prefix : this.prefixes) {
      if (annotationType.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    return Arrays.equals(this.prefixes, ((PackagesAnnotationFilter) other).prefixes);
  }

  @Override
  public int hashCode() {
    return this.hashCode;
  }

  @Override
  public String toString() {
    return "Packages annotation filter: " + StringUtils.arrayToCommaDelimitedString(this.prefixes);
  }

}
