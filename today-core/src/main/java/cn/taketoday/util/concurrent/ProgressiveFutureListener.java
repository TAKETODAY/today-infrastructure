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

package cn.taketoday.util.concurrent;

/**
 * Listens to the result of a {@link ProgressiveFuture}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 20:48
 */
public interface ProgressiveFutureListener<F extends ProgressiveFuture<?>> extends FutureListener<F> {

  /**
   * Invoked when the operation has progressed.
   *
   * @param progress the progress of the operation so far (cumulative)
   * @param total the number that signifies the end of the
   * operation when {@code progress} reaches at it.
   * {@code -1} if the end of operation is unknown.
   */
  void operationProgressed(F future, long progress, long total) throws Exception;

}
