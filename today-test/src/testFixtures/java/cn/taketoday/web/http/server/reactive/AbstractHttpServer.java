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

package cn.taketoday.web.http.server.reactive;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.http.server.reactive.ContextPathCompositeHandler;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StopWatch;

/**
 * @author Rossen Stoyanchev
 */
public abstract class AbstractHttpServer implements HttpServer {

  protected Logger logger = LoggerFactory.getLogger(getClass().getName());

  private String host = "0.0.0.0";

  private int port = 0;

  private HttpHandler httpHandler;

  private Map<String, HttpHandler> handlerMap;

  private volatile boolean running;

  private final Object lifecycleMonitor = new Object();

  @Override
  public void setHost(String host) {
    this.host = host;
  }

  public String getHost() {
    return host;
  }

  @Override
  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public void setHandler(HttpHandler handler) {
    this.httpHandler = handler;
  }

  public HttpHandler getHttpHandler() {
    return this.httpHandler;
  }

  public void registerHttpHandler(String contextPath, HttpHandler handler) {
    if (this.handlerMap == null) {
      this.handlerMap = new LinkedHashMap<>();
    }
    this.handlerMap.put(contextPath, handler);
  }

  public Map<String, HttpHandler> getHttpHandlerMap() {
    return this.handlerMap;
  }

  protected HttpHandler resolveHttpHandler() {
    return (getHttpHandlerMap() != null ?
            new ContextPathCompositeHandler(getHttpHandlerMap()) : getHttpHandler());
  }

  // InitializingBean

  @Override
  public final void afterPropertiesSet() throws Exception {
    Assert.notNull(this.host, "Host is required");
    Assert.isTrue(this.port >= 0, "Port must not be a negative number");
    Assert.isTrue(this.httpHandler != null || this.handlerMap != null, "No HttpHandler configured");
    Assert.state(!this.running, "Cannot reconfigure while running");

    synchronized(this.lifecycleMonitor) {
      initServer();
    }
  }

  protected abstract void initServer() throws Exception;

  // Lifecycle

  @Override
  public final void start() {
    synchronized(this.lifecycleMonitor) {
      if (!isRunning()) {
        String serverName = getClass().getSimpleName();
        if (logger.isDebugEnabled()) {
          logger.debug("Starting " + serverName + "...");
        }
        this.running = true;
        try {
          StopWatch stopWatch = new StopWatch();
          stopWatch.start();
          startInternal();
          long millis = stopWatch.getTotalTimeMillis();
          if (logger.isDebugEnabled()) {
            logger.debug("Server started on port " + getPort() + "(" + millis + " millis).");
          }
        }
        catch (Throwable ex) {
          throw new IllegalStateException(ex);
        }
      }
    }

  }

  protected abstract void startInternal() throws Exception;

  @Override
  public final void stop() {
    synchronized(this.lifecycleMonitor) {
      if (isRunning()) {
        String serverName = getClass().getSimpleName();
        logger.debug("Stopping " + serverName + "...");
        this.running = false;
        try {
          StopWatch stopWatch = new StopWatch();
          stopWatch.start();
          stopInternal();
          logger.debug("Server stopped (" + stopWatch.getTotalTimeMillis() + " millis).");
        }
        catch (Throwable ex) {
          throw new IllegalStateException(ex);
        }
        finally {
          reset();
        }
      }
    }
  }

  protected abstract void stopInternal() throws Exception;

  @Override
  public boolean isRunning() {
    return this.running;
  }

  private void reset() {
    this.host = "0.0.0.0";
    this.port = 0;
    this.httpHandler = null;
    this.handlerMap = null;
    resetInternal();
  }

  protected abstract void resetInternal();

}
