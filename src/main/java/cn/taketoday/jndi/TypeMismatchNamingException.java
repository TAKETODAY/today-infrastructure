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

package cn.taketoday.jndi;

import javax.naming.NamingException;

/**
 * Exception thrown if a type mismatch is encountered for an object
 * located in a JNDI environment. Thrown by JndiTemplate.
 *
 * @author Juergen Hoeller
 * @see JndiTemplate#lookup(String, Class)
 * @since 4.0
 */
@SuppressWarnings("serial")
public class TypeMismatchNamingException extends NamingException {

  private final Class<?> requiredType;

  private final Class<?> actualType;

  /**
   * Construct a new TypeMismatchNamingException,
   * building an explanation text from the given arguments.
   *
   * @param jndiName the JNDI name
   * @param requiredType the required type for the lookup
   * @param actualType the actual type that the lookup returned
   */
  public TypeMismatchNamingException(String jndiName, Class<?> requiredType, Class<?> actualType) {
    super("Object of type [" + actualType + "] available at JNDI location [" +
            jndiName + "] is not assignable to [" + requiredType.getName() + "]");
    this.requiredType = requiredType;
    this.actualType = actualType;
  }

  /**
   * Return the required type for the lookup, if available.
   */
  public final Class<?> getRequiredType() {
    return this.requiredType;
  }

  /**
   * Return the actual type that the lookup returned, if available.
   */
  public final Class<?> getActualType() {
    return this.actualType;
  }

}
