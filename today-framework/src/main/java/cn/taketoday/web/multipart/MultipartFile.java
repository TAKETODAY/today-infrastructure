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

import cn.taketoday.core.io.InputStreamSource;

/**
 * @author TODAY <br>
 * 2018-07-11 13:02:52
 */
public interface MultipartFile extends Serializable, InputStreamSource {

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
  String getFileName();

  /**
   * Save upload file to server.
   *
   * @param dest the destination file path
   * @throws IOException if an error occurs when write to dest.
   */
  void save(File dest) throws IOException;

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

}
