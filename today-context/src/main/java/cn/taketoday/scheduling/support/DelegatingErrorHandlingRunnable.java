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

package cn.taketoday.scheduling.support;

import java.lang.reflect.UndeclaredThrowableException;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ErrorHandler;

/**
 * Runnable wrapper that catches any exception or error thrown from its
 * delegate Runnable and allows an {@link ErrorHandler} to handle it.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 4.0
 */
public class DelegatingErrorHandlingRunnable implements Runnable {

  private final Runnable delegate;
  private final ErrorHandler errorHandler;

  /**
   * Create a new DelegatingErrorHandlingRunnable.
   *
   * @param delegate the Runnable implementation to delegate to
   * @param errorHandler the ErrorHandler for handling any exceptions
   */
  public DelegatingErrorHandlingRunnable(Runnable delegate, ErrorHandler errorHandler) {
    Assert.notNull(delegate, "Delegate must not be null");
    Assert.notNull(errorHandler, "ErrorHandler must not be null");
    this.delegate = delegate;
    this.errorHandler = errorHandler;
  }

  @Override
  public void run() {
    try {
      this.delegate.run();
    }
    catch (UndeclaredThrowableException ex) {
      this.errorHandler.handleError(ex.getUndeclaredThrowable());
    }
    catch (Throwable ex) {
      this.errorHandler.handleError(ex);
    }
  }

  @Override
  public String toString() {
    return "DelegatingErrorHandlingRunnable for " + this.delegate;
  }

}
