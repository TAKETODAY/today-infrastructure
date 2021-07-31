/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.cglib.core;

import cn.taketoday.context.NestedRuntimeException;
import cn.taketoday.context.utils.ExceptionUtils;

/**
 * @version $Id: CodeGenerationException.java,v 1.3 2004/06/24 21:15:21
 * herbyderby Exp $
 */
public class CodeGenerationException extends NestedRuntimeException {
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
