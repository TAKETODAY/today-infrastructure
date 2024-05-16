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
 * Indicates an issue with encoding the input Object stream with a focus on
 * not being able to encode Objects. As opposed to a more general I/O errors
 * or a {@link CodecException} such as a configuration issue that an
 * {@link Encoder} may also choose to raise.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Encoder
 * @since 4.0
 */
public class EncodingException extends CodecException {

  /**
   * Create a new EncodingException.
   *
   * @param msg the detail message
   */
  public EncodingException(String msg) {
    super(msg);
  }

  /**
   * Create a new EncodingException.
   *
   * @param msg the detail message
   * @param cause root cause for the exception, if any
   */
  public EncodingException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
