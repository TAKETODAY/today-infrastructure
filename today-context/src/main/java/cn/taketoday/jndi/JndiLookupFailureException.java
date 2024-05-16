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

package cn.taketoday.jndi;

import javax.naming.NamingException;

import cn.taketoday.core.NestedRuntimeException;

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
