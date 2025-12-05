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

package infra.web.server;

import infra.http.HttpStatus;
import infra.web.ResponseStatusException;

/**
 * Exception for errors that fit response status 413 (Content too large) for use in
 * Web applications.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@SuppressWarnings("serial")
public class RequestBodySizeExceededException extends ResponseStatusException {

  private final long maxContentLength;

  public RequestBodySizeExceededException(long maxContentLength) {
    super(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum request body size %sexceeded".formatted(maxContentLength >= 0 ? "of %d bytes ".formatted(maxContentLength) : ""), null);
    this.maxContentLength = maxContentLength;
  }

  public long getMaxContentLength() {
    return maxContentLength;
  }

}
