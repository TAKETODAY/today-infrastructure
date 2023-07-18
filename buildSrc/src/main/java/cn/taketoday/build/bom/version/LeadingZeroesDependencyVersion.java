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
 * A {@link DependencyVersion} that tolerates leading zeroes.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class LeadingZeroesDependencyVersion extends ArtifactVersionDependencyVersion {

  private static final Pattern PATTERN = Pattern.compile("0*([0-9]+)\\.0*([0-9]+)\\.0*([0-9]+)");

  private final String original;

  private LeadingZeroesDependencyVersion(ArtifactVersion artifactVersion, String original) {
    super(artifactVersion);
    this.original = original;
  }

  @Override
  public String toString() {
    return this.original;
  }

  static LeadingZeroesDependencyVersion parse(String input) {
    Matcher matcher = PATTERN.matcher(input);
    if (!matcher.matches()) {
      return null;
    }
    ArtifactVersion artifactVersion = new DefaultArtifactVersion(
            matcher.group(1) + matcher.group(2) + matcher.group(3));
    return new LeadingZeroesDependencyVersion(artifactVersion, input);
  }

}
