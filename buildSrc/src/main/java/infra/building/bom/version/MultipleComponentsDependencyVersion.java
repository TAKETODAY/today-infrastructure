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
