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

import java.util.EventListener;

/**
 * Listens to the result of a {@link Future}.
 * The result of the asynchronous operation is notified once this listener
 * is added by calling {@link Future#addListener(FutureContextListener, Object)}.
 * <pre>
 * Future f = Future.forSettable(..);
 * f.addListener((future, context) -> { .. }, context);
 * </pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FutureListener
 * @since 4.0 2024/3/21 16:36
 */
@FunctionalInterface
public interface FutureContextListener<F extends Future<?>, C> extends EventListener {

  /**
   * Invoked when the operation associated with the {@link Future} has been completed.
   *
   * @param completed the source {@link Future} which called this callback
   */
  void operationComplete(F completed, C context) throws Throwable;

}
