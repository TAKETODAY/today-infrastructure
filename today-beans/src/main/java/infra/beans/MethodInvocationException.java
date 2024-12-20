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

package infra.beans;

import java.beans.PropertyChangeEvent;

/**
 * Thrown when a bean property getter or setter method throws an exception,
 * analogous to an InvocationTargetException.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Rod Johnson
 * @since 4.0 2022/2/17 17:35
 */
public class MethodInvocationException extends PropertyAccessException {

  /**
   * Error code that a method invocation error will be registered with.
   */
  public static final String ERROR_CODE = "methodInvocation";

  /**
   * Create a new MethodInvocationException.
   *
   * @param propertyChangeEvent the PropertyChangeEvent that resulted in an exception
   * @param cause the Throwable raised by the invoked method
   */
  public MethodInvocationException(PropertyChangeEvent propertyChangeEvent, Throwable cause) {
    super(propertyChangeEvent, "Property '%s' threw exception".formatted(propertyChangeEvent.getPropertyName()), cause);
  }

  @Override
  public String getErrorCode() {
    return ERROR_CODE;
  }

}
