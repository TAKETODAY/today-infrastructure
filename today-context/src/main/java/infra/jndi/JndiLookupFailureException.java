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

package infra.jndi;

import javax.naming.NamingException;

import infra.core.NestedRuntimeException;

/**
 * RuntimeException to be thrown in case of JNDI lookup failures,
 * in particular from code that does not declare JNDI's checked
 * {@link NamingException}: for example, from
 * {@link JndiObjectTargetSource}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JndiLookupFailureException extends NestedRuntimeException {

  /**
   * Construct a new JndiLookupFailureException,
   * wrapping the given JNDI NamingException.
   *
   * @param msg the detail message
   * @param cause the NamingException root cause
   */
  public JndiLookupFailureException(String msg, NamingException cause) {
    super(msg, cause);
  }

}
