/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.framework.reactive;

import java.util.Objects;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.web.framework.StandardWebServerApplicationContext;
import cn.taketoday.web.framework.WebServerApplicationContext;
import cn.taketoday.web.framework.WebServerException;
import cn.taketoday.web.framework.server.AbstractWebServer;
import cn.taketoday.web.framework.server.WebServer;
import cn.taketoday.web.framework.server.WebServerApplicationLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
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
 * Netty {@link WebServer}
 *
 * @author TODAY 2019-07-02 21:15
 */
public class NettyWebServer extends AbstractWebServer implements WebServer, DisposableBean {
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

  private EventLoopGroup childGroup;
  private EventLoopGroup parentGroup;
  private Class<? extends ServerSocketChannel> socketChannel;

  private LogLevel loggingLevel;

  /**
   * Framework Channel Initializer
   */
  private NettyServerInitializer nettyServerInitializer;

  @Override
  protected void initApplicationContext(ApplicationContext context) {
    super.initApplicationContext(context);
    applyContextPath(context);
  }

  private void applyContextPath(ApplicationContext context) {
    if (context instanceof StandardWebServerApplicationContext) {
      ((StandardWebServerApplicationContext) context).setContextPath(getContextPath());
    }
  }

  /**
   * Subclasses can override this method to perform epoll is available logic
   */
  protected boolean epollIsAvailable() {
    return epollPresent;
  }

  /**
   * Subclasses can override this method to perform KQueue is available logic
   */
  protected boolean kQueueIsAvailable() {
    return kQueuePresent;
  }

  @Override
  protected void contextInitialized() {
    super.contextInitialized();
    WebServerApplicationContext context = obtainApplicationContext();
    try {
      WebServerApplicationLoader loader
              = new WebServerApplicationLoader(this::getMergedInitializers);
      loader.setApplicationContext(context);
      loader.onStartup(context);
    }
    catch (Throwable e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  @Override
  public void start() {
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

    bootstrap.group(parentGroup, childGroup)
            .channel(socketChannel);

    NettyServerInitializer nettyServerInitializer = getNettyServerInitializer();
    Assert.state(nettyServerInitializer != null, "No NettyServerInitializer");

    bootstrap.childHandler(nettyServerInitializer);
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

    postBootstrap(bootstrap);

    ChannelFuture channelFuture = bootstrap.bind(getHost(), getPort());
    try {
      channelFuture.sync();
    }
    catch (InterruptedException e) {
      log.error("Interrupted", e);
      throw new WebServerException(e);
    }
  }

  /**
   * before bootstrap
   *
   * @param bootstrap netty ServerBootstrap
   */
  protected void preBootstrap(ServerBootstrap bootstrap) {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    // adjust context path
    WebServerApplicationContext context = obtainApplicationContext();
    NettyRequestContextConfig contextConfig = context.getBean(NettyRequestContextConfig.class);
    Assert.state(contextConfig != null, "No NettyRequestContextConfig");
    String contextPath = contextConfig.getContextPath();
    String serverContextPath = getContextPath();
    if (contextPath == null) {
      contextConfig.setContextPath(serverContextPath);
    }
    else {
      if (serverContextPath != null) {
        if (!Objects.equals(serverContextPath, contextPath)) {
          log.info("Using NettyRequestContextConfig 'contextPath' -> '{}'", contextPath);
          setContextPath(contextPath);
          // reset contextPath
          applyContextPath(context);
        }
      }
      else {
        setContextPath(contextPath);
      }
    }
  }

  /**
   * after bootstrap
   *
   * @param bootstrap netty ServerBootstrap
   */
  protected void postBootstrap(ServerBootstrap bootstrap) {
    log.info("Netty web server started on port: [{}] with context path '{}'", getPort(), getContextPath());

    if (loggingLevel != null) {
      bootstrap.handler(new LoggingHandler(loggingLevel));
    }
  }

  protected NettyServerInitializer getNettyServerInitializer() {
    NettyServerInitializer serverInitializer = this.nettyServerInitializer;

    if (serverInitializer == null) {
      WebServerApplicationContext context = obtainApplicationContext();
      serverInitializer = context.getBean(NettyServerInitializer.class);
      if (serverInitializer == null) {
        ReactiveChannelHandler reactiveDispatcher = context.getBean(ReactiveChannelHandler.class);
        serverInitializer = new NettyServerInitializer(reactiveDispatcher);
      }
    }
    return serverInitializer;
  }

  @Override
  public void destroy() throws Exception {
    stop();
  }

  @Override
  public void stop() {
    log.info("Shutdown netty web server: [{}]", this);

    if (this.parentGroup != null) {
      this.parentGroup.shutdownGracefully();
    }
    if (this.childGroup != null) {
      this.childGroup.shutdownGracefully();
    }
  }

  public EventLoopGroup getChildGroup() {
    return childGroup;
  }

  public EventLoopGroup getParentGroup() {
    return parentGroup;
  }

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

  public void setNettyServerInitializer(NettyServerInitializer nettyServerInitializer) {
    this.nettyServerInitializer = nettyServerInitializer;
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
  public LogLevel getLoggingLevel() {
    return loggingLevel;
  }

  static class EpollDelegate {
    static void init(ServerBootstrap bootstrap, NettyWebServer server) {
      bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
      if (server.socketChannel == null) {
        server.setSocketChannel(EpollServerSocketChannel.class);
      }
      if (server.parentGroup == null) {
        server.setParentGroup(new EpollEventLoopGroup(
                server.getParentThreadCount(), new DefaultThreadFactory("epoll-parent@")));
      }
      if (server.childGroup == null) {
        server.setChildGroup(new EpollEventLoopGroup(
                server.getChildThreadCount(), new DefaultThreadFactory("epoll-child@")));
      }
    }
  }

  static class KQueueDelegate {
    static void init(NettyWebServer server) {
      if (server.socketChannel == null) {
        server.setSocketChannel(KQueueServerSocketChannel.class);
      }
      if (server.parentGroup == null) {
        server.setParentGroup(new KQueueEventLoopGroup(
                server.getParentThreadCount(), new DefaultThreadFactory("kQueue-parent@")));
      }
      if (server.childGroup == null) {
        server.setChildGroup(new KQueueEventLoopGroup(
                server.getChildThreadCount(), new DefaultThreadFactory("kQueue-child@")));
      }
    }
  }
}
