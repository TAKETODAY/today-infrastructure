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

package cn.taketoday.web.multipart;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;
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
public interface MultipartFile extends Multipart, InputStreamSource {

  /**
   * Return the content type of the file.
   *
   * @return the content type, or {@code null} if not defined
   * (or no file has been chosen in the multipart form)
   */
  @Nullable
  @Override
  String getContentType();

  /**
   * Return the size of the file in bytes.
   *
   * @return the size of the file, or 0 if empty
   */
  long getSize();

  /**
   * Return the name of the parameter in the multipart form.
   *
   * @return the name of the parameter (never {@code null} or empty)
   */
  @Override
  String getName();

  /**
   * Return the original filename in the client's filesystem.
   * <p>This may contain path information depending on the browser used,
   * but it typically will not with any other than Opera.
   * <p><strong>Note:</strong> Please keep in mind this filename is supplied
   * by the client and should not be used blindly. In addition to not using
   * the directory portion, the file name could also contain characters such
   * as ".." and others that can be used maliciously. It is recommended to not
   * use this filename directly. Preferably generate a unique one and save
   * this one somewhere for reference, if necessary.
   *
   * @return the original filename, or the empty String if no file has been chosen
   * in the multipart form, or {@code null} if not defined or not available
   * @see <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a>
   * @see <a href="https://owasp.org/www-community/vulnerabilities/Unrestricted_File_Upload">Unrestricted File Upload</a>
   */
  @Nullable
  String getOriginalFilename();

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
  @Override
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
  @Override
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
   * <p>This may either move the file in the filesystem, copy the file in the
   * filesystem, or save memory-held contents to the destination file. If the
   * destination file already exists, it will be deleted first.
   * <p>If the target file has been moved in the filesystem, this operation
   * cannot be invoked again afterwards. Therefore, call this method just once
   * in order to work with any storage mechanism.
   * <p><b>NOTE:</b> Depending on the underlying provider, temporary storage
   * may be container-dependent, including the base directory for relative
   * destinations specified here (e.g. with Web multipart handling).
   * For absolute destinations, the target file may get renamed/moved from its
   * temporary location or newly copied, even if a temporary copy already exists.
   *
   * @param dest the destination file (typically absolute)
   * @throws IOException in case of reading or writing errors
   * @throws IllegalStateException if the file has already been moved
   * in the filesystem and is not available anymore for another transfer
   */
  void transferTo(File dest) throws IOException, IllegalStateException;

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
