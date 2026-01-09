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

package infra.scheduling.support;

import java.lang.reflect.UndeclaredThrowableException;

import infra.lang.Assert;
import infra.util.ErrorHandler;

/**
 * Runnable wrapper that catches any exception or error thrown from its
 * delegate Runnable and allows an {@link ErrorHandler} to handle it.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
    Assert.notNull(delegate, "Delegate is required");
    Assert.notNull(errorHandler, "ErrorHandler is required");
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
