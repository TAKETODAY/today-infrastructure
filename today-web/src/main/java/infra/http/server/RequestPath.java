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

package infra.http.server;

import org.jspecify.annotations.Nullable;

import java.net.URI;

/**
 * Specialization of {@link PathContainer} that sub-divides the path into a
 * {@link #contextPath()} and the remaining {@link #pathWithinApplication()}.
 * The latter is typically used for request mapping within the application
 * while the former is useful when preparing external links that point back to
 * the application.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class RequestPath extends PathContainer {

  /**
   * Returns the portion of the URL path that represents the application.
   * The context path is always at the beginning of the path and starts but
   * does not end with "/". It is shared for URLs of the same application.
   */
  public abstract PathContainer contextPath();

  /**
   * The portion of the request path after the context path which is typically
   * used for request mapping within the application .
   */
  public abstract PathContainer pathWithinApplication();

  /**
   * Return a new {@code RequestPath} instance with a modified context path.
   * The new context path must match 0 or more path segments at the start.
   *
   * @param contextPath the new context path
   * @return a new {@code RequestPath} instance
   */
  public abstract RequestPath modifyContextPath(@Nullable String contextPath);

  /**
   * Parse the URI for a request into a {@code RequestPath}.
   *
   * @param uri the URI of the request
   * @param contextPath the contextPath portion of the URI path
   */
  public static RequestPath parse(URI uri, @Nullable String contextPath) {
    return parse(uri.getRawPath(), contextPath);
  }

  /**
   * Variant of {@link #parse(URI, String)} with the encoded
   * {@link URI#getRawPath() raw path}.
   *
   * @param rawPath the path
   * @param contextPath the contextPath portion of the URI path
   */
  public static RequestPath parse(String rawPath, @Nullable String contextPath) {
    return new DefaultRequestPath(rawPath, contextPath);
  }

}
