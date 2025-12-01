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

import java.util.List;
import java.util.Map;

import infra.util.MultiValueMap;
import infra.web.multipart.MultipartFile;
import infra.web.multipart.MultipartRequest;
import infra.web.multipart.Part;
import infra.web.util.WebUtils;

/**
 * Abstract base implementation of the {@link MultipartRequest} interface.
 * <p>Provides management of pre-generated {@link MultipartFile} instances.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 17:16
 */
public abstract class AbstractMultipartRequest implements MultipartRequest {

  private @Nullable MultiValueMap<String, Part> parts;

  private @Nullable MultiValueMap<String, MultipartFile> multipartFiles;

  @Override
  public Iterable<String> getFileNames() {
    return getFiles().keySet();
  }

  @Override
  public Iterable<String> getPartNames() {
    return getParts().keySet();
  }

  @Override
  public @Nullable MultipartFile getFile(String name) {
    return getFiles().getFirst(name);
  }

  @Override
  public @Nullable List<MultipartFile> getFiles(String name) {
    return getFiles().get(name);
  }

  @Override
  public @Nullable List<Part> getParts(String name) {
    return getParts().get(name);
  }

  @Override
  public Map<String, MultipartFile> getFileMap() {
    return getFiles().toSingleValueMap();
  }

  /**
   * Obtain the MultipartFile Map for retrieval,
   * lazily initializing it if necessary.
   *
   * @see #parseRequest()
   */
  @Override
  public MultiValueMap<String, MultipartFile> getFiles() {
    var multipartFiles = this.multipartFiles;
    if (multipartFiles == null) {
      multipartFiles = MultiValueMap.forLinkedHashMap();
      for (Map.Entry<String, List<Part>> entry : getParts().entrySet()) {
        for (Part part : entry.getValue()) {
          if (!part.isFormField()) {
            multipartFiles.add(entry.getKey(), (MultipartFile) part);
          }
        }
      }
      this.multipartFiles = multipartFiles;
    }
    return multipartFiles;
  }

  @Override
  public MultiValueMap<String, Part> getParts() {
    var parts = this.parts;
    if (parts == null) {
      parts = parseRequest();
      this.parts = parts;
    }
    return parts;
  }

  @Override
  public @Nullable Part getPart(String name) {
    return getParts().getFirst(name);
  }

  /**
   * Determine whether the underlying multipart request has been resolved.
   *
   * @return {@code true} when eagerly initialized or lazily triggered,
   * {@code false} in case of a lazy-resolution request that got aborted
   * before any parameters or multipart files have been accessed
   * @see #getFiles()
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
  protected abstract MultiValueMap<String, Part> parseRequest();

}
