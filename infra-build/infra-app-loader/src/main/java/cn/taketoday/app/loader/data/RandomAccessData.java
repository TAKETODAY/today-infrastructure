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

package cn.taketoday.app.loader.data;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interface that provides read-only random access to some underlying data.
 * Implementations must allow concurrent reads in a thread-safe manner.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface RandomAccessData {

  /**
   * Returns an {@link InputStream} that can be used to read the underlying data. The
   * caller is responsible close the underlying stream.
   *
   * @return a new input stream that can be used to read the underlying data.
   * @throws IOException if the stream cannot be opened
   */
  InputStream getInputStream() throws IOException;

  /**
   * Returns a new {@link RandomAccessData} for a specific subsection of this data.
   *
   * @param offset the offset of the subsection
   * @param length the length of the subsection
   * @return the subsection data
   */
  RandomAccessData getSubsection(long offset, long length);

  /**
   * Reads all the data and returns it as a byte array.
   *
   * @return the data
   * @throws IOException if the data cannot be read
   */
  byte[] read() throws IOException;

  /**
   * Reads the {@code length} bytes of data starting at the given {@code offset}.
   *
   * @param offset the offset from which data should be read
   * @param length the number of bytes to be read
   * @return the data
   * @throws IOException if the data cannot be read
   * @throws IndexOutOfBoundsException if offset is beyond the end of the file or
   * subsection
   * @throws EOFException if offset plus length is greater than the length of the file
   * or subsection
   */
  byte[] read(long offset, long length) throws IOException;

  /**
   * Returns the size of the data.
   *
   * @return the size
   */
  long getSize();

}
