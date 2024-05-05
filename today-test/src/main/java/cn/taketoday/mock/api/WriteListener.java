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

package cn.taketoday.mock.api;

import java.io.IOException;
import java.util.EventListener;

/**
 * Callback notification mechanism that signals to the developer it's possible to write content without blocking.
 */
public interface WriteListener extends EventListener {

  /**
   * When an instance of the WriteListener is registered with a {@link MockOutputStream}, this method will be invoked
   * by the container the first time when it is possible to write data. Subsequently the container will invoke this method
   * if and only if the {@link MockOutputStream#isReady()} method has been called and has returned a
   * value of <code>false</code> and a write operation has subsequently become possible.
   *
   * @throws IOException if an I/O related error has occurred during processing
   */
  public void onWritePossible() throws IOException;

  /**
   * Invoked when an error occurs writing data using the non-blocking APIs.
   *
   * @param t the throwable to indicate why the write operation failed
   */
  public void onError(final Throwable t);

}
