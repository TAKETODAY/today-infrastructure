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
package cn.taketoday.framework.web.netty;

import java.net.InetSocketAddress;

import cn.taketoday.framework.web.server.GracefulShutdownCallback;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.server.WebServerException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;

/**
 * Netty {@link WebServer}
 *
 * @author TODAY 2019-07-02 21:15
 */
public class NettyWebServer implements WebServer {

  private static final Logger log = LoggerFactory.getLogger(NettyWebServer.class);

  private final InetSocketAddress listenAddress;

  private final EventLoopGroup parentGroup;
  private final EventLoopGroup childGroup;
  private final ServerBootstrap serverBootstrap;

  @Nullable
  private ChannelFuture channelFuture;

  public NettyWebServer(ServerBootstrap serverBootstrap, InetSocketAddress listenAddress,
          EventLoopGroup parentGroup, EventLoopGroup childGroup) {
    this.serverBootstrap = serverBootstrap;
    this.listenAddress = listenAddress;
    this.parentGroup = parentGroup;
    this.childGroup = childGroup;
  }

  @Override
  public void start() {
    channelFuture = serverBootstrap.bind(listenAddress.getAddress(), getPort());
    try {
      channelFuture.sync();
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted", e);
      throw new WebServerException("Interrupted", e);
    }
  }

  @Override
  public void stop() {
    log.info("Shutdown netty web server: [{}]", this);

    shutdown();
  }

  private void shutdown() {

    parentGroup.shutdownGracefully();
    childGroup.shutdownGracefully();

    if (channelFuture != null) {
      channelFuture.channel().close();
    }

  }

  @Override
  public void shutDownGracefully(GracefulShutdownCallback callback) {
    log.info("Shutdown netty web server: [{}] gracefully", this);

    shutdown();
  }

  @Override
  public int getPort() {
    return listenAddress.getPort();
  }

}