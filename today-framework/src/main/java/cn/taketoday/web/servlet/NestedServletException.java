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

package cn.taketoday.web.servlet;

import java.io.Serial;

import jakarta.servlet.ServletException;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;

/**
 * Subclass of {@link ServletException} that properly handles a root cause in terms
 * of message and stacktrace, just like NestedChecked/RuntimeException does.
 *
 * <p>Note that the plain ServletException doesn't expose its root cause at all,
 * neither in the exception message nor in printed stack traces! While this might
 * be fixed in later Servlet API variants (which even differ per vendor for the
 * same API version), it is not reliably available on Servlet 2.4
 *
 * <p>The similarity between this class and the NestedChecked/RuntimeException
 * class is unavoidable, as this class needs to derive from ServletException.
 *
 * @author Juergen Hoeller
 * @see #getMessage
 * @see #printStackTrace
 * @see cn.taketoday.core.NestedCheckedException
 * @see cn.taketoday.core.NestedRuntimeException
 * @since 4.0
 */
@Deprecated
public class NestedServletException extends ServletException {

  @Serial
  private static final long serialVersionUID = -5292377985529381145L;

  static {
    // Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
    // issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
    ExceptionUtils.class.getName();
  }

  /**
   * Construct a {@code NestedServletException} with the specified detail message.
   *
   * @param msg the detail message
   */
  public NestedServletException(String msg) {
    super(msg);
  }

  /**
   * Construct a {@code NestedServletException} with the specified detail message
   * and nested exception.
   *
   * @param msg the detail message
   * @param cause the nested exception
   */
  public NestedServletException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
