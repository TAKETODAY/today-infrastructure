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

package infra.app.loader.tools;

/**
 * Utilities for working with signal handling.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class SignalUtils {

  private static final sun.misc.Signal SIG_INT = new sun.misc.Signal("INT");

  /**
   * Handle {@literal INT} signals by calling the specified {@link Runnable}.
   *
   * @param runnable the runnable to call on SIGINT.
   */
  public static void attachSignalHandler(Runnable runnable) {
    sun.misc.Signal.handle(SIG_INT, (signal) -> runnable.run());
  }

}
