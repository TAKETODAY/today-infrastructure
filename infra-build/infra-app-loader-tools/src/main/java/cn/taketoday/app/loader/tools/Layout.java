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

import cn.taketoday.lang.Nullable;

/**
 * Strategy interface used to determine the layout for a particular type of archive.
 * Layouts may additionally implement {@link CustomLoaderLayout} if they wish to write
 * custom loader classes.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Layouts
 * @see RepackagingLayout
 * @see CustomLoaderLayout
 * @since 4.0
 */
public interface Layout {

  /**
   * Returns the launcher class name for this layout.
   *
   * @return the launcher class name
   */
  @Nullable
  String getLauncherClassName();

  /**
   * Returns the destination path for a given library.
   *
   * @param libraryName the name of the library (excluding any path)
   * @param scope the scope of the library
   * @return the location of the library relative to the root of the archive (should end
   * with '/') or {@code null} if the library should not be included.
   */
  @Nullable
  String getLibraryLocation(String libraryName, LibraryScope scope);

  /**
   * Returns the location of classes within the archive.
   *
   * @return the classes location
   */
  String getClassesLocation();

  /**
   * Returns the location of the classpath index file that should be written or
   * {@code null} if not index is required. The result should include the filename and
   * is relative to the root of the jar.
   *
   * @return the classpath index file location
   */
  @Nullable
  default String getClasspathIndexFileLocation() {
    return null;
  }

  /**
   * Returns the location of the layer index file that should be written or {@code null}
   * if not index is required. The result should include the filename and is relative to
   * the root of the jar.
   *
   * @return the layer index file location
   */
  @Nullable
  default String getLayersIndexFileLocation() {
    return null;
  }

  /**
   * Returns if loader classes should be included to make the archive executable.
   *
   * @return if the layout is executable
   */
  boolean isExecutable();

}
