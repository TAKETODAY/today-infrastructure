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

package infra.web.server.support;

import java.net.InetSocketAddress;
import java.util.function.IntSupplier;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.server.GracefulShutdownCallback;
import infra.web.server.GracefulShutdownResult;
import infra.web.server.PortInUseException;
import infra.web.server.ServerProperties.Netty;
import infra.web.server.WebServer;
import infra.web.server.WebServerException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;

/**
 * Netty {@link WebServer}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-02 21:15
 */
final class NettyWebServer implements WebServer, IntSupplier {

  private static final Logger log = LoggerFactory.getLogger(NettyWebServer.class);

  private final EventLoopGroup childGroup;

  private final boolean sslEnabled;

  private final EventLoopGroup parentGroup;

  private final Netty.Shutdown shutdownConfig;

  private final ServerBootstrap serverBootstrap;

  private volatile boolean shutdownComplete = false;

  private InetSocketAddress listenAddress;

  NettyWebServer(EventLoopGroup parentGroup, EventLoopGroup childGroup, ServerBootstrap serverBootstrap,
          InetSocketAddress listenAddress, Netty.Shutdown shutdownConfig, boolean sslEnabled) {
    this.serverBootstrap = serverBootstrap;
    this.shutdownConfig = shutdownConfig;
    this.listenAddress = listenAddress;
    this.parentGroup = parentGroup;
    this.childGroup = childGroup;
    this.sslEnabled = sslEnabled;
  }

  @Override
  public void start() throws WebServerException {
    try {
      if (serverBootstrap.bind(listenAddress).syncUninterruptibly().channel().localAddress() instanceof InetSocketAddress localAddress) {
        listenAddress = localAddress;
      }
      log.info("Netty started on port: {} {}", getPort(), sslEnabled ? "(https)" : "(http)");
    }
    catch (Exception ex) {
      PortInUseException.throwIfPortBindingException(ex, this);
      throw new WebServerException("Unable to start Netty", ex);
    }
  }

  @Override
  public int getAsInt() {
    return getPort();
  }

  @Override
  public void stop() {
    log.info("Shutdown netty web server: [{}] on port: '{}'", this, getPort());
    if (!shutdownComplete) {
      shutdown();
    }
  }

  private void shutdown() {
    parentGroup.shutdownGracefully(shutdownConfig.quietPeriod, shutdownConfig.timeout, shutdownConfig.unit);
    childGroup.shutdownGracefully(shutdownConfig.quietPeriod, shutdownConfig.timeout, shutdownConfig.unit);
  }

  @Override
  public void shutDownGracefully(GracefulShutdownCallback callback) {
    log.info("Commencing graceful shutdown. Waiting for active requests to complete");
    try {
      shutdown();
      childGroup.terminationFuture().sync();
      parentGroup.terminationFuture().sync();
      shutdownComplete = true;
      callback.shutdownComplete(GracefulShutdownResult.IDLE);
      log.info("Graceful shutdown complete");
    }
    catch (Exception ex) {
      log.info("Graceful shutdown aborted with one or more requests still active");
      callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
    }
  }

  @Override
  public int getPort() {
    return listenAddress.getPort();
  }

}
