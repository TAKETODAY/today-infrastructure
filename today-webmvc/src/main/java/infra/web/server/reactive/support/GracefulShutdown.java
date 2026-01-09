/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.server.reactive.support;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.function.Supplier;

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
