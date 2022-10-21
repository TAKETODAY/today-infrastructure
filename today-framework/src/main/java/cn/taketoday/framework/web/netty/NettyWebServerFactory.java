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
import java.util.Locale;

import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/20 13:44
 */
public class NettyWebServerFactory
        extends AbstractReactiveWebServerFactory implements ReactiveWebServerFactory {

  private static final Logger log = LoggerFactory.getLogger(NettyWebServer.class);

  static boolean epollPresent = ClassUtils.isPresent(
          "io.netty.channel.epoll.EpollServerSocketChannel", NettyWebServer.class.getClassLoader());

  static boolean kQueuePresent = ClassUtils.isPresent(
          "io.netty.channel.kqueue.KQueueServerSocketChannel", NettyWebServer.class.getClassLoader());

  /**
   * the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  private int childThreadCount = 4;

  /**
   * the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For parent {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  private int parentThreadCount = 2;

  @Nullable
  private EventLoopGroup childGroup;

  @Nullable
  private EventLoopGroup parentGroup;

  @Nullable
  private Class<? extends ServerSocketChannel> socketChannel;

  @Nullable
  private LogLevel loggingLevel;

  /**
   * Framework Channel Initializer
   */
  @Nullable
  private NettyChannelInitializer nettyChannelInitializer;

  @Nullable
  public EventLoopGroup getChildGroup() {
    return childGroup;
  }

  @Nullable
  public EventLoopGroup getParentGroup() {
    return parentGroup;
  }

  @Nullable
  public Class<? extends ServerSocketChannel> getSocketChannel() {
    return socketChannel;
  }

  public void setParentGroup(EventLoopGroup parentGroup) {
    this.parentGroup = parentGroup;
  }

  public void setChildGroup(EventLoopGroup childGroup) {
    this.childGroup = childGroup;
  }

  public void setSocketChannel(Class<? extends ServerSocketChannel> socketChannel) {
    this.socketChannel = socketChannel;
  }

  /**
   * set the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For parent {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public void setParentThreadCount(int parentThreadCount) {
    this.parentThreadCount = parentThreadCount;
  }

  /**
   * get the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For parent {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public int getParentThreadCount() {
    return parentThreadCount;
  }

  /**
   * set the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public void setChildThreadCount(int childThreadCount) {
    this.childThreadCount = childThreadCount;
  }

  /**
   * get the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public int getChildThreadCount() {
    return childThreadCount;
  }

  public void setNettyChannelInitializer(NettyChannelInitializer nettyChannelInitializer) {
    this.nettyChannelInitializer = nettyChannelInitializer;
  }

  /**
   * Set {@link LoggingHandler} logging Level
   * <p>
   * If that {@code loggingLevel} is {@code null} will not register logging handler
   * </p>
   *
   * @param loggingLevel LogLevel
   * @see LogLevel
   * @see LoggingHandler
   * @see ServerBootstrap#handler
   */
  public void setLoggingLevel(LogLevel loggingLevel) {
    this.loggingLevel = loggingLevel;
  }

  /**
   * Get {@link LoggingHandler} logging Level
   *
   * @see LogLevel
   * @see LoggingHandler
   * @see ServerBootstrap#handler
   */
  @Nullable
  public LogLevel getLoggingLevel() {
    return loggingLevel;
  }

  /**
   * Subclasses can override this method to perform epoll is available logic
   */
  protected boolean epollIsAvailable() {
    return epollPresent && "Linux".equalsIgnoreCase(System.getProperty("os.name"));
  }

  /**
   * Subclasses can override this method to perform KQueue is available logic
   */
  protected boolean kQueueIsAvailable() {
    return kQueuePresent
            && System.getProperty("os.name").toUpperCase(Locale.ENGLISH).contains("BSD");
  }

  @Override
  public WebServer getWebServer(HttpHandler httpHandler) {
    ServerBootstrap bootstrap = new ServerBootstrap();
    preBootstrap(bootstrap);

    // enable epoll
    if (epollIsAvailable()) {
      EpollDelegate.init(bootstrap, this);
    }
    else if (kQueueIsAvailable()) {
      KQueueDelegate.init(this);
    }
    else {
      if (parentGroup == null) {
        parentGroup = new NioEventLoopGroup(parentThreadCount, new DefaultThreadFactory("parent@"));
      }
      if (childGroup == null) {
        childGroup = new NioEventLoopGroup(childThreadCount, new DefaultThreadFactory("child@"));
      }
      if (socketChannel == null) {
        socketChannel = NioServerSocketChannel.class;
      }
    }

    bootstrap.group(parentGroup, childGroup);
    bootstrap.channel(socketChannel);

    NettyChannelInitializer channelInitializer = getNettyChannelInitializer();
    Assert.state(channelInitializer != null, "No NettyChannelInitializer");

    bootstrap.childHandler(channelInitializer);
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

    postBootstrap(bootstrap);

    InetSocketAddress listenAddress = getListenAddress();
    return new NettyWebServer(bootstrap, listenAddress, parentGroup, childGroup);
  }

  private InetSocketAddress getListenAddress() {
    if (getAddress() != null) {
      return new InetSocketAddress(getAddress().getHostAddress(), getPort());
    }
    return new InetSocketAddress(getPort());
  }

  /**
   * before bootstrap
   *
   * @param bootstrap netty ServerBootstrap
   */
  protected void preBootstrap(ServerBootstrap bootstrap) {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
  }

  /**
   * after bootstrap
   *
   * @param bootstrap netty ServerBootstrap
   */
  protected void postBootstrap(ServerBootstrap bootstrap) {
    log.info("Netty web server started on port: '{}'", getPort());
    if (loggingLevel != null) {
      bootstrap.handler(new LoggingHandler(loggingLevel));
    }
  }

  @Nullable
  public NettyChannelInitializer getNettyChannelInitializer() {
    return nettyChannelInitializer;
  }

  static class EpollDelegate {
    static void init(ServerBootstrap bootstrap, NettyWebServerFactory factory) {
      bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
      if (factory.socketChannel == null) {
        factory.setSocketChannel(EpollServerSocketChannel.class);
      }
      if (factory.parentGroup == null) {
        factory.setParentGroup(new EpollEventLoopGroup(
                factory.getParentThreadCount(), new DefaultThreadFactory("epoll-parent@")));
      }
      if (factory.childGroup == null) {
        factory.setChildGroup(new EpollEventLoopGroup(
                factory.getChildThreadCount(), new DefaultThreadFactory("epoll-child@")));
      }
    }
  }

  static class KQueueDelegate {
    static void init(NettyWebServerFactory factory) {
      if (factory.socketChannel == null) {
        factory.setSocketChannel(KQueueServerSocketChannel.class);
      }
      if (factory.parentGroup == null) {
        factory.setParentGroup(new KQueueEventLoopGroup(
                factory.getParentThreadCount(), new DefaultThreadFactory("kQueue-parent@")));
      }
      if (factory.childGroup == null) {
        factory.setChildGroup(new KQueueEventLoopGroup(
                factory.getChildThreadCount(), new DefaultThreadFactory("kQueue-child@")));
      }
    }
  }

}
