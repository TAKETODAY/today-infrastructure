/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.framework.reactive.server;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.LongAdder;

import javax.annotation.PreDestroy;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.framework.StandardWebServerApplicationContext;
import cn.taketoday.framework.WebServerApplicationContext;
import cn.taketoday.framework.WebServerException;
import cn.taketoday.framework.reactive.NettyWebServerApplicationLoader;
import cn.taketoday.framework.reactive.ReactiveChannelHandler;
import cn.taketoday.framework.server.AbstractWebServer;
import cn.taketoday.framework.server.WebServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ResourceLeakDetector;
import lombok.Setter;

/**
 * @author TODAY 2019-07-02 21:15
 */
@Setter
@Props(prefix = { "server.", "server.netty." })
public class NettyWebServer extends AbstractWebServer implements WebServer {

  private Channel channel;
  private EventLoopGroup childGroup;
  private EventLoopGroup parentGroup;
  private Class<? extends ServerSocketChannel> socketChannel;

  @Override
  protected void initApplicationContext(ApplicationContext context) {
    super.initApplicationContext(context);
    if (context instanceof StandardWebServerApplicationContext) {
      ((StandardWebServerApplicationContext) context).setContextPath(getContextPath());
    }
  }

  protected boolean epollIsAvailable() {
    try {
      Object obj = Class.forName("io.netty.channel.epoll.Epoll").getMethod("isAvailable").invoke(null);
      return obj != null
              && Boolean.parseBoolean(obj.toString())
              && System.getProperty("os.name").toLowerCase().contains("linux");
    }
    catch (Exception e) {
      return false;
    }
  }

  @Override
  protected void prepareInitialize() {
    super.prepareInitialize();
  }

  @Override
  protected void contextInitialized() {
    super.contextInitialized();

    try {
      new NettyWebServerApplicationLoader(this::getMergedInitializers)
              .onStartup(obtainApplicationContext());
    }
    catch (Throwable e) {
      throw new ConfigurationException(e);
    }
  }

  @Override
  public void start() {

    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    final ServerBootstrap bootstrap = new ServerBootstrap();

    int acceptThreadCount = 2;
    int threadCount = 2;

    // enable epoll
    if (epollIsAvailable()) {

      bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
      socketChannel = EpollServerSocketChannel.class;
      this.parentGroup = new EpollEventLoopGroup(threadCount, new NamedThreadFactory("epoll-parent@"));
      this.childGroup = new EpollEventLoopGroup(acceptThreadCount, new NamedThreadFactory("epoll-child@"));
    }
    else {
      this.parentGroup = new NioEventLoopGroup(acceptThreadCount, new NamedThreadFactory("parent@"));
      this.childGroup = new NioEventLoopGroup(threadCount, new NamedThreadFactory("child@"));
      socketChannel = NioServerSocketChannel.class;
    }

    bootstrap.group(getParentGroup(), getChildGroup())
            .channel(getSocketChannel());

    bootstrap.handler(new LoggingHandler(LogLevel.INFO));

    bootstrap.childHandler(obtainNettyServerInitializer());
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

    try {
      channel = bootstrap.bind(getHost(), getPort())
              .sync()
              .channel();
    }
    catch (InterruptedException e) {
      throw new WebServerException(e);
    }
  }

  protected final NettyServerInitializer obtainNettyServerInitializer() {
    NettyServerInitializer ret = getNettyServerInitializer();
    Assert.state(ret != null, "No NettyServerInitializer");
    return ret;
  }

  protected NettyServerInitializer getNettyServerInitializer() {
    final WebServerApplicationContext context = obtainApplicationContext();
    NettyServerInitializer ret = context.getBean(NettyServerInitializer.class);
    if (ret == null) {
      final ReactiveChannelHandler reactiveDispatcher = context.getBean(ReactiveChannelHandler.class);
      ret = new NettyServerInitializer(reactiveDispatcher);
    }
    return ret;
  }

  @PreDestroy
  @Override
  public void stop() {
    log.info("shutdown: [{}]", this);

    if (this.parentGroup != null) {
      this.parentGroup.shutdownGracefully();
    }
    if (this.childGroup != null) {
      this.childGroup.shutdownGracefully();
    }
  }

  public static class NamedThreadFactory implements ThreadFactory {

    private final String prefix;
    private final LongAdder threadNumber = new LongAdder();

    public NamedThreadFactory(String prefix) {
      this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable runnable) {
      threadNumber.add(1);
      return new Thread(runnable, prefix.concat("thread-") + threadNumber.intValue());
    }
  }

  public Channel getChannel() {
    return channel;
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
}
