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
import java.util.List;

import cn.taketoday.annotation.config.web.netty.ServerBootstrapCustomizer;
import cn.taketoday.framework.web.server.AbstractConfigurableWebServerFactory;
import cn.taketoday.framework.web.server.ChannelWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties.Netty;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.NetUtil;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultThreadFactory;

import static cn.taketoday.util.ClassUtils.isPresent;

/**
 * Factory for {@link NettyWebServer}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/20 13:44
 */
public class NettyWebServerFactory extends AbstractConfigurableWebServerFactory implements ChannelWebServerFactory {

  /**
   * the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  private int workerThreadCount = 4;

  /**
   * the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For parent {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  private int acceptorThreadCount = 2;

  /**
   * The SOMAXCONN value of the current machine.  If failed to get the value,  {@code 200} is used as a
   * default value for Windows and {@code 128} for others.
   * <p>
   * so_backlog
   */
  private int maxConnection = NetUtil.SOMAXCONN;

  @Nullable
  private EventLoopGroup workerGroup;

  @Nullable
  private EventLoopGroup acceptorGroup;

  @Nullable
  private Class<? extends ServerSocketChannel> socketChannel;

  @Nullable
  private LogLevel loggingLevel;

  @Nullable
  private List<ServerBootstrapCustomizer> bootstrapCustomizers;

  private Netty nettyConfig = new Netty();

  /**
   * EventLoopGroup for acceptor
   *
   * @param acceptorGroup acceptor
   */
  public void setAcceptorGroup(@Nullable EventLoopGroup acceptorGroup) {
    this.acceptorGroup = acceptorGroup;
  }

  /**
   * set the worker EventLoopGroup
   *
   * @param workerGroup worker
   */
  public void setWorkerGroup(@Nullable EventLoopGroup workerGroup) {
    this.workerGroup = workerGroup;
  }

  public void setSocketChannel(@Nullable Class<? extends ServerSocketChannel> socketChannel) {
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
  public void setAcceptorThreadCount(int acceptorThreadCount) {
    this.acceptorThreadCount = acceptorThreadCount;
  }

  /**
   * get the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For parent {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public int getAcceptorThreadCount() {
    return acceptorThreadCount;
  }

  /**
   * set the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public void setWorkerThreadCount(int workThreadCount) {
    this.workerThreadCount = workThreadCount;
  }

  /**
   * The SOMAXCONN value of the current machine.  If failed to get the value,  {@code 200} is used as a
   * default value for Windows and {@code 128} for others.
   * <p>
   * so_backlog
   */
  public void setMaxConnection(int maxConnection) {
    this.maxConnection = maxConnection;
  }

  /**
   * get the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public int getWorkThreadCount() {
    return workerThreadCount;
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

  public void setBootstrapCustomizers(@Nullable List<ServerBootstrapCustomizer> bootstrapCustomizers) {
    this.bootstrapCustomizers = bootstrapCustomizers;
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

  @Nullable
  public EventLoopGroup getWorkerGroup() {
    return workerGroup;
  }

  @Nullable
  public EventLoopGroup getAcceptorGroup() {
    return acceptorGroup;
  }

  @Nullable
  public Class<? extends ServerSocketChannel> getSocketChannel() {
    return socketChannel;
  }

  /**
   * Subclasses can override this method to perform epoll is available logic
   */
  protected boolean epollIsAvailable() {
    return isPresent("io.netty.channel.epoll.EpollServerSocketChannel", getClass())
            && Epoll.isAvailable();
  }

  /**
   * Subclasses can override this method to perform KQueue is available logic
   */
  protected boolean kQueueIsAvailable() {
    return isPresent("io.netty.channel.kqueue.KQueueServerSocketChannel", getClass())
            && KQueue.isAvailable();
  }

  @Override
  public WebServer getWebServer(ChannelHandler channelHandler) {
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
      if (acceptorGroup == null) {
        acceptorGroup = new NioEventLoopGroup(acceptorThreadCount, new DefaultThreadFactory("acceptor"));
      }
      if (workerGroup == null) {
        workerGroup = new NioEventLoopGroup(workerThreadCount, new DefaultThreadFactory("workers"));
      }
      if (socketChannel == null) {
        socketChannel = NioServerSocketChannel.class;
      }
    }

    Assert.state(workerGroup != null, "No 'workerGroup'");
    Assert.state(acceptorGroup != null, "No 'acceptorGroup'");

    bootstrap.group(acceptorGroup, workerGroup);
    bootstrap.channel(socketChannel);
    bootstrap.option(ChannelOption.SO_BACKLOG, maxConnection);

    if (loggingLevel != null) {
      bootstrap.handler(new LoggingHandler(loggingLevel));
    }

    bootstrap.childHandler(createChannelInitializer(nettyConfig, channelHandler));
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

    if (bootstrapCustomizers != null) {
      for (ServerBootstrapCustomizer customizer : bootstrapCustomizers) {
        customizer.customize(bootstrap);
      }
    }

    postBootstrap(bootstrap);

    InetSocketAddress listenAddress = getListenAddress();
    return new NettyWebServer(acceptorGroup, workerGroup, bootstrap, listenAddress, nettyConfig.shutdown);
  }

