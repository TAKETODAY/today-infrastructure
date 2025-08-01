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

package infra.web.accept;

import infra.web.RequestContext;

/**
 * Contract to add handling of requests with a deprecated API version. Typically,
 * this involves use of response headers to send hints and information about
 * the deprecated version to clients.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see StandardApiVersionDeprecationHandler
 * @since 5.0
 */
public interface ApiVersionDeprecationHandler {

  /**
   * Check if the requested API version is deprecated, and if so handle it
   * accordingly, e.g. by setting response headers to signal the deprecation,
   * to specify relevant dates and provide links to further details.
   *
   * @param version the resolved and parsed request version
   * @param request the current request
   */
  void handleVersion(Comparable<?> version, RequestContext request);

}
