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
package cn.taketoday.mock.api.fileupload.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.taketoday.mock.api.fileupload.InvalidFileNameException;

/**
 * Utility class for working with streams.
 */
public final class Streams {

  /**
   * Private constructor, to prevent instantiation.
   * This class has only static methods.
   */
  private Streams() {
    // Does nothing
  }

  /**
   * Default buffer size for use in
   * {@link #copy(InputStream, OutputStream, boolean)}.
   */
  public static final int DEFAULT_BUFFER_SIZE = 8192;

  /**
   * Copies the contents of the given {@link InputStream}
   * to the given {@link OutputStream}. Shortcut for
   * <pre>
   *   copy(pInputStream, pOutputStream, new byte[8192]);
   * </pre>
   *
   * @param inputStream The input stream, which is being read.
   * It is guaranteed, that {@link InputStream#close()} is called
   * on the stream.
   * @param outputStream The output stream, to which data should
   * be written. May be null, in which case the input streams
   * contents are simply discarded.
   * @param closeOutputStream True guarantees, that {@link OutputStream#close()}
   * is called on the stream. False indicates, that only
   * {@link OutputStream#flush()} should be called finally.
   * @return Number of bytes, which have been copied.
   * @throws IOException An I/O error occurred.
   */
  public static long copy(final InputStream inputStream, final OutputStream outputStream,
          final boolean closeOutputStream)
          throws IOException {
    return copy(inputStream, outputStream, closeOutputStream, new byte[DEFAULT_BUFFER_SIZE]);
  }

  /**
   * Copies the contents of the given {@link InputStream}
   * to the given {@link OutputStream}.
   *
   * @param inputStream The input stream, which is being read.
   * It is guaranteed, that {@link InputStream#close()} is called
   * on the stream.
   * @param outputStream The output stream, to which data should
   * be written. May be null, in which case the input streams
   * contents are simply discarded.
   * @param closeOutputStream True guarantees, that {@link OutputStream#close()}
   * is called on the stream. False indicates, that only
   * {@link OutputStream#flush()} should be called finally.
   * @param buffer Temporary buffer, which is to be used for
   * copying data.
   * @return Number of bytes, which have been copied.
   * @throws IOException An I/O error occurred.
   */
  public static long copy(final InputStream inputStream,
          final OutputStream outputStream, final boolean closeOutputStream,
          final byte[] buffer)
          throws IOException {
    try (OutputStream out = outputStream;
            InputStream in = inputStream) {
      long total = 0;
      for (; ; ) {
        final int res = in.read(buffer);
        if (res == -1) {
          break;
        }
        if (res > 0) {
          total += res;
          if (out != null) {
            out.write(buffer, 0, res);
          }
        }
      }
      if (out != null) {
        if (closeOutputStream) {
          out.close();
        }
        else {
          out.flush();
        }
      }
      in.close();
      return total;
    }
  }

  /**
   * Checks, whether the given file name is valid in the sense,
   * that it doesn't contain any NUL characters. If the file name
   * is valid, it will be returned without any modifications. Otherwise,
   * an {@link InvalidFileNameException} is raised.
   *
   * @param fileName The file name to check
   * @return Unmodified file name, if valid.
   * @throws InvalidFileNameException The file name was found to be invalid.
   */
  public static String checkFileName(final String fileName) {
    if (fileName != null && fileName.indexOf('\u0000') != -1) {
      // pFileName.replace("\u0000", "\\0")
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < fileName.length(); i++) {
        final char c = fileName.charAt(i);
        switch (c) {
          case 0:
            sb.append("\\0");
            break;
          default:
            sb.append(c);
            break;
        }
      }
      throw new InvalidFileNameException(fileName,
              "Invalid file name: " + sb);
    }
    return fileName;
  }

}
