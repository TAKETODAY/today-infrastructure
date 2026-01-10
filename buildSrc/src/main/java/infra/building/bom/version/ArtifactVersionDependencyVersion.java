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

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link DependencyVersion} backed by an {@link ArtifactVersion}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ArtifactVersionDependencyVersion extends AbstractDependencyVersion {

  private final ArtifactVersion artifactVersion;

  protected ArtifactVersionDependencyVersion(ArtifactVersion artifactVersion) {
    super(new ComparableVersion(artifactVersion.toString()));
    this.artifactVersion = artifactVersion;
  }

  protected ArtifactVersionDependencyVersion(ArtifactVersion artifactVersion, ComparableVersion comparableVersion) {
    super(comparableVersion);
    this.artifactVersion = artifactVersion;
  }

  @Override
  public boolean isNewerThan(DependencyVersion other) {
    if (other instanceof ReleaseTrainDependencyVersion) {
      return false;
    }
    return compareTo(other) > 0;
  }

  @Override
  public boolean isSameMajorAndNewerThan(DependencyVersion other) {
    if (other instanceof ReleaseTrainDependencyVersion) {
      return false;
    }
    return extractArtifactVersionDependencyVersion(other).map(this::isSameMajorAndNewerThan).orElse(true);
  }

  private boolean isSameMajorAndNewerThan(ArtifactVersionDependencyVersion other) {
    return this.artifactVersion.getMajorVersion() == other.artifactVersion.getMajorVersion() && isNewerThan(other);
  }

  @Override
  public boolean isSameMinorAndNewerThan(DependencyVersion other) {
    if (other instanceof ReleaseTrainDependencyVersion) {
      return false;
    }
    return extractArtifactVersionDependencyVersion(other).map(this::isSameMinorAndNewerThan).orElse(true);
  }

  private boolean isSameMinorAndNewerThan(ArtifactVersionDependencyVersion other) {
    return this.artifactVersion.getMajorVersion() == other.artifactVersion.getMajorVersion()
            && this.artifactVersion.getMinorVersion() == other.artifactVersion.getMinorVersion()
            && isNewerThan(other);
  }

  @Override
  public int compareTo(DependencyVersion other) {
    if (other instanceof ArtifactVersionDependencyVersion otherArtifactDependencyVersion) {
      ArtifactVersion otherArtifactVersion = otherArtifactDependencyVersion.artifactVersion;
      if ((!Objects.equals(this.artifactVersion.getQualifier(), otherArtifactVersion.getQualifier()))
              && "snapshot".equalsIgnoreCase(otherArtifactVersion.getQualifier())
              && otherArtifactVersion.getMajorVersion() == this.artifactVersion.getMajorVersion()
              && otherArtifactVersion.getMinorVersion() == this.artifactVersion.getMinorVersion()
              && otherArtifactVersion.getIncrementalVersion() == this.artifactVersion.getIncrementalVersion()) {
        return 1;
      }
    }
    return super.compareTo(other);
  }

  @Override
  public String toString() {
    return this.artifactVersion.toString();
  }

  protected Optional<ArtifactVersionDependencyVersion> extractArtifactVersionDependencyVersion(
          DependencyVersion other) {
    ArtifactVersionDependencyVersion artifactVersion = null;
    if (other instanceof ArtifactVersionDependencyVersion otherVersion) {
      artifactVersion = otherVersion;
    }
    return Optional.ofNullable(artifactVersion);
  }

  static ArtifactVersionDependencyVersion parse(String version) {
    ArtifactVersion artifactVersion = new DefaultArtifactVersion(version);
    if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier().equals(version)) {
      return null;
    }
    return new ArtifactVersionDependencyVersion(artifactVersion);
  }

}
