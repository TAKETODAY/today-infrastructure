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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface used to write jar entry data.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface EntryWriter {

  /**
   * Write entry data to the specified output stream.
   *
   * @param outputStream the destination for the data
   * @throws IOException in case of I/O errors
   */
  void write(OutputStream outputStream) throws IOException;

  /**
   * Return the size of the content that will be written, or {@code -1} if the size is
   * not known.
   *
   * @return the size of the content
   */
  default int size() {
    return -1;
  }

}
