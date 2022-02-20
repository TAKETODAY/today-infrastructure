/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.server;

/**
 * Simple interface that represents a fully configured web server (for example Tomcat,
 * Jetty, Netty). Allows the server to be {@link #start() started} and {@link #stop()
 * stopped}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 4.0
 */
public interface WebServer {

  /**
   * Starts the web server. Calling this method on an already started server has no
   * effect.
   *
   * @throws WebServerException if the server cannot be started
   */
  void start() throws WebServerException;

  /**
   * Stops the web server. Calling this method on an already stopped server has no
   * effect.
   *
   * @throws WebServerException if the server cannot be stopped
   */
  void stop() throws WebServerException;

  /**
   * Return the port this server is listening on.
   *
   * @return the port (or -1 if none)
   */
  int getPort();

  /**
   * Initiates a graceful shutdown of the web server. Handling of new requests is
   * prevented and the given {@code callback} is invoked at the end of the attempt. The
   * attempt can be explicitly ended by invoking {@link #stop}. The default
   * implementation invokes the callback immediately with
   * {@link GracefulShutdownResult#IMMEDIATE}, i.e. no attempt is made at a graceful
   * shutdown.
   *
   * @param callback the callback to invoke when the graceful shutdown completes
   * @since 4.0
   */
  default void shutDownGracefully(GracefulShutdownCallback callback) {
    callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
  }

}
