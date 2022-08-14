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

package cn.taketoday.framework.web.embedded.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Tomcat;

import java.util.ArrayList;
import java.util.Collections;

import cn.taketoday.framework.web.server.GracefulShutdownCallback;
import cn.taketoday.framework.web.server.GracefulShutdownResult;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ExceptionUtils;

/**
 * Handles Tomcat graceful shutdown.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
final class GracefulShutdown {
  private static final Logger log = LoggerFactory.getLogger(GracefulShutdown.class);

  private final Tomcat tomcat;

  private volatile boolean aborted = false;

  GracefulShutdown(Tomcat tomcat) {
    this.tomcat = tomcat;
  }

  void shutDownGracefully(GracefulShutdownCallback callback) {
    log.info("Commencing graceful shutdown. Waiting for active requests to complete");
    new Thread(() -> doShutdown(callback), "tomcat-shutdown").start();
  }

  private void doShutdown(GracefulShutdownCallback callback) {
    getConnectors().forEach(this::close);
    try {
      for (Container host : tomcat.getEngine().findChildren()) {
        for (Container context : host.findChildren()) {
          while (isActive(context)) {
            if (this.aborted) {
              log.info("Graceful shutdown aborted with one or more requests still active");
              callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
              return;
            }
            Thread.sleep(50);
          }
        }
      }

    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    log.info("Graceful shutdown complete");
    callback.shutdownComplete(GracefulShutdownResult.IDLE);
  }

  private ArrayList<Connector> getConnectors() {
    ArrayList<Connector> connectors = new ArrayList<>();
    for (Service service : this.tomcat.getServer().findServices()) {
      Collections.addAll(connectors, service.findConnectors());
    }
    return connectors;
  }

  private void close(Connector connector) {
    connector.pause();
    connector.getProtocolHandler().closeServerSocketGraceful();
  }

  private boolean isActive(Container context) {
    try {
      if (((StandardContext) context).getInProgressAsyncCount() > 0) {
        return true;
      }
      for (Container wrapper : context.findChildren()) {
        if (((StandardWrapper) wrapper).getCountAllocated() > 0) {
          return true;
        }
      }
      return false;
    }
    catch (Exception ex) {
      throw ExceptionUtils.sneakyThrow(ex);
    }
  }

  void abort() {
    this.aborted = true;
  }

}
