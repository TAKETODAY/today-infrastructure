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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * A TAR archive that can be written to an output stream.
 *
 * @author Phillip Webb
 * @since 4.0
 */
@FunctionalInterface
public interface TarArchive {

  /**
   * {@link Instant} that can be used to normalize TAR files so all entries have the
   * same modification time.
   */
  Instant NORMALIZED_TIME = OffsetDateTime.of(1980, 1, 1, 0, 0, 1, 0, ZoneOffset.UTC).toInstant();

  /**
   * Write the TAR archive to the given output stream.
   *
   * @param outputStream the output stream to write to
   * @throws IOException on IO error
   */
  void writeTo(OutputStream outputStream) throws IOException;

  /**
   * Factory method to create a new {@link TarArchive} instance with a specific layout.
   *
   * @param layout the TAR layout
   * @return a new {@link TarArchive} instance
   */
  static TarArchive of(IOConsumer<Layout> layout) {
    return (outputStream) -> {
      TarLayoutWriter writer = new TarLayoutWriter(outputStream);
      layout.accept(writer);
      writer.finish();
    };
  }

  /**
   * Factory method to adapt a ZIP file to {@link TarArchive}.
   *
   * @param zip the source zip file
   * @param owner the owner of the entries in the TAR
   * @return a new {@link TarArchive} instance
   */
  static TarArchive fromZip(File zip, Owner owner) {
    return new ZipFileTarArchive(zip, owner);
  }

}
