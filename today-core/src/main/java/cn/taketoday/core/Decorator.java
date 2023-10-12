/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core;

/**
 * A callback interface for a decorator to be applied to any {@code T}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/12 21:26
 */
public interface Decorator<T> {

  /**
   * Decorate the given {@code delegate}, returning a potentially wrapped
   * {@code delegate} for actual execution, internally delegating to the
   * original methods implementation.
   *
   * @param delegate the original {@code delegate}
   * @return the decorated object
   */
  T decorate(T delegate);

  /**
   * call it after this decoration
   */
  default Decorator<T> andThen(Decorator<T> decorator) {
    return delegate -> {
      delegate = decorate(delegate);
      return decorator.decorate(delegate);
    };
  }

}
