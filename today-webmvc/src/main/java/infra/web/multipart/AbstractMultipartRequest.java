/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.multipart;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.http.HttpHeaders;
import infra.util.MultiValueMap;
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
