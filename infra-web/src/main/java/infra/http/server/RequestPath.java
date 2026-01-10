/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
