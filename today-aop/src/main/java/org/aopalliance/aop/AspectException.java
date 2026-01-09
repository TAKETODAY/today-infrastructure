/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package org.aopalliance.aop;

import java.io.Serial;

/**
 * Superclass for all AOP infrastructure exceptions. Unchecked, as such
 * exceptions are fatal and end user code shouldn't be forced to catch them.
 *
 * @author Rod Johnson
 * @author Bob Lee
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class AspectException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 371663334385751868L;

  /**
   * Constructor for AspectException.
   */
  public AspectException(String s) {
    super(s);
  }

  /**
   * Constructor for AspectException.
   */
  public AspectException(String s, Throwable t) {
    super(s, t);
  }

}
