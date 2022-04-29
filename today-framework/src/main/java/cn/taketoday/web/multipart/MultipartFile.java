/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.multipart;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.FileCopyUtils;

/**
 * A representation of an uploaded file received in a multipart request.
 *
 * <p>The file contents are either stored in memory or temporarily on disk.
 * In either case, the user is responsible for copying file contents to a
 * session-level or persistent store as and if desired. The temporary storage
 * will be cleared at the end of request processing.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MultipartRequest
 * @since 2018-07-11 13:02:52
 */
public interface MultipartFile extends Multipart, Serializable, InputStreamSource {

  /**
   * Get upload file content type.
   *
   * @return upload file content type
   */
  String getContentType();

  /**
   * Return the size of the file in bytes.
   *
   * @return the size of the file, or 0 if empty
   */
  long getSize();

  /**
   * Gets the name of this part.
   *
   * @return The name of this part as a {@code String}
   */
  String getName();

  /**
   * Return the original filename in the client's file system.
   */
  String getOriginalFilename();

  /**
   * Transfer the received file to the given destination file.
   * <p>This may either move the file in the filesystem, copy the file in the
   * filesystem, or save memory-held contents to the destination file. If the
   * destination file already exists, it will be deleted first.
   * <p>If the target file has been moved in the filesystem, this operation
   * cannot be invoked again afterwards. Therefore, call this method just once
   * in order to work with any storage mechanism.
   * <p><b>NOTE:</b> Depending on the underlying provider, temporary storage
   * may be container-dependent, including the base directory for relative
   * destinations specified here (e.g. with Servlet multipart handling).
   * For absolute destinations, the target file may get renamed/moved from its
   * temporary location or newly copied, even if a temporary copy already exists.
   *
   * @param dest the destination file (typically absolute)
   * @throws IOException in case of reading or writing errors
   * @throws IllegalStateException if the file has already been moved
   * in the filesystem and is not available anymore for another transfer
   * @see jakarta.servlet.http.Part#write(String)
   */
  void transferTo(File dest) throws IOException, IllegalStateException;

  /**
   * Return whether the uploaded file is empty, that is, either no file has
   * been chosen in the multipart form or the chosen file has no content.
   */
  boolean isEmpty();

  /**
   * Returns the contents of the file item as an array of bytes.
   *
   * @throws IOException If any IO exception occurred
   * @since 2.3.3
   */
  byte[] getBytes() throws IOException;

  /**
   * Get original resource
   *
   * @return Original resource
   * @since 2.3.3
   */
  Object getOriginalResource();

  /**
   * Deletes the underlying storage for a file item, including deleting any
   * associated temporary disk file.
   *
   * @throws IOException if an error occurs.
   * @since 2.3.3
   */
  void delete() throws IOException;

  /**
   * Return a Resource representation of this MultipartFile. This can be used
   * as input to the {@code RestTemplate} or the {@code WebClient} to expose
   * content length and the filename along with the InputStream.
   *
   * @return this MultipartFile adapted to the Resource contract
   * @since 4.0
   */
  default Resource getResource() {
    return new MultipartFileResource(this);
  }

  /**
   * Transfer the received file to the given destination file.
   * <p>The default implementation simply copies the file input stream.
   *
   * @see #getInputStream()
   * @see #transferTo(File)
   * @since 4.0
   */
  default void transferTo(Path dest) throws IOException, IllegalStateException {
    FileCopyUtils.copy(getInputStream(), Files.newOutputStream(dest));
  }

}
