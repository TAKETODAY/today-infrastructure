/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.app.loader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Manifest;

import cn.taketoday.lang.Nullable;

/**
 * An archive that can be launched by the {@link Launcher}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JarFileArchive
 * @since 4.0
 */
public interface Archive extends AutoCloseable {

  /**
   * Predicate that accepts all entries.
   */
  Predicate<Entry> ALL_ENTRIES = (entry) -> true;

  /**
   * Returns the manifest of the archive.
   *
   * @return the manifest or {@code null}
   * @throws IOException if the manifest cannot be read
   */
  @Nullable
  Manifest getManifest() throws IOException;

  /**
   * Returns classpath URLs for the archive that match the specified filter.
   *
   * @param includeFilter filter used to determine which entries should be included.
   * @return the classpath URLs
   * @throws IOException on IO error
   */
  default Set<URL> getClassPathUrls(Predicate<Entry> includeFilter) throws IOException {
    return getClassPathUrls(includeFilter, ALL_ENTRIES);
  }

  /**
   * Returns classpath URLs for the archive that match the specified filters.
   *
   * @param includeFilter filter used to determine which entries should be included
   * @param directorySearchFilter filter used to optimize tree walking for exploded
   * archives by determining if a directory needs to be searched or not
   * @return the classpath URLs
   * @throws IOException on IO error
   */
  Set<URL> getClassPathUrls(Predicate<Entry> includeFilter, Predicate<Entry> directorySearchFilter)
          throws IOException;

  /**
   * Returns if this archive is backed by an exploded archive directory.
   *
   * @return if the archive is exploded
   */
  default boolean isExploded() {
    return getRootDirectory() != null;
  }

  /**
   * Returns the root directory of this archive or {@code null} if the archive is not
   * backed by a directory.
   *
   * @return the root directory
   */
  @Nullable
  default File getRootDirectory() {
    return null;
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
   * Factory method to create an appropriate {@link Archive} from the given
   * {@link Class} target.
   *
   * @param target a target class that will be used to find the archive code source
   * @return an new {@link Archive} instance
   * @throws Exception if the archive cannot be created
   */
  static Archive create(Class<?> target) throws Exception {
    return create(target.getProtectionDomain());
  }

  static Archive create(ProtectionDomain protectionDomain) throws Exception {
    CodeSource codeSource = protectionDomain.getCodeSource();
    URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
    String path = (location != null) ? location.getSchemeSpecificPart() : null;
    if (path == null) {
      throw new IllegalStateException("Unable to determine code source archive");
    }
    return create(new File(path));
  }

  /**
   * Factory method to create an {@link Archive} from the given {@link File} target.
   *
   * @param target a target {@link File} used to create the archive. May be a directory
   * or a jar file.
   * @return a new {@link Archive} instance.
   * @throws Exception if the archive cannot be created
   */
  static Archive create(File target) throws Exception {
    if (!target.exists()) {
      throw new IllegalStateException("Unable to determine code source archive from " + target);
    }
    return (target.isDirectory() ? new ExplodedArchive(target) : new JarFileArchive(target));
  }

  /**
   * Represents a single entry in the archive.
   */
  interface Entry {

    /**
     * Returns the name of the entry.
     *
     * @return the name of the entry
     */
    String name();

    /**
     * Returns {@code true} if the entry represents a directory.
     *
     * @return if the entry is a directory
     */
    boolean isDirectory();

  }

}
