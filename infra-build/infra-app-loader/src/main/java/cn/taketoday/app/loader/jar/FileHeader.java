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

package cn.taketoday.app.loader.jar;

import java.util.zip.ZipEntry;

/**
 * A file header record that has been loaded from a Jar file.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JarEntry
 * @see CentralDirectoryFileHeader
 * @since 4.0
 */
interface FileHeader {

  /**
   * Returns {@code true} if the header has the given name.
   *
   * @param name the name to test
   * @param suffix an additional suffix (or {@code 0})
   * @return {@code true} if the header has the given name
   */
  boolean hasName(CharSequence name, char suffix);

  /**
   * Return the offset of the load file header within the archive data.
   *
   * @return the local header offset
   */
  long getLocalHeaderOffset();

  /**
   * Return the compressed size of the entry.
   *
   * @return the compressed size.
   */
  long getCompressedSize();

  /**
   * Return the uncompressed size of the entry.
   *
   * @return the uncompressed size.
   */
  long getSize();

  /**
   * Return the method used to compress the data.
   *
   * @return the zip compression method
   * @see ZipEntry#STORED
   * @see ZipEntry#DEFLATED
   */
  int getMethod();

}
