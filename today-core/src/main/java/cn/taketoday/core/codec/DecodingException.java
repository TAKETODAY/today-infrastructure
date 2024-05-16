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

package cn.taketoday.core.codec;

import cn.taketoday.lang.Nullable;

/**
 * Indicates an issue with decoding the input stream with a focus on content
 * related issues such as a parse failure. As opposed to more general I/O
 * errors, illegal state, or a {@link CodecException} such as a configuration
 * issue that a {@link Decoder} may choose to raise.
 *
 * <p>For example in server web application, a {@code DecodingException} would
 * translate to a response with a 400 (bad input) status while
 * {@code CodecException} would translate to 500 (server error) status.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Decoder
 * @since 4.0
 */
public class DecodingException extends CodecException {

  /**
   * Create a new DecodingException.
   *
   * @param msg the detail message
   */
  public DecodingException(String msg) {
    super(msg);
  }

  /**
   * Create a new DecodingException.
   *
   * @param msg the detail message
   * @param cause root cause for the exception, if any
   */
  public DecodingException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
