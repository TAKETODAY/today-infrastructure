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
 * <p>
 * This class represents a call-back mechanism that will notify implementations as HTTP request data becomes available
 * to be read without blocking.
 * </p>
 *
 * @since Servlet 3.1
 */
public interface ReadListener extends EventListener {

  /**
   * When an instance of the <code>ReadListener</code> is registered with a {@link ServletInputStream}, this method will
   * be invoked by the container the first time when it is possible to read data. Subsequently the container will invoke
   * this method if and only if the {@link ServletInputStream#isReady()} method has been called and has
   * returned a value of <code>false</code> <em>and</em> data has subsequently become available to read.
   *
   * @throws IOException if an I/O related error has occurred during processing
   */
  public void onDataAvailable() throws IOException;

  /**
   * Invoked when all data for the current request has been read.
   *
   * @throws IOException if an I/O related error has occurred during processing
   */
  public void onAllDataRead() throws IOException;

  /**
   * Invoked when an error occurs processing the request.
   *
   * @param t the throwable to indicate why the read operation failed
   */
  public void onError(Throwable t);

}
