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

import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpHeaders;

/**
 * Extended interface for a {@link Resource} to be written to an
 * HTTP response.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface HttpResource extends Resource {

  /**
   * The HTTP headers to be contributed to the HTTP response
   * that serves the current resource.
   *
   * @return the HTTP response headers
   */
  HttpHeaders getResponseHeaders();

}
