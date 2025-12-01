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

import infra.http.DefaultHttpHeaders;
import infra.http.HttpHeaders;
import infra.web.multipart.Part;

/**
 * Abstract base class for handling multipart data in "multipart/form-data" requests.
 * This class provides a foundation for implementing parts of a multipart request,
 * such as form fields or file uploads. It implements the {@link Part} interface
 * and provides default behavior for common operations like header management and
 * string representation.
 *
 * <p>Subclasses are expected to implement specific behavior for handling multipart
 * data, such as retrieving bytes, determining if the part is a form field, or
 * cleaning up resources.
 *
 * <p><strong>Header Management:</strong>
 * The {@link #getHeaders()} method lazily initializes the headers for the part.
 * If no headers are explicitly set, it creates default headers using the
 * {@link #createHttpHeaders()} method. Subclasses can override this method to
 * customize header creation.
 *
 * <p><strong>Thread Safety:</strong>
 * This class is not inherently thread-safe. If used in a multithreaded environment,
 * care must be taken to ensure proper synchronization when accessing or modifying
 * shared state, such as headers.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Part
 * @since 4.0 2022/5/13 11:06
 */
public abstract class AbstractPart implements Part {

  protected @Nullable HttpHeaders headers;

  @Override
  public HttpHeaders getHeaders() {
    HttpHeaders headers = this.headers;
    if (headers == null) {
      headers = createHttpHeaders();
      this.headers = headers;
    }
    return headers;
  }

  protected HttpHeaders createHttpHeaders() {
    DefaultHttpHeaders headers = HttpHeaders.forWritable();
    String contentType = getContentType();
    if (contentType != null) {
      headers.setOrRemove(HttpHeaders.CONTENT_TYPE, contentType);
    }
    return headers;
  }

  @Override
  public String toString() {
    return "%s: %s=%s".formatted(getClass().getSimpleName(), getName(), getValue());
  }

}
