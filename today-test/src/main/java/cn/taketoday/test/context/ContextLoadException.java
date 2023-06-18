/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context;

import java.io.Serial;

import cn.taketoday.context.ApplicationContext;

/**
 * Exception thrown when an error occurs while a {@link SmartContextLoader}
 * attempts to load an {@link ApplicationContext}.
 *
 * <p>This exception provides access to the {@linkplain #getApplicationContext()
 * application context} that failed to load as well as the {@linkplain #getCause()
 * exception} caught while attempting to load that context.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SmartContextLoader#loadContext(MergedContextConfiguration)
 * @since 4.0 2023/6/15 21:12
 */
public class ContextLoadException extends Exception {

  @Serial
  private static final long serialVersionUID = 1L;

  private final ApplicationContext applicationContext;

  /**
   * Create a new {@code ContextLoadException} for the supplied
   * {@link ApplicationContext} and {@link Throwable}.
   *
   * @param applicationContext the application context that failed to load
   * @param cause the exception caught while attempting to load that context
   */
  public ContextLoadException(ApplicationContext applicationContext, Throwable cause) {
    super(cause);
    this.applicationContext = applicationContext;
  }

  /**
   * Get the {@code ApplicationContext} that failed to load.
   * <p>Clients must not retain a long-lived reference to the context returned
   * from this method.
   */
  public ApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

}
