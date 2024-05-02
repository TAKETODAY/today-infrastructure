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

package cn.taketoday.app.loader.net.protocol.jar;

/**
 * {@link ThreadLocal} state for {@link Handler} optimizations.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class Optimizations {

  private static final ThreadLocal<Boolean> status = new ThreadLocal<>();

  private Optimizations() {
  }

  static void enable(boolean readContents) {
    status.set(readContents);
  }

  static void disable() {
    status.remove();
  }

  static boolean isEnabled() {
    return status.get() != null;
  }

  static boolean isEnabled(boolean readContents) {
    return Boolean.valueOf(readContents).equals(status.get());
  }

}
