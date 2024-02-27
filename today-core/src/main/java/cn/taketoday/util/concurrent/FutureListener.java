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
 * Listens to the result of a {@link ListenableFuture}.
 * The result of the asynchronous operation is notified once this listener
 * is added by calling {@link ListenableFuture#addListener(FutureListener)}.
 *
 * @param <F> the future type
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface FutureListener<F extends ListenableFuture<?>> extends EventListener {

  /**
   * Invoked when the operation associated with
   * the {@link ListenableFuture} has been completed.
   *
   * @param future the source {@link ListenableFuture} which called this callback
   */
  void operationComplete(F future) throws Exception;

}
