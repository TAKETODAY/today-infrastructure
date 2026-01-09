/*
 * Copyright 2017 - 2026 the TODAY authors.
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
