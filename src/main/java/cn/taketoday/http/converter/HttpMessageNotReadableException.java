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

package cn.taketoday.http.converter;

import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Thrown by {@link HttpMessageConverter} implementations when the
 * {@link HttpMessageConverter#read} method fails.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class HttpMessageNotReadableException extends HttpMessageConversionException {

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
