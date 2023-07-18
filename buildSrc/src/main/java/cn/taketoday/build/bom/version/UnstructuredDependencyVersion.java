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
 * A {@link DependencyVersion} with no structure such that version comparisons are not
 * possible.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class UnstructuredDependencyVersion extends AbstractDependencyVersion implements DependencyVersion {

  private final String version;

  private UnstructuredDependencyVersion(String version) {
    super(new ComparableVersion(version));
    this.version = version;
  }

  @Override
  public boolean isNewerThan(DependencyVersion other) {
    return compareTo(other) > 0;
  }

  @Override
  public boolean isSameMajorAndNewerThan(DependencyVersion other) {
    return compareTo(other) > 0;
  }

  @Override
  public boolean isSameMinorAndNewerThan(DependencyVersion other) {
    return compareTo(other) > 0;
  }

  @Override
  public String toString() {
    return this.version;
  }

  static UnstructuredDependencyVersion parse(String version) {
    return new UnstructuredDependencyVersion(version);
  }

}
