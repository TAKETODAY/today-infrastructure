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

package cn.taketoday.bytecode.core;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;

/**
 * @version $Id: CodeGenerationException.java,v 1.3 2004/06/24 21:15:21
 * herbyderby Exp $
 */
public class CodeGenerationException extends NestedRuntimeException {

  public CodeGenerationException(Throwable cause) {
    super(ExceptionUtils.unwrapIfNecessary(cause).toString(), cause);
  }

  public CodeGenerationException(@Nullable String message) {
    super(message);
  }

  public CodeGenerationException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

}