  /**
   * Creates Infra netty channel initializer
   *
   * @param netty netty config
   * @param channelHandler ChannelInboundHandler
   */
  protected ChannelInitializer<Channel> createChannelInitializer(Netty netty, ChannelHandler channelHandler) {
    var initializer = createInitializer(channelHandler);
    initializer.setHttpDecoderConfig(createHttpDecoderConfig(netty));
    initializer.setMaxContentLength(netty.maxContentLength.toBytesInt());
    initializer.setCloseOnExpectationFailed(netty.closeOnExpectationFailed);
    return initializer;
  }

  protected final HttpDecoderConfig createHttpDecoderConfig(Netty netty) {
    return new HttpDecoderConfig()
            .setInitialBufferSize(netty.initialBufferSize.toBytesInt())
            .setMaxChunkSize(netty.maxChunkSize.toBytesInt())
            .setMaxHeaderSize(netty.maxHeaderSize)
            .setValidateHeaders(netty.validateHeaders)
            .setChunkedSupported(netty.chunkedSupported)
            .setAllowPartialChunks(netty.allowPartialChunks)
            .setMaxInitialLineLength(netty.maxInitialLineLength)
            .setAllowDuplicateContentLengths(netty.allowDuplicateContentLengths);
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

  }

  public void applyFrom(Netty netty) {
    if (netty.loggingLevel != null) {
      setLoggingLevel(netty.loggingLevel);
    }

    if (netty.socketChannel != null) {
      setSocketChannel(netty.socketChannel);
    }

    if (netty.acceptorThreads != null) {
      setAcceptorThreadCount(netty.acceptorThreads);
    }

    if (netty.workerThreads != null) {
      setWorkerThreadCount(netty.workerThreads);
    }

    if (netty.maxConnection != null) {
      setMaxConnection(netty.maxConnection);
    }

    nettyConfig = netty;
  }

  private NettyChannelInitializer createInitializer(ChannelHandler channelHandler) {
    Ssl ssl = getSsl();
    if (Ssl.isEnabled(ssl)) {
      SSLNettyChannelInitializer initializer = new SSLNettyChannelInitializer(
              channelHandler, isHttp2Enabled(), ssl, getSslBundle(), getServerNameSslBundles());
      addBundleUpdateHandler(null, ssl.bundle, initializer);
      for (var pair : ssl.serverNameBundles) {
        addBundleUpdateHandler(pair.serverName, pair.bundle, initializer);
      }
      return initializer;
    }
    return new NettyChannelInitializer(channelHandler);
  }

  private void addBundleUpdateHandler(@Nullable String serverName, @Nullable String bundleName, SSLNettyChannelInitializer initializer) {
    if (StringUtils.hasText(bundleName)) {
      getSslBundles().addBundleUpdateHandler(bundleName, sslBundle -> initializer.updateSSLBundle(serverName, sslBundle));
    }
  }

  private InetSocketAddress getListenAddress() {
    if (getAddress() != null) {
      return new InetSocketAddress(getAddress().getHostAddress(), getPort());
    }
    return new InetSocketAddress(getPort());
  }

  static class EpollDelegate {
    static void init(ServerBootstrap bootstrap, NettyWebServerFactory factory) {
      bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
      if (factory.getSocketChannel() == null) {
        factory.setSocketChannel(EpollServerSocketChannel.class);
      }
      if (factory.getAcceptorGroup() == null) {
        factory.setAcceptorGroup(new EpollEventLoopGroup(
                factory.acceptorThreadCount, new DefaultThreadFactory("epoll-acceptor")));
      }
      if (factory.getWorkerGroup() == null) {
        factory.setWorkerGroup(new EpollEventLoopGroup(
                factory.workerThreadCount, new DefaultThreadFactory("epoll-workers")));
      }
    }
  }

  static class KQueueDelegate {
    static void init(NettyWebServerFactory factory) {
      if (factory.getSocketChannel() == null) {
        factory.setSocketChannel(KQueueServerSocketChannel.class);
      }
      if (factory.getAcceptorGroup() == null) {
        factory.setAcceptorGroup(new KQueueEventLoopGroup(
                factory.workerThreadCount, new DefaultThreadFactory("kQueue-acceptor")));
      }
      if (factory.getWorkerGroup() == null) {
        factory.setWorkerGroup(new KQueueEventLoopGroup(
                factory.workerThreadCount, new DefaultThreadFactory("kQueue-workers")));
      }
    }
  }

}
