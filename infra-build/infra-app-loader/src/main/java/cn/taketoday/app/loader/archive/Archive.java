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

package cn.taketoday.app.loader.archive;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.Manifest;

import cn.taketoday.app.loader.Launcher;

/**
 * An archive that can be launched by the {@link Launcher}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JarFileArchive
 * @since 4.0
 */
public interface Archive extends Iterable<Archive.Entry>, AutoCloseable {

  /**
   * Returns a URL that can be used to load the archive.
   *
   * @return the archive URL
   * @throws MalformedURLException if the URL is malformed
   */
  URL getUrl() throws MalformedURLException;

  /**
   * Returns the manifest of the archive.
   *
   * @return the manifest
   * @throws IOException if the manifest cannot be read
   */
  Manifest getManifest() throws IOException;

  /**
   * Returns nested {@link Archive}s for entries that match the specified filters.
   *
   * @param searchFilter filter used to limit when additional sub-entry searching is
   * required or {@code null} if all entries should be considered.
   * @param includeFilter filter used to determine which entries should be included in
   * the result or {@code null} if all entries should be included
   * @return the nested archives
   * @throws IOException on IO error
   */
  Iterator<Archive> getNestedArchives(EntryFilter searchFilter, EntryFilter includeFilter) throws IOException;

  /**
   * Return if the archive is exploded (already unpacked).
   *
   * @return if the archive is exploded
   */
  default boolean isExploded() {
    return false;
  }

  /**
   * Closes the {@code Archive}, releasing any open resources.
   *
   * @throws Exception if an error occurs during close processing
   */
  @Override
  default void close() throws Exception {

  }

  /**
   * Represents a single entry in the archive.
   */
  interface Entry {

    /**
     * Returns {@code true} if the entry represents a directory.
     *
     * @return if the entry is a directory
     */
    boolean isDirectory();

    /**
     * Returns the name of the entry.
     *
     * @return the name of the entry
     */
    String getName();

  }

  /**
   * Strategy interface to filter {@link Entry Entries}.
   */
  @FunctionalInterface
  interface EntryFilter {

    /**
     * Apply the jar entry filter.
     *
     * @param entry the entry to filter
     * @return {@code true} if the filter matches
     */
    boolean matches(Entry entry);

  }

}
