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

import org.jspecify.annotations.Nullable;

import infra.http.HttpStatus;
import infra.web.ResponseStatusException;

/**
 * Exception raised when an API version cannot be parsed, or is not in the
 * supported version set.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@SuppressWarnings("serial")
public class InvalidApiVersionException extends ResponseStatusException {

  private final String version;

  public InvalidApiVersionException(String version) {
    this(version, null, null);
  }

  public InvalidApiVersionException(String version, @Nullable String msg, @Nullable Exception cause) {
    super(HttpStatus.BAD_REQUEST, (msg != null ? msg : "Invalid API version: '" + version + "'."), cause);
    this.version = version;
  }

  /**
   * Return the requested version.
   */
  public String getVersion() {
    return this.version;
  }

}
