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

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Base class for {@link DependencyVersion} implementations.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class AbstractDependencyVersion implements DependencyVersion {

  private final ComparableVersion comparableVersion;

  protected AbstractDependencyVersion(ComparableVersion comparableVersion) {
    this.comparableVersion = comparableVersion;
  }

  @Override
  public int compareTo(DependencyVersion other) {
    ComparableVersion otherComparable = (other instanceof AbstractDependencyVersion otherVersion)
                                        ? otherVersion.comparableVersion : new ComparableVersion(other.toString());
    return this.comparableVersion.compareTo(otherComparable);
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
    AbstractDependencyVersion other = (AbstractDependencyVersion) obj;
    if (!this.comparableVersion.equals(other.comparableVersion)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return this.comparableVersion.hashCode();
  }

  @Override
  public String toString() {
    return this.comparableVersion.toString();
  }

}
