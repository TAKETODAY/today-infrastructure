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

package infra.http.converter;

import org.jspecify.annotations.Nullable;

import infra.http.HttpInputMessage;
import infra.lang.Assert;

/**
 * Thrown by {@link HttpMessageConverter} implementations when the
 * {@link HttpMessageConverter#read} method fails.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HttpMessageNotReadableException extends HttpMessageConversionException {

  @Nullable
  private final HttpInputMessage httpInputMessage;

  /**
   * Create a new HttpMessageNotReadableException.
   *
   * @param msg the detail message
   * @param httpInputMessage the original HTTP message
   */
  public HttpMessageNotReadableException(@Nullable String msg, @Nullable HttpInputMessage httpInputMessage) {
    super(msg);
    this.httpInputMessage = httpInputMessage;
  }

  /**
   * Create a new HttpMessageNotReadableException.
   *
   * @param msg the detail message
   * @param cause the root cause (if any)
   * @param httpInputMessage the original HTTP message
   */
  public HttpMessageNotReadableException(@Nullable String msg, @Nullable Throwable cause, @Nullable HttpInputMessage httpInputMessage) {
    super(msg, cause);
    this.httpInputMessage = httpInputMessage;
  }

  /**
   * Return the original HTTP message.
   */
  public HttpInputMessage getHttpInputMessage() {
    Assert.state(this.httpInputMessage != null, "No HttpInputMessage available - use non-deprecated constructors");
    return this.httpInputMessage;
  }

}
