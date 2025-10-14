/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.multipart.support;

import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import infra.util.MultiValueMap;
import infra.web.multipart.Multipart;
import infra.web.multipart.MultipartFile;
import infra.web.multipart.MultipartRequest;
import infra.web.util.WebUtils;

/**
 * Abstract base implementation of the {@link MultipartRequest} interface.
 * <p>Provides management of pre-generated {@link MultipartFile} instances.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 17:16
 */
public abstract class AbstractMultipartRequest implements MultipartRequest {

  private @Nullable MultiValueMap<String, Multipart> parts;

  private @Nullable MultiValueMap<String, MultipartFile> multipartFiles;

  @Override
  public Iterator<String> getFileNames() {
    return getMultipartFiles().keySet().iterator();
  }

  @Override
  public @Nullable MultipartFile getFile(String name) {
    return getMultipartFiles().getFirst(name);
  }

  @Override
  public @Nullable List<MultipartFile> getFiles(String name) {
    return getMultipartFiles().get(name);
  }

  @Override
  public @Nullable List<Multipart> multipartData(String name) {
    return multipartData().get(name);
  }

  @Override
  public Map<String, MultipartFile> getFileMap() {
    return getMultipartFiles().toSingleValueMap();
  }

  /**
   * Obtain the MultipartFile Map for retrieval,
   * lazily initializing it if necessary.
   *
   * @see #parseRequest()
   */
  @Override
  public MultiValueMap<String, MultipartFile> getMultipartFiles() {
    var multipartFiles = this.multipartFiles;
    if (multipartFiles == null) {
      multipartFiles = MultiValueMap.forLinkedHashMap();
      for (Map.Entry<String, List<Multipart>> entry : multipartData().entrySet()) {
        for (Multipart multipart : entry.getValue()) {
          if (!multipart.isFormField()) {
            multipartFiles.add(entry.getKey(), (MultipartFile) multipart);
          }
        }
      }
      this.multipartFiles = multipartFiles;
    }
    return multipartFiles;
  }

  @Override
  public MultiValueMap<String, Multipart> multipartData() {
    var parts = this.parts;
    if (parts == null) {
      parts = parseRequest();
      this.parts = parts;
    }
    return parts;
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
    return parts != null;
  }

  @Override
  public void cleanup() {
    WebUtils.cleanupMultipartRequest(parts);
  }

  /**
   * Lazily initialize the multipart request, if possible.
   * Only called if not already eagerly initialized.
   */
  protected abstract MultiValueMap<String, Multipart> parseRequest();

}
