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

package cn.taketoday.http.server;

import java.net.URI;

import cn.taketoday.lang.Nullable;

/**
 * Specialization of {@link PathContainer} that sub-divides the path into a
 * {@link #contextPath()} and the remaining {@link #pathWithinApplication()}.
 * The latter is typically used for request mapping within the application
 * while the former is useful when preparing external links that point back to
 * the application.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface RequestPath extends PathContainer {

  /**
   * Returns the portion of the URL path that represents the application.
   * The context path is always at the beginning of the path and starts but
   * does not end with "/". It is shared for URLs of the same application.
   * <p>The context path may come from the underlying runtime API such as
   * when deploying as a WAR to a Servlet container or it may be assigned in
   * a WebFlux application through the use of
   * {@link cn.taketoday.http.server.reactive.ContextPathCompositeHandler
   * ContextPathCompositeHandler}.
   */
  PathContainer contextPath();

  /**
   * The portion of the request path after the context path which is typically
   * used for request mapping within the application .
   */
  PathContainer pathWithinApplication();

  /**
   * Return a new {@code RequestPath} instance with a modified context path.
   * The new context path must match 0 or more path segments at the start.
   *
   * @param contextPath the new context path
   * @return a new {@code RequestPath} instance
   */
  RequestPath modifyContextPath(String contextPath);

  /**
   * Parse the URI for a request into a {@code RequestPath}.
   *
   * @param uri the URI of the request
   * @param contextPath the contextPath portion of the URI path
   */
  static RequestPath parse(URI uri, @Nullable String contextPath) {
    return parse(uri.getRawPath(), contextPath);
  }

  /**
   * Variant of {@link #parse(URI, String)} with the encoded
   * {@link URI#getRawPath() raw path}.
   *
   * @param rawPath the path
   * @param contextPath the contextPath portion of the URI path
   */
  static RequestPath parse(String rawPath, @Nullable String contextPath) {
    return new DefaultRequestPath(rawPath, contextPath);
  }

}
