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
import java.util.function.BiConsumer;

/**
 * Default {@link Cleaner} implementation that delegates to {@link java.lang.ref.Cleaner}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class DefaultCleaner implements Cleaner {

  static final DefaultCleaner instance = new DefaultCleaner();

  static BiConsumer<Object, Cleanable> tracker;

  private final java.lang.ref.Cleaner cleaner = java.lang.ref.Cleaner.create();

  @Override
  public Cleanable register(Object obj, Runnable action) {
    Cleanable cleanable = (action != null) ? this.cleaner.register(obj, action) : null;
    if (tracker != null) {
      tracker.accept(obj, cleanable);
    }
    return cleanable;
  }

}
