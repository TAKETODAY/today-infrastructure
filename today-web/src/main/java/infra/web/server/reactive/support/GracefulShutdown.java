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

package infra.web.server.reactive.support;

import java.time.Duration;
import java.util.function.Supplier;

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.server.GracefulShutdownCallback;
import infra.web.server.GracefulShutdownResult;
import reactor.netty.DisposableServer;

/**
 * Handles Netty graceful shutdown.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class GracefulShutdown {

  private static final Logger logger = LoggerFactory.getLogger(GracefulShutdown.class);

  private final Supplier<DisposableServer> disposableServer;

  @Nullable
  private volatile Thread shutdownThread;

  private volatile boolean shuttingDown;

  GracefulShutdown(Supplier<DisposableServer> disposableServer) {
    this.disposableServer = disposableServer;
  }

  void shutDownGracefully(GracefulShutdownCallback callback) {
    DisposableServer server = disposableServer.get();
    if (server == null) {
      return;
    }
    logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
    Thread thread = new Thread(() -> doShutdown(callback, server), "netty-shutdown");
    this.shutdownThread = thread;
    thread.start();
  }

  private void doShutdown(GracefulShutdownCallback callback, DisposableServer server) {
    this.shuttingDown = true;
    try {
      server.disposeNow(Duration.ofNanos(Long.MAX_VALUE));
      logger.info("Graceful shutdown complete");
      callback.shutdownComplete(GracefulShutdownResult.IDLE);
    }
    catch (Exception ex) {
      logger.info("Graceful shutdown aborted with one or more requests still active");
      callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
    }
    finally {
      this.shutdownThread = null;
      this.shuttingDown = false;
    }
  }

  void abort() {
    Thread shutdownThread = this.shutdownThread;
    if (shutdownThread != null) {
      while (!this.shuttingDown) {
        sleep(50);
      }
      shutdownThread.interrupt();
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

}
