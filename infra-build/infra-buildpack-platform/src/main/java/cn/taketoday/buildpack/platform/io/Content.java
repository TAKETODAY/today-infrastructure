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

package cn.taketoday.buildpack.platform.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.FileCopyUtils;

/**
 * Content with a known size that can be written to an {@link OutputStream}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public interface Content {

  /**
   * The size of the content in bytes.
   *
   * @return the content size
   */
  int size();

  /**
   * Write the content to the given output stream.
   *
   * @param outputStream the output stream to write to
   * @throws IOException on IO error
   */
  void writeTo(OutputStream outputStream) throws IOException;

  /**
   * Create a new {@link Content} from the given UTF-8 string.
   *
   * @param string the string to write
   * @return a new {@link Content} instance
   */
  static Content of(String string) {
    Assert.notNull(string, "String is required");
    return of(string.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Create a new {@link Content} from the given input stream.
   *
   * @param bytes the bytes to write
   * @return a new {@link Content} instance
   */
  static Content of(byte[] bytes) {
    Assert.notNull(bytes, "Bytes is required");
    return of(bytes.length, () -> new ByteArrayInputStream(bytes));
  }

  /**
   * Create a new {@link Content} from the given file.
   *
   * @param file the file to write
   * @return a new {@link Content} instance
   */
  static Content of(File file) {
    Assert.notNull(file, "File is required");
    return of((int) file.length(), () -> new FileInputStream(file));
  }

  /**
   * Create a new {@link Content} from the given input stream. The stream will be closed
   * after it has been written.
   *
   * @param size the size of the supplied input stream
   * @param supplier the input stream supplier
   * @return a new {@link Content} instance
   */
  static Content of(int size, IOSupplier<InputStream> supplier) {
    Assert.isTrue(size >= 0, "Size must not be negative");
    Assert.notNull(supplier, "Supplier is required");
    return new Content() {

      @Override
      public int size() {
        return size;
      }

      @Override
      public void writeTo(OutputStream outputStream) throws IOException {
        FileCopyUtils.copy(supplier.get(), outputStream);
      }

    };
  }

}
