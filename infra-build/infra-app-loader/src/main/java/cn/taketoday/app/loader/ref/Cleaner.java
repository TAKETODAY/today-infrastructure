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

package cn.taketoday.app.loader.ref;

import java.lang.ref.Cleaner.Cleanable;

/**
 * Wrapper for {@link java.lang.ref.Cleaner} providing registration support.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public interface Cleaner {

  /**
   * Provides access to the default clean instance which delegates to
   * {@link java.lang.ref.Cleaner}.
   */
  Cleaner instance = DefaultCleaner.instance;

  /**
   * Registers an object and the clean action to run when the object becomes phantom
   * reachable.
   *
   * @param obj the object to monitor
   * @param action the cleanup action to run
   * @return a {@link Cleanable} instance
   * @see java.lang.ref.Cleaner#register(Object, Runnable)
   */
  Cleanable register(Object obj, Runnable action);

}
