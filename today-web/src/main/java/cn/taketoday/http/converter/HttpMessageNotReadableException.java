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

package cn.taketoday.http.converter;

import java.io.Serial;

import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private final HttpInputMessage httpInputMessage;

  /**
   * Create a new HttpMessageNotReadableException.
   *
   * @param msg the detail message
   */
  public HttpMessageNotReadableException(String msg) {
    super(msg);
    this.httpInputMessage = null;
  }

  /**
   * Create a new HttpMessageNotReadableException.
   *
   * @param msg the detail message
   * @param cause the root cause (if any)
   */
  public HttpMessageNotReadableException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
    this.httpInputMessage = null;
  }

  /**
   * Create a new HttpMessageNotReadableException.
   *
   * @param msg the detail message
   * @param httpInputMessage the original HTTP message
   */
  public HttpMessageNotReadableException(String msg, HttpInputMessage httpInputMessage) {
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
  public HttpMessageNotReadableException(String msg, @Nullable Throwable cause, HttpInputMessage httpInputMessage) {
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
