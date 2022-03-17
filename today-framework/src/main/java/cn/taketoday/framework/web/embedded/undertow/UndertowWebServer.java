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

package cn.taketoday.framework.web.embedded.undertow;

import org.xnio.channels.BoundChannel;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.framework.web.server.GracefulShutdownCallback;
import cn.taketoday.framework.web.server.GracefulShutdownResult;
import cn.taketoday.framework.web.server.PortInUseException;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.server.WebServerException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.GracefulShutdownHandler;

/**
 * {@link WebServer} that can be used to control an Undertow web server. Usually this
 * class should be created using the {@link UndertowReactiveWebServerFactory} and not
 * directly.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Christoph Dreis
 * @author Brian Clozel
 * @since 4.0
 */
public class UndertowWebServer implements WebServer {

  private static final Logger logger = LoggerFactory.getLogger(UndertowWebServer.class);

  private final AtomicReference<GracefulShutdownCallback> gracefulShutdownCallback = new AtomicReference<>();

  private final Object monitor = new Object();

  private final Undertow.Builder builder;

  private final Iterable<HttpHandlerFactory> httpHandlerFactories;

  private final boolean autoStart;

  @Nullable
  private Undertow undertow;

  private volatile boolean started = false;

  @Nullable
  private volatile GracefulShutdownHandler gracefulShutdown;

  @Nullable
  private volatile List<Closeable> closeables;

  /**
   * Create a new {@link UndertowWebServer} instance.
   *
   * @param builder the builder
   * @param autoStart if the server should be started
   */
  public UndertowWebServer(Undertow.Builder builder, boolean autoStart) {
    this(builder, Collections.singleton(new CloseableHttpHandlerFactory(null)), autoStart);
  }

  /**
   * Create a new {@link UndertowWebServer} instance.
   *
   * @param builder the builder
   * @param httpHandlerFactories the handler factories
   * @param autoStart if the server should be started
   */
  public UndertowWebServer(Undertow.Builder builder, Iterable<HttpHandlerFactory> httpHandlerFactories,
                           boolean autoStart) {
    this.builder = builder;
    this.httpHandlerFactories = httpHandlerFactories;
    this.autoStart = autoStart;
  }

  @Override
  public void start() throws WebServerException {
    synchronized(this.monitor) {
      if (this.started) {
        return;
      }
      try {
        if (!this.autoStart) {
          return;
        }
        if (this.undertow == null) {
          this.undertow = createUndertowServer();
        }
        this.undertow.start();
        this.started = true;
        String message = getStartLogMessage();
        logger.info(message);
      }
      catch (Exception ex) {
        try {
          PortInUseException.ifPortBindingException(ex, (bindException) -> {
            List<Port> failedPorts = getConfiguredPorts();
            failedPorts.removeAll(getActualPorts());
            if (failedPorts.size() == 1) {
              throw new PortInUseException(failedPorts.get(0).getNumber());
            }
          });
          throw new WebServerException("Unable to start embedded Undertow", ex);
        }
        finally {
          stopSilently();
        }
      }
    }
  }

  private void stopSilently() {
    try {
      if (this.undertow != null) {
        this.undertow.stop();
        this.closeables.forEach(this::closeSilently);
      }
    }
    catch (Exception ex) {
      // Ignore
    }
  }

  private void closeSilently(Closeable closeable) {
    try {
      closeable.close();
    }
    catch (Exception ignored) {
    }
  }

  private Undertow createUndertowServer() {
    this.closeables = new ArrayList<>();
    this.gracefulShutdown = null;
    HttpHandler handler = createHttpHandler();
    this.builder.setHandler(handler);
    return this.builder.build();
  }

  protected HttpHandler createHttpHandler() {
    HttpHandler handler = null;
    for (HttpHandlerFactory factory : this.httpHandlerFactories) {
      handler = factory.getHandler(handler);
      if (handler instanceof Closeable) {
        this.closeables.add((Closeable) handler);
      }
      if (handler instanceof GracefulShutdownHandler) {
        Assert.isNull(this.gracefulShutdown, "Only a single GracefulShutdownHandler can be defined");
        this.gracefulShutdown = (GracefulShutdownHandler) handler;
      }
    }
    return handler;
  }

  private String getPortsDescription() {
    List<Port> ports = getActualPorts();
    if (!ports.isEmpty()) {
      return StringUtils.collectionToDelimitedString(ports, " ");
    }
    return "unknown";
  }

  private List<Port> getActualPorts() {
    List<Port> ports = new ArrayList<>();
    try {
      if (!this.autoStart) {
        ports.add(new Port(-1, "unknown"));
      }
      else {
        for (BoundChannel channel : extractChannels()) {
          ports.add(getPortFromChannel(channel));
        }
      }
    }
    catch (Exception ex) {
      // Continue
    }
    return ports;
  }

