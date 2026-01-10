/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode.core;

import org.jspecify.annotations.Nullable;

import infra.core.NestedRuntimeException;
import infra.util.ExceptionUtils;

/**
 * @version $Id: CodeGenerationException.java,v 1.3 2004/06/24 21:15:21
 * herbyderby Exp $
 */
public class CodeGenerationException extends NestedRuntimeException {

  public CodeGenerationException(Throwable cause) {
    super(ExceptionUtils.unwrapIfNecessary(cause).toString(), cause);
  }

  public CodeGenerationException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

}
