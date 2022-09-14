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

package cn.taketoday.framework.web.embedded.jetty;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import cn.taketoday.framework.web.server.GracefulShutdownCallback;
import cn.taketoday.framework.web.server.GracefulShutdownResult;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;

/**
 * Handles Jetty graceful shutdown.
 *
 * @author Andy Wilkinson
 */
final class GracefulShutdown {

  private static final Logger logger = LoggerFactory.getLogger(JettyWebServer.class);

  private final Server server;

  private final Supplier<Integer> activeRequests;

  private volatile boolean shuttingDown = false;

  GracefulShutdown(Server server, Supplier<Integer> activeRequests) {
    this.server = server;
    this.activeRequests = activeRequests;
  }

  void shutDownGracefully(GracefulShutdownCallback callback) {
    logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
    boolean jetty10 = isJetty10();
    for (Connector connector : this.server.getConnectors()) {
      shutdown(connector, !jetty10);
    }
    this.shuttingDown = true;
    new Thread(() -> awaitShutdown(callback), "jetty-shutdown").start();

  }

  @SuppressWarnings("unchecked")
  private void shutdown(Connector connector, boolean getResult) {
    Future<Void> result;
    try {
      result = connector.shutdown();
    }
    catch (NoSuchMethodError ex) {
      Method shutdown = ReflectionUtils.findMethod(connector.getClass(), "shutdown");
      result = (Future<Void>) ReflectionUtils.invokeMethod(shutdown, connector);
    }
    if (getResult) {
      try {
        result.get();
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      catch (ExecutionException ex) {
        // Continue
      }
    }
  }

  private boolean isJetty10() {
    try {
      return CompletableFuture.class.equals(Connector.class.getMethod("shutdown").getReturnType());
    }
    catch (Exception ex) {
      return false;
    }
  }

  private void awaitShutdown(GracefulShutdownCallback callback) {
    while (this.shuttingDown && this.activeRequests.get() > 0) {
      sleep(100);
    }
    this.shuttingDown = false;
    long activeRequests = this.activeRequests.get();
    if (activeRequests == 0) {
      logger.info("Graceful shutdown complete");
      callback.shutdownComplete(GracefulShutdownResult.IDLE);
    }
    else {
      logger.info(LogMessage.format("Graceful shutdown aborted with {} request(s) still active", activeRequests));
      callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
    }
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  void abort() {
    this.shuttingDown = false;
  }

}
