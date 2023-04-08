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

import java.io.Serial;

import cn.taketoday.core.NestedRuntimeException;

/**
 * Root of the hierarchy of Object XML Mapping exceptions.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public abstract class XmlMappingException extends NestedRuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Construct an {@code XmlMappingException} with the specified detail message.
   *
   * @param msg the detail message
   */
  public XmlMappingException(String msg) {
    super(msg);
  }

  /**
   * Construct an {@code XmlMappingException} with the specified detail message
   * and nested exception.
   *
   * @param msg the detail message
   * @param cause the nested exception
   */
  public XmlMappingException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
