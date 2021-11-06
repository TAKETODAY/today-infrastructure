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

package cn.taketoday.core.codec;

import cn.taketoday.lang.Nullable;

/**
 * Indicates an issue with encoding the input Object stream with a focus on
 * not being able to encode Objects. As opposed to a more general I/O errors
 * or a {@link CodecException} such as a configuration issue that an
 * {@link Encoder} may also choose to raise.
 *
 * @author Rossen Stoyanchev
 * @see Encoder
 * @since 4.0
 */
@SuppressWarnings("serial")
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
