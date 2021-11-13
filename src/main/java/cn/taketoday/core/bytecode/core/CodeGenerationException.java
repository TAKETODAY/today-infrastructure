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
package cn.taketoday.core.bytecode.core;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.util.ExceptionUtils;

import java.io.Serial;

/**
 * @version $Id: CodeGenerationException.java,v 1.3 2004/06/24 21:15:21
 * herbyderby Exp $
 */
public class CodeGenerationException extends NestedRuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public CodeGenerationException() { }

  public CodeGenerationException(Throwable cause) {
    super(ExceptionUtils.unwrapThrowable(cause).toString(), cause);
  }

  public CodeGenerationException(String message) {
    super(message);
  }

  public CodeGenerationException(String message, Throwable cause) {
    super(message, cause);
  }

}
