/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.http.server;

/**
 * A control that can put the processing of an HTTP request in asynchronous mode during
 * which the response remains open until explicitly closed.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ServerHttpAsyncRequestControl {

  /**
   * Enable asynchronous processing after which the response remains open until a call
   * to {@link #complete()} is made or the server times out the request. Once enabled,
   * additional calls to this method are ignored.
   */
  void start();

  /**
   * A variation on {@link #start()} that allows specifying a timeout value to use to
   * use for asynchronous processing. If {@link #complete()} is not called within the
   * specified value, the request times out.
   */
  void start(long timeout);

  /**
   * Return whether asynchronous request processing has been started.
   */
  boolean isStarted();

  /**
   * Mark asynchronous request processing as completed.
   */
  void complete();

  /**
   * Return whether asynchronous request processing has been completed.
   */
  boolean isCompleted();

}
