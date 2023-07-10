/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools;

import sun.misc.Signal;

/**
 * Utilities for working with signal handling.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class SignalUtils {

  private static final Signal SIG_INT = new Signal("INT");

  /**
   * Handle {@literal INT} signals by calling the specified {@link Runnable}.
   *
   * @param runnable the runnable to call on SIGINT.
   */
  public static void attachSignalHandler(Runnable runnable) {
    Signal.handle(SIG_INT, (signal) -> runnable.run());
  }

}
