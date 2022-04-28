/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.multipart.support;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.MultipartRequest;

/**
 * Abstract base implementation of the {@link MultipartRequest} interface.
 * <p>Provides management of pre-generated {@link MultipartFile} instances.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 17:16
 */
public abstract class AbstractMultipartRequest implements MultipartRequest {

  @Nullable
  private MultiValueMap<String, MultipartFile> multipartFiles;

  @Override
  public Iterator<String> getFileNames() {
    return getMultipartFiles().keySet().iterator();
  }

  @Override
  public MultipartFile getFile(String name) {
    return getMultipartFiles().getFirst(name);
  }

  @Override
  public List<MultipartFile> getFiles(String name) {
    List<MultipartFile> multipartFiles = getMultipartFiles().get(name);
    return Objects.requireNonNullElse(multipartFiles, Collections.emptyList());
  }

  @Override
  public Map<String, MultipartFile> getFileMap() {
    return getMultipartFiles().toSingleValueMap();
  }

  @Override
  public MultiValueMap<String, MultipartFile> getMultiFileMap() {
    return getMultipartFiles();
  }

  /**
   * Determine whether the underlying multipart request has been resolved.
   *
   * @return {@code true} when eagerly initialized or lazily triggered,
   * {@code false} in case of a lazy-resolution request that got aborted
   * before any parameters or multipart files have been accessed
   * @see #getMultipartFiles()
   */
  public boolean isResolved() {
    return multipartFiles != null;
  }

  /**
   * Set a Map with parameter names as keys and list of MultipartFile objects as values.
   * To be invoked by subclasses on initialization.
   */
  protected final void setMultipartFiles(MultiValueMap<String, MultipartFile> multipartFiles) {
    this.multipartFiles = multipartFiles;
  }

  /**
   * Obtain the MultipartFile Map for retrieval,
   * lazily initializing it if necessary.
   *
   * @see #initializeMultipart()
   */
  protected MultiValueMap<String, MultipartFile> getMultipartFiles() {
    if (this.multipartFiles == null) {
      initializeMultipart();
    }
    return this.multipartFiles;
  }

  /**
   * Lazily initialize the multipart request, if possible.
   * Only called if not already eagerly initialized.
   */
  protected void initializeMultipart() {
    throw new IllegalStateException("Multipart request not initialized");
  }

}
