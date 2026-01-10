/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.building.bom.version;

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
