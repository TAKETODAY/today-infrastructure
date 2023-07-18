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

import java.util.regex.Pattern;

/**
 * A specialization of {@link ArtifactVersionDependencyVersion} for calendar versions.
 * Calendar versions are always considered to be newer than
 * {@link ReleaseTrainDependencyVersion release train versions}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class CalendarVersionDependencyVersion extends ArtifactVersionDependencyVersion {

  private static final Pattern CALENDAR_VERSION_PATTERN = Pattern.compile("\\d{4}\\.\\d+\\.\\d+(-.+)?");

  protected CalendarVersionDependencyVersion(ArtifactVersion artifactVersion) {
    super(artifactVersion);
  }

  protected CalendarVersionDependencyVersion(ArtifactVersion artifactVersion, ComparableVersion comparableVersion) {
    super(artifactVersion, comparableVersion);
  }

  @Override
  public boolean isNewerThan(DependencyVersion other) {
    if (other instanceof ReleaseTrainDependencyVersion) {
      return true;
    }
    return super.isNewerThan(other);
  }

  static CalendarVersionDependencyVersion parse(String version) {
    if (!CALENDAR_VERSION_PATTERN.matcher(version).matches()) {
      return null;
    }
    ArtifactVersion artifactVersion = new DefaultArtifactVersion(version);
    if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier().equals(version)) {
      return null;
    }
    return new CalendarVersionDependencyVersion(artifactVersion);
  }

}
