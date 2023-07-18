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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * A fallback {@link DependencyVersion} to handle versions with four or five components
 * that cannot be handled by {@link ArtifactVersion} because the fourth component is
 * numeric.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class MultipleComponentsDependencyVersion extends ArtifactVersionDependencyVersion {

  private final String original;

  private MultipleComponentsDependencyVersion(ArtifactVersion artifactVersion, String original) {
    super(artifactVersion, new ComparableVersion(original));
    this.original = original;
  }

  @Override
  public String toString() {
    return this.original;
  }

  static MultipleComponentsDependencyVersion parse(String input) {
    String[] components = input.split("\\.");
    if (components.length == 4 || components.length == 5) {
      ArtifactVersion artifactVersion = new DefaultArtifactVersion(
              components[0] + "." + components[1] + "." + components[2]);
      if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier().equals(input)) {
        return null;
      }
      return new MultipleComponentsDependencyVersion(artifactVersion, input);
    }
    return null;
  }

}
