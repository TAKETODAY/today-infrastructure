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

package infra.web.server.netty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.function.IntSupplier;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.server.GracefulShutdownCallback;
import infra.web.server.GracefulShutdownResult;
import infra.web.server.PortInUseException;
import infra.web.server.WebServer;
import infra.web.server.WebServerException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;

/**
 * Netty implementation of {@link WebServer}.
 * <p>
 * This class provides a Netty-based web server that can be started and stopped,
 * and supports graceful shutdown operations. It manages the lifecycle of the
 * underlying Netty server including binding to a port, handling SSL configuration,
 * and managing event loop groups.
 * <p>
 * The server can be configured with various parameters including the parent and
 * child event loop groups, server bootstrap configuration, listening address,
 * shutdown configuration, and SSL settings.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-02 21:15
 */
final class NettyWebServer implements WebServer, IntSupplier {

  private static final Logger log = LoggerFactory.getLogger(NettyWebServer.class);

  private final EventLoopGroup childGroup;

  private final boolean sslEnabled;

  private final boolean http2Enabled;

  private final EventLoopGroup parentGroup;

  private final NettyServerProperties.Shutdown shutdownConfig;

  private final ServerBootstrap serverBootstrap;

  private SocketAddress bindAddress;

  private volatile boolean shutdownComplete = false;

  NettyWebServer(EventLoopGroup parentGroup, EventLoopGroup childGroup, ServerBootstrap serverBootstrap,
          SocketAddress bindAddress, NettyServerProperties.Shutdown shutdownConfig, boolean sslEnabled, boolean http2Enabled) {
    this.serverBootstrap = serverBootstrap;
    this.shutdownConfig = shutdownConfig;
    this.bindAddress = bindAddress;
    this.parentGroup = parentGroup;
    this.childGroup = childGroup;
    this.sslEnabled = sslEnabled;
    this.http2Enabled = http2Enabled;
  }

  @Override
  public void start() throws WebServerException {
    try {
      if (serverBootstrap.bind(bindAddress).syncUninterruptibly().channel().localAddress() instanceof InetSocketAddress la) {
        bindAddress = la;
      }
      String bindInfo = getBindInfo();
      String protocolInfo = sslEnabled ? "(https)" : "(http)";
      String http2Support = http2Enabled ? " with HTTP/2" : "";
      log.info("Netty started on {} {}{}", bindInfo, protocolInfo, http2Support);
    }
    catch (Exception ex) {
      PortInUseException.throwIfPortBindingException(ex, this);
      throw new WebServerException("Unable to start Netty", ex);
    }
  }

  private String getBindInfo() {
    int port = getPort();
    if (port > 0) {
      return "port: " + port;
    }
    return bindAddress.toString();
  }

  @Override
  public int getAsInt() {
    return getPort();
  }

  @Override
  public void stop() {
    if (!shutdownComplete) {
      log.info("Shutdown netty web server: [{}] on ", this, getBindInfo());
      shutdown();
    }
  }

  private void shutdown() {
    parentGroup.shutdownGracefully(shutdownConfig.quietPeriod, shutdownConfig.timeout, shutdownConfig.unit);
    childGroup.shutdownGracefully(shutdownConfig.quietPeriod, shutdownConfig.timeout, shutdownConfig.unit);
  }

  @Override
  public void shutdownGracefully(GracefulShutdownCallback callback) {
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
    if (bindAddress instanceof InetSocketAddress isa) {
      return isa.getPort();
    }
    return -1;
  }

}
