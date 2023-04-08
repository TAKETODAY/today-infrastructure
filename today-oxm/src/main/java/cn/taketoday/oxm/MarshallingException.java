/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.oxm;

/**
 * Base class for exception thrown when a marshalling or unmarshalling error occurs.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see MarshallingFailureException
 * @see UnmarshallingFailureException
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class MarshallingException extends XmlMappingException {

  /**
   * Construct a {@code MarshallingException} with the specified detail message.
   *
   * @param msg the detail message
   */
  protected MarshallingException(String msg) {
    super(msg);
  }

  /**
   * Construct a {@code MarshallingException} with the specified detail message
   * and nested exception.
   *
   * @param msg the detail message
   * @param cause the nested exception
   */
  protected MarshallingException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
