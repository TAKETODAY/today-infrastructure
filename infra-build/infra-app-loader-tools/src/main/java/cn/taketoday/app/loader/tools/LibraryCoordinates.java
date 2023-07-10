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
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface LibraryCoordinates {

  /**
   * Return the group ID of the coordinates.
   *
   * @return the group ID
   */
  String getGroupId();

  /**
   * Return the artifact ID of the coordinates.
   *
   * @return the artifact ID
   */
  String getArtifactId();

  /**
   * Return the version of the coordinates.
   *
   * @return the version
   */
  String getVersion();

  /**
   * Factory method to create {@link LibraryCoordinates} with the specified values.
   *
   * @param groupId the group ID
   * @param artifactId the artifact ID
   * @param version the version
   * @return a new {@link LibraryCoordinates} instance
   */
  static LibraryCoordinates of(String groupId, String artifactId, String version) {
    return new DefaultLibraryCoordinates(groupId, artifactId, version);
  }

  /**
   * Utility method that returns the given coordinates using the standard
   * {@code group:artifact:version} form.
   *
   * @param coordinates the coordinates to convert (may be {@code null})
   * @return the standard notation form or {@code "::"} when the coordinates are null
   */
  static String toStandardNotationString(LibraryCoordinates coordinates) {
    if (coordinates == null) {
      return "::";
    }
    StringBuilder builder = new StringBuilder();
    builder.append((coordinates.getGroupId() != null) ? coordinates.getGroupId() : "");
    builder.append(":");
    builder.append((coordinates.getArtifactId() != null) ? coordinates.getArtifactId() : "");
    builder.append(":");
    builder.append((coordinates.getVersion() != null) ? coordinates.getVersion() : "");
    return builder.toString();
  }

}
