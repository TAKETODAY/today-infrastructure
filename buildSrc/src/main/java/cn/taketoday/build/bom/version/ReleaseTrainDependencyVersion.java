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

package cn.taketoday.build.bom.version;

import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link DependencyVersion} for a release train such as Spring Data.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ReleaseTrainDependencyVersion implements DependencyVersion {

  private static final Pattern VERSION_PATTERN = Pattern.compile("([A-Z][a-z]+)-([A-Z]+)([0-9]*)");

  private final String releaseTrain;

  private final String type;

  private final int version;

  private final String original;

  private ReleaseTrainDependencyVersion(String releaseTrain, String type, int version, String original) {
    this.releaseTrain = releaseTrain;
    this.type = type;
    this.version = version;
    this.original = original;
  }

  @Override
  public int compareTo(DependencyVersion other) {
    if (!(other instanceof ReleaseTrainDependencyVersion otherReleaseTrain)) {
      return -1;
    }
    int comparison = this.releaseTrain.compareTo(otherReleaseTrain.releaseTrain);
    if (comparison != 0) {
      return comparison;
    }
    comparison = this.type.compareTo(otherReleaseTrain.type);
    if (comparison != 0) {
      return comparison;
    }
    return Integer.compare(this.version, otherReleaseTrain.version);
  }

  @Override
  public boolean isNewerThan(DependencyVersion other) {
    if (other instanceof CalendarVersionDependencyVersion) {
      return false;
    }
    if (!(other instanceof ReleaseTrainDependencyVersion otherReleaseTrain)) {
      return true;
    }
    return otherReleaseTrain.compareTo(this) < 0;
  }

  @Override
  public boolean isSameMajorAndNewerThan(DependencyVersion other) {
    return isNewerThan(other);
  }

  @Override
  public boolean isSameMinorAndNewerThan(DependencyVersion other) {
    if (other instanceof CalendarVersionDependencyVersion) {
      return false;
    }
    if (!(other instanceof ReleaseTrainDependencyVersion otherReleaseTrain)) {
      return true;
    }
    return otherReleaseTrain.releaseTrain.equals(this.releaseTrain) && isNewerThan(other);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ReleaseTrainDependencyVersion other = (ReleaseTrainDependencyVersion) obj;
    if (!this.original.equals(other.original)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return this.original.hashCode();
  }

  @Override
  public String toString() {
    return this.original;
  }

  static ReleaseTrainDependencyVersion parse(String input) {
    Matcher matcher = VERSION_PATTERN.matcher(input);
    if (!matcher.matches()) {
      return null;
    }
    return new ReleaseTrainDependencyVersion(matcher.group(1), matcher.group(2),
            (StringUtils.isNotEmpty(matcher.group(3))) ? Integer.parseInt(matcher.group(3)) : 0, input);
  }

}
