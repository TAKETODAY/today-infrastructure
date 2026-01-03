/*
 * Copyright 2017 - 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.List;

import infra.lang.Assert;
import infra.web.server.AbstractConfigurableWebServerFactory;
import infra.web.server.GenericWebServerFactory;
import infra.web.server.ServerProperties.Netty;
import infra.web.server.Ssl;
import infra.web.server.WebServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.NetUtil;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultThreadFactory;

import static infra.util.ClassUtils.isPresent;

/**
 * Factory for {@link NettyWebServer}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/20 13:44
 */
public class NettyWebServerFactory extends AbstractConfigurableWebServerFactory implements GenericWebServerFactory {

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

  private @Nullable EventLoopGroup workerGroup;

  private @Nullable EventLoopGroup acceptorGroup;

  private @Nullable Class<? extends ServerSocketChannel> socketChannel;

  private @Nullable LogLevel loggingLevel;

  private @Nullable List<ServerBootstrapCustomizer> bootstrapCustomizers;

  private Netty nettyConfig = new Netty();

  private @Nullable ChannelConfigurer channelConfigurer;

  /** @since 5.0 */
  private @Nullable String workerPoolName;

  /** @since 5.0 */
  private @Nullable String acceptorPoolName;

  private @Nullable ChannelHandler channelHandler;

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

  /**
   * Set {@link ChannelConfigurer}
   *
   * @param channelConfigurer ChannelConfigurer
   */
  public void setChannelConfigurer(@Nullable ChannelConfigurer channelConfigurer) {
    this.channelConfigurer = channelConfigurer;
  }

  /**
   * Set {@link ServerBootstrapCustomizer}
   *
   * @param bootstrapCustomizers ServerBootstrapCustomizer list
   */
  public void setBootstrapCustomizers(@Nullable List<ServerBootstrapCustomizer> bootstrapCustomizers) {
    this.bootstrapCustomizers = bootstrapCustomizers;
  }

  /**
   * Set the worker thread pool name
   *
   * @since 5.0
   */
  public void setWorkerPoolName(@Nullable String workerPoolName) {
    this.workerPoolName = workerPoolName;
  }

  /**
   * Set the acceptor thread pool name
   *
   * @since 5.0
   */
  public void setAcceptorPoolName(@Nullable String acceptorPoolName) {
    this.acceptorPoolName = acceptorPoolName;
  }

  /**
   * set ChannelHandler
   *
   * @since 5.0
   */
  public void setChannelHandler(@Nullable ChannelHandler channelHandler) {
    this.channelHandler = channelHandler;
  }

  /**
   * Get {@link LoggingHandler} logging Level
   *
   * @see LogLevel
   * @see LoggingHandler
   * @see ServerBootstrap#handler
   */
  public @Nullable LogLevel getLoggingLevel() {
    return loggingLevel;
  }

  public @Nullable EventLoopGroup getWorkerGroup() {
    return workerGroup;
  }

  public @Nullable EventLoopGroup getAcceptorGroup() {
    return acceptorGroup;
  }

  public @Nullable Class<? extends ServerSocketChannel> getSocketChannel() {
    return socketChannel;
  }

  public @Nullable ChannelHandler getChannelHandler() {
    return channelHandler;
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
  public WebServer getWebServer() {
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
      IoHandlerFactory ioHandlerFactory = NioIoHandler.newFactory();
      if (acceptorGroup == null) {
        acceptorGroup = new MultiThreadIoEventLoopGroup(acceptorThreadCount,
                new DefaultThreadFactory(acceptorPoolName == null ? "acceptor" : acceptorPoolName), ioHandlerFactory);
      }
      if (workerGroup == null) {
        workerGroup = new MultiThreadIoEventLoopGroup(workerThreadCount,
                new DefaultThreadFactory(workerPoolName == null ? "workers" : workerPoolName), ioHandlerFactory);
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

    ChannelHandler channelHandler = getChannelHandler();
    Assert.state(channelHandler != null, "No 'channelHandler' set");
    bootstrap.childHandler(createChannelInitializer(nettyConfig, channelHandler));
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

    if (bootstrapCustomizers != null) {
      for (ServerBootstrapCustomizer customizer : bootstrapCustomizers) {
        customizer.customize(bootstrap);
      }
    }

    postBootstrap(bootstrap);

    InetSocketAddress listenAddress = getListenAddress();
    return new NettyWebServer(acceptorGroup, workerGroup, bootstrap,
            listenAddress, nettyConfig.shutdown, Ssl.isEnabled(getSsl()));
  }

  /**
   * Creates Infra netty channel initializer
   *
   * @param netty netty config
   * @param channelHandler ChannelHandler
   */
  protected ChannelInitializer<Channel> createChannelInitializer(Netty netty, ChannelHandler channelHandler) {
    return createInitializer(channelHandler, createHttpDecoderConfig(netty));
  }

  protected final HttpDecoderConfig createHttpDecoderConfig(Netty netty) {
    return new HttpDecoderConfig()
            .setInitialBufferSize(netty.initialBufferSize.toBytesInt())
            .setMaxChunkSize(netty.maxChunkSize.toBytesInt())
            .setMaxHeaderSize(netty.maxHeaderSize.toBytesInt())
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

    setWorkerPoolName(netty.workerPoolName);
    setAcceptorPoolName(netty.acceptorPoolName);

    nettyConfig = netty;
  }

  private NettyChannelInitializer createInitializer(ChannelHandler channelHandler, HttpDecoderConfig config) {
    Ssl ssl = getSsl();
    if (Ssl.isEnabled(ssl)) {
      SSLNettyChannelInitializer initializer = new SSLNettyChannelInitializer(channelHandler, config,
              channelConfigurer, isHttp2Enabled(), ssl, getSslBundle(), getServerNameSslBundles());
      addBundleUpdateHandler(ssl, initializer::updateSSLBundle);
      return initializer;
    }
    return new NettyChannelInitializer(channelHandler, channelConfigurer, config);
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
      IoHandlerFactory ioHandlerFactory = EpollIoHandler.newFactory();

      if (factory.getAcceptorGroup() == null) {
        factory.setAcceptorGroup(new MultiThreadIoEventLoopGroup(
                factory.acceptorThreadCount, new DefaultThreadFactory(
                factory.acceptorPoolName == null ? "epoll-acceptor" : factory.acceptorPoolName), ioHandlerFactory));
      }
      if (factory.getWorkerGroup() == null) {
        factory.setWorkerGroup(new MultiThreadIoEventLoopGroup(
                factory.workerThreadCount, new DefaultThreadFactory(
                factory.workerPoolName == null ? "epoll-workers" : factory.workerPoolName), ioHandlerFactory));
      }
    }
  }

  static class KQueueDelegate {
    static void init(NettyWebServerFactory factory) {
      if (factory.getSocketChannel() == null) {
        factory.setSocketChannel(KQueueServerSocketChannel.class);
      }
      IoHandlerFactory ioHandlerFactory = KQueueIoHandler.newFactory();
      if (factory.getAcceptorGroup() == null) {
        factory.setAcceptorGroup(new MultiThreadIoEventLoopGroup(
                factory.workerThreadCount, new DefaultThreadFactory(
                factory.acceptorPoolName == null ? "kQueue-acceptor" : factory.acceptorPoolName), ioHandlerFactory));
      }
      if (factory.getWorkerGroup() == null) {
        factory.setWorkerGroup(new MultiThreadIoEventLoopGroup(
                factory.workerThreadCount, new DefaultThreadFactory(
                factory.workerPoolName == null ? "kQueue-workers" : factory.workerPoolName), ioHandlerFactory));
      }
    }
  }

}
