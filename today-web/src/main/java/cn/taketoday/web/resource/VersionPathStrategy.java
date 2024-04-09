/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.resource;

import cn.taketoday.lang.Nullable;

/**
 * A strategy for extracting and embedding a resource version in its URL path.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface VersionPathStrategy {

  /**
   * Extract the resource version from the request path.
   *
   * @param requestPath the request path to check
   * @return the version string or {@code null} if none was found
   */
  @Nullable
  String extractVersion(String requestPath);

  /**
   * Remove the version from the request path. It is assumed that the given
   * version was extracted via {@link #extractVersion(String)}.
   *
   * @param requestPath the request path of the resource being resolved
   * @param version the version obtained from {@link #extractVersion(String)}
   * @return the request path with the version removed
   */
  String removeVersion(String requestPath, String version);

  /**
   * Add a version to the given request path.
   *
   * @param requestPath the requestPath
   * @param version the version
   * @return the requestPath updated with a version string
   */
  String addVersion(String requestPath, String version);

}
