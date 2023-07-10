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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.lang.Nullable;

/**
 * Encapsulates information about a single library that may be packed into the archive.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Libraries
 * @since 4.0
 */
public class Library {

  private final String name;

  private final File file;

  private final LibraryScope scope;

  @Nullable
  private final LibraryCoordinates coordinates;

  private final boolean unpackRequired;

  private final boolean local;

  private final boolean included;

  /**
   * Create a new {@link Library}.
   *
   * @param file the source file
   * @param scope the scope of the library
   */
  public Library(File file, LibraryScope scope) {
    this(null, file, scope, null, false, false, true);
  }

  /**
   * Create a new {@link Library}.
   *
   * @param name the name of the library as it should be written or {@code null} to use
   * the file name
   * @param file the source file
   * @param scope the scope of the library
   * @param coordinates the library coordinates or {@code null}
   * @param unpackRequired if the library needs to be unpacked before it can be used
   * @param local if the library is local (part of the same build) to the application
   * that is being packaged
   * @param included if the library is included in the fat jar
   */
  public Library(@Nullable String name, File file, LibraryScope scope,
          @Nullable LibraryCoordinates coordinates, boolean unpackRequired, boolean local, boolean included) {
    this.name = (name != null) ? name : file.getName();
    this.file = file;
    this.scope = scope;
    this.coordinates = coordinates;
    this.unpackRequired = unpackRequired;
    this.local = local;
    this.included = included;
  }

  /**
   * Return the name of file as it should be written.
   *
   * @return the name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the library file.
   *
   * @return the file
   */
  public File getFile() {
    return this.file;
  }

  /**
   * Open a stream that provides the content of the source file.
   *
   * @return the file content
   * @throws IOException on error
   */
  InputStream openStream() throws IOException {
    return new FileInputStream(this.file);
  }

  /**
   * Return the scope of the library.
   *
   * @return the scope
   */
  public LibraryScope getScope() {
    return this.scope;
  }

  /**
   * Return the {@linkplain LibraryCoordinates coordinates} of the library.
   *
   * @return the coordinates
   */
  @Nullable
  public LibraryCoordinates getCoordinates() {
    return this.coordinates;
  }

  /**
   * Return if the file cannot be used directly as a nested jar and needs to be
   * unpacked.
   *
   * @return if unpack is required
   */
  public boolean isUnpackRequired() {
    return this.unpackRequired;
  }

  long getLastModified() {
    return this.file.lastModified();
  }

  /**
   * Return if the library is local (part of the same build) to the application that is
   * being packaged.
   *
   * @return if the library is local
   */
  public boolean isLocal() {
    return this.local;
  }

  /**
   * Return if the library is included in the fat jar.
   *
   * @return if the library is included
   */
  public boolean isIncluded() {
    return this.included;
  }

}
