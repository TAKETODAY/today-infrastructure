/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
package cn.taketoday.bytecode.beans;

import java.io.Serial;

public class BulkBeanException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  private int index;
  private Throwable cause;

  public BulkBeanException(String message, int index) {
    super(message);
    this.index = index;
  }

  public BulkBeanException(Throwable cause, int index) {
    super(cause.getMessage());
    this.index = index;
    this.cause = cause;
  }

  public int getIndex() {
    return index;
  }

  public Throwable getCause() {
    return cause;
  }
}
