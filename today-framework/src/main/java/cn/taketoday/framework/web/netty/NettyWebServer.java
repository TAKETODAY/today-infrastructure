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

package cn.taketoday.framework.web.netty;

import java.net.InetSocketAddress;

import cn.taketoday.framework.web.server.GracefulShutdownCallback;
import cn.taketoday.framework.web.server.GracefulShutdownResult;
import cn.taketoday.framework.web.server.ServerProperties.Netty;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;

/**
 * Netty {@link WebServer}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-02 21:15
 */
public class NettyWebServer implements WebServer {

  private static final Logger log = LoggerFactory.getLogger(NettyWebServer.class);

  private final InetSocketAddress listenAddress;

  private final EventLoopGroup childGroup;

  private final EventLoopGroup parentGroup;

  private final Netty.Shutdown shutdownConfig;

  private final ServerBootstrap serverBootstrap;

  private volatile boolean shutdownComplete = false;

  NettyWebServer(EventLoopGroup parentGroup, EventLoopGroup childGroup,
          ServerBootstrap serverBootstrap, InetSocketAddress listenAddress, Netty.Shutdown shutdownConfig) {
    this.serverBootstrap = serverBootstrap;
    this.shutdownConfig = shutdownConfig;
    this.listenAddress = listenAddress;
    this.parentGroup = parentGroup;
    this.childGroup = childGroup;
  }

  @Override
  public void start() {
    serverBootstrap.bind(listenAddress)
            .syncUninterruptibly();
    log.info("Netty web server started on port: '{}'", getPort());
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
