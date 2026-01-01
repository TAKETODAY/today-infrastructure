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

package infra.web.multipart;

import infra.web.RequestContext;

/**
 * Strategy interface for parsing multipart requests.
 *
 * <p>Implementations of this interface are responsible for parsing
 * multipart requests and creating {@link MultipartRequest} objects
 * that provide access to the parsed parts.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/2 15:02
 */
public interface MultipartParser {

  /**
   * Parse the given multipart request context into a MultipartRequest.
   *
   * @param request the request context to parse
   * @return the parsed MultipartRequest
   * @throws MultipartException if parsing fails
   */
  MultipartRequest parse(RequestContext request) throws MultipartException;

}
