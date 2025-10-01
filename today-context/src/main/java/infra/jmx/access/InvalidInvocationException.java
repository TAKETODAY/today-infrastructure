/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jmx.access;

import org.jspecify.annotations.Nullable;

import javax.management.JMRuntimeException;

/**
 * Thrown when trying to invoke an operation on a proxy that is not exposed
 * by the proxied MBean resource's management interface.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see MBeanClientInterceptor
 * @since 4.0
 */
@SuppressWarnings("serial")
public class InvalidInvocationException extends JMRuntimeException {

  /**
   * Create a new {@code InvalidInvocationException} with the supplied
   * error message.
   *
   * @param msg the detail message
   */
  public InvalidInvocationException(@Nullable String msg) {
    super(msg);
  }

}
