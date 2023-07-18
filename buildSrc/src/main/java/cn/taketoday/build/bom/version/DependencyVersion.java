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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Version of a dependency.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface DependencyVersion extends Comparable<DependencyVersion> {

  List<Function<String, DependencyVersion>> parsers = Arrays.asList(
          CalendarVersionDependencyVersion::parse,
          ArtifactVersionDependencyVersion::parse,
          ReleaseTrainDependencyVersion::parse,
          MultipleComponentsDependencyVersion::parse,
          CombinedPatchAndQualifierDependencyVersion::parse,
          LeadingZeroesDependencyVersion::parse,
          UnstructuredDependencyVersion::parse
  );

  /**
   * Returns whether this version is newer than the given {@code other} version.
   *
   * @param other version to test
   * @return {@code true} if this version is newer, otherwise {@code false}
   */
  boolean isNewerThan(DependencyVersion other);

  /**
   * Returns whether this version has the same major versions as the {@code other}
   * version while also being newer.
   *
   * @param other version to test
   * @return {@code true} if this version has the same major and is newer, otherwise
   * {@code false}
   */
  boolean isSameMajorAndNewerThan(DependencyVersion other);

  /**
   * Returns whether this version has the same major and minor versions as the
   * {@code other} version while also being newer.
   *
   * @param other version to test
   * @return {@code true} if this version has the same major and minor and is newer,
   * otherwise {@code false}
   */
  boolean isSameMinorAndNewerThan(DependencyVersion other);

  static DependencyVersion parse(String version) {
    for (Function<String, DependencyVersion> parser : parsers) {
      DependencyVersion result = parser.apply(version);
      if (result != null) {
        return result;
      }
    }
    throw new IllegalArgumentException("Version '" + version + "' could not be parsed");
  }

}
