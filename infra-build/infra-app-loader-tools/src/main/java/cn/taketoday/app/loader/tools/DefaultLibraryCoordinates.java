/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools;

/**
 * Encapsulates information about the artifact coordinates of a library.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultLibraryCoordinates implements LibraryCoordinates {

  private final String groupId;

  private final String artifactId;

  private final String version;

  /**
   * Create a new instance from discrete elements.
   *
   * @param groupId the group ID
   * @param artifactId the artifact ID
   * @param version the version
   */
  DefaultLibraryCoordinates(String groupId, String artifactId, String version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  /**
   * Return the group ID of the coordinates.
   *
   * @return the group ID
   */
  @Override
  public String getGroupId() {
    return this.groupId;
  }

  /**
   * Return the artifact ID of the coordinates.
   *
   * @return the artifact ID
   */
  @Override
  public String getArtifactId() {
    return this.artifactId;
  }

  /**
   * Return the version of the coordinates.
   *
   * @return the version
   */
  @Override
  public String getVersion() {
    return this.version;
  }

  /**
   * Return the coordinates in the form {@code groupId:artifactId:version}.
   */
  @Override
  public String toString() {
    return LibraryCoordinates.toStandardNotationString(this);
  }

}
