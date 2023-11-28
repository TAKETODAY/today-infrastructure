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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http.codec.multipart;

import java.io.File;
import java.nio.file.Path;

import reactor.core.publisher.Mono;

/**
 * Specialization of {@link Part} that represents an uploaded file received in
 * a multipart request.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface FilePart extends Part {

  /**
   * Return the original filename in the client's filesystem.
   * <p><strong>Note:</strong> Please keep in mind this filename is supplied
   * by the client and should not be used blindly. In addition to not using
   * the directory portion, the file name could also contain characters such
   * as ".." and others that can be used maliciously. It is recommended to not
   * use this filename directly. Preferably generate a unique one and save
   * this one one somewhere for reference, if necessary.
   *
   * @return the original filename, or the empty String if no file has been chosen
   * in the multipart form, or {@code null} if not defined or not available
   * @see <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a>
   * @see <a href="https://owasp.org/www-community/vulnerabilities/Unrestricted_File_Upload">Unrestricted File Upload</a>
   */
  String filename();

  /**
   * Convenience method to copy the content of the file in this part to the
   * given destination file. If the destination file already exists, it will
   * be truncated first.
   * <p>The default implementation delegates to {@link #transferTo(Path)}.
   *
   * @param dest the target file
   * @return completion {@code Mono} with the result of the file transfer,
   * possibly {@link IllegalStateException} if the part isn't a file
   * @see #transferTo(Path)
   */
  default Mono<Void> transferTo(File dest) {
    return transferTo(dest.toPath());
  }

  /**
   * Convenience method to copy the content of the file in this part to the
   * given destination file. If the destination file already exists, it will
   * be truncated first.
   *
   * @param dest the target file
   * @return completion {@code Mono} with the result of the file transfer,
   * possibly {@link IllegalStateException} if the part isn't a file
   * @see #transferTo(File)
   */
  Mono<Void> transferTo(Path dest);

}