  @SuppressWarnings("unchecked")
  private List<BoundChannel> extractChannels() {
    Field channelsField = ReflectionUtils.findField(Undertow.class, "channels");
    ReflectionUtils.makeAccessible(channelsField);
    return (List<BoundChannel>) ReflectionUtils.getField(channelsField, this.undertow);
  }

  @Nullable
  private Port getPortFromChannel(BoundChannel channel) {
    SocketAddress socketAddress = channel.getLocalAddress();
    if (socketAddress instanceof InetSocketAddress) {
      Field sslField = ReflectionUtils.findField(channel.getClass(), "ssl");
      String protocol = (sslField != null) ? "https" : "http";
      return new Port(((InetSocketAddress) socketAddress).getPort(), protocol);
    }
    return null;
  }

  private List<Port> getConfiguredPorts() {
    List<Port> ports = new ArrayList<>();
    for (Object listener : extractListeners()) {
      try {
        Port port = getPortFromListener(listener);
        if (port.getNumber() != 0) {
          ports.add(port);
        }
      }
      catch (Exception ex) {
        // Continue
      }
    }
    return ports;
  }

  @SuppressWarnings("unchecked")
  private List<Object> extractListeners() {
    Field listenersField = ReflectionUtils.findField(Undertow.class, "listeners");
    ReflectionUtils.makeAccessible(listenersField);
    return (List<Object>) ReflectionUtils.getField(listenersField, this.undertow);
  }

  private Port getPortFromListener(Object listener) {
    Field typeField = ReflectionUtils.findField(listener.getClass(), "type");
    ReflectionUtils.makeAccessible(typeField);
    String protocol = ReflectionUtils.getField(typeField, listener).toString();
    Field portField = ReflectionUtils.findField(listener.getClass(), "port");
    ReflectionUtils.makeAccessible(portField);
    int port = (Integer) ReflectionUtils.getField(portField, listener);
    return new Port(port, protocol);
  }

  @Override
  public void stop() throws WebServerException {
    synchronized(this.monitor) {
      if (!this.started) {
        return;
      }
      this.started = false;
      if (this.gracefulShutdown != null) {
        notifyGracefulCallback(false);
      }
      try {
        this.undertow.stop();
        for (Closeable closeable : this.closeables) {
          closeable.close();
        }
      }
      catch (Exception ex) {
        throw new WebServerException("Unable to stop undertow", ex);
      }
    }
  }

  @Override
  public int getPort() {
    List<Port> ports = getActualPorts();
    if (ports.isEmpty()) {
      return -1;
    }
    return ports.get(0).getNumber();
  }

  @Override
  public void shutDownGracefully(GracefulShutdownCallback callback) {
    GracefulShutdownHandler gracefulShutdown = this.gracefulShutdown;
    if (gracefulShutdown == null) {
      callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
      return;
    }
    logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
    gracefulShutdownCallback.set(callback);
    gracefulShutdown.shutdown();
    gracefulShutdown.addShutdownListener(this::notifyGracefulCallback);
  }

  private void notifyGracefulCallback(boolean success) {
    GracefulShutdownCallback callback = this.gracefulShutdownCallback.getAndSet(null);
    if (callback != null) {
      if (success) {
        logger.info("Graceful shutdown complete");
        callback.shutdownComplete(GracefulShutdownResult.IDLE);
      }
      else {
        logger.info("Graceful shutdown aborted with one or more requests still active");
        callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
      }
    }
  }

  protected String getStartLogMessage() {
    return "Undertow started on port(s) " + getPortsDescription();
  }

  /**
   * An active Undertow port.
   */
  private record Port(int number, String protocol) {

    int getNumber() {
      return this.number;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Port other = (Port) obj;
      return this.number == other.number;
    }

    @Override
    public int hashCode() {
      return this.number;
    }

    @Override
    public String toString() {
      return this.number + " (" + this.protocol + ")";
    }

  }

  /**
   * {@link HttpHandlerFactory} to wrap a closable.
   */
  private record CloseableHttpHandlerFactory(@Nullable Closeable closeable)
          implements HttpHandlerFactory {

    private CloseableHttpHandlerFactory(@Nullable Closeable closeable) {
      this.closeable = closeable;
    }

    @Override
    public HttpHandler getHandler(HttpHandler next) {
      if (this.closeable == null) {
        return next;
      }
      return new CloseableHttpHandler() {

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
          next.handleRequest(exchange);
        }

        @Override
        public void close() throws IOException {
          CloseableHttpHandlerFactory.this.closeable.close();
        }

      };
    }

  }

  /**
   * {@link Closeable} {@link HttpHandler}.
   */
  private interface CloseableHttpHandler extends HttpHandler, Closeable {

  }

}
