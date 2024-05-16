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

package cn.taketoday.web.bind.resolver;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.bind.MissingRequestValueException;

/**
 * Raised when the part of a "multipart/form-data" request identified by its
 * name cannot be found.
 *
 * <p>This may be because the request is not a multipart/form-data request,
 * because the part is not present in the request, or because the web
 * application is not configured correctly for processing multipart requests,
 * e.g. no {@code MultipartResolver}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MissingRequestPartException extends MissingRequestValueException implements ErrorResponse {

  private final String requestPartName;

  /**
   * Constructor for MissingRequestPartException.
   *
   * @param requestPartName the name of the missing part of the multipart request
   */
  public MissingRequestPartException(String requestPartName) {
    super("Required part '%s' is not present.".formatted(requestPartName), false, null, new Object[] { requestPartName });
    this.requestPartName = requestPartName;
    getBody().setDetail(getMessage());
  }

  /**
   * Return the name of the offending part of the multipart request.
   */
  public String getRequestPartName() {
    return this.requestPartName;
  }

  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.BAD_REQUEST;
  }

}
