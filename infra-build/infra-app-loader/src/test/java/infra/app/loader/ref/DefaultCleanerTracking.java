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


package infra.app.loader.ref;

import java.lang.ref.Cleaner.Cleanable;
import java.util.function.BiConsumer;

/**
 * Utility that allows tests to set a tracker on {@link DefaultCleaner}.
 *
 * @author Phillip Webb
 */
public final class DefaultCleanerTracking {

  private DefaultCleanerTracking() {
  }

  public static void set(BiConsumer<Object, Cleanable> tracker) {
    DefaultCleaner.tracker = tracker;
  }

}
