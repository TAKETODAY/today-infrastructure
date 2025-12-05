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

import infra.http.HttpHeaders;
import infra.util.MultiValueMap;
import infra.web.multipart.MultipartRequest;
import infra.web.multipart.Part;
import infra.web.util.WebUtils;

/**
 * Abstract base implementation of the {@link MultipartRequest} interface.
 * <p>Provides management of pre-generated {@link Part} instances.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 17:16
 */
public abstract class AbstractMultipartRequest implements MultipartRequest {

  private @Nullable MultiValueMap<String, Part> parts;

  @Override
  public Iterable<String> getPartNames() {
    return getParts().keySet();
  }

  @Override
  public @Nullable List<Part> getParts(String name) {
    return getParts().get(name);
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
   */
  public boolean isResolved() {
    return parts != null;
  }

  @Override
  public @Nullable HttpHeaders getHeaders(String name) {
    Part part = getPart(name);
    return part != null ? part.getHeaders() : null;
  }

  @Override
  public void cleanup() {
    WebUtils.cleanupMultipartRequest(parts);
    parts = null;
  }

  /**
   * Lazily initialize the multipart request, if possible.
   * Only called if not already eagerly initialized.
   */
  protected abstract MultiValueMap<String, Part> parseRequest();

}
