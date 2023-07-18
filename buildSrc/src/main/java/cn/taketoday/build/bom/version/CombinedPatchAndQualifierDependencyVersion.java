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
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link DependencyVersion} where the patch and qualifier are not separated.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class CombinedPatchAndQualifierDependencyVersion extends ArtifactVersionDependencyVersion {

  private static final Pattern PATTERN = Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+)([A-Za-z][A-Za-z0-9]+)");

  private final String original;

  private CombinedPatchAndQualifierDependencyVersion(ArtifactVersion artifactVersion, String original) {
    super(artifactVersion);
    this.original = original;
  }

  @Override
  public String toString() {
    return this.original;
  }

  static CombinedPatchAndQualifierDependencyVersion parse(String version) {
    Matcher matcher = PATTERN.matcher(version);
    if (!matcher.matches()) {
      return null;
    }
    ArtifactVersion artifactVersion = new DefaultArtifactVersion(matcher.group(1) + "." + matcher.group(2));
    if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier().equals(version)) {
      return null;
    }
    return new CombinedPatchAndQualifierDependencyVersion(artifactVersion, version);
  }

}
