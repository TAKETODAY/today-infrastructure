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

import org.jspecify.annotations.Nullable;

import java.net.SocketAddress;
import java.util.List;

import infra.lang.Assert;
import infra.web.server.AbstractConfigurableWebServerFactory;
import infra.web.server.GenericWebServerFactory;
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
import io.netty.channel.ServerChannel;
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
 * Factory for creating {@link NettyWebServer} instances.
 * This factory handles the configuration and setup of Netty server components including
 * event loop groups, channel initialization, SSL support, HTTP/2 configuration, and various
 * Netty-specific options such as thread counts, connection limits, and logging levels.
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

  private @Nullable Class<? extends ServerChannel> socketChannel;

  private @Nullable LogLevel loggingLevel;

  private @Nullable List<ServerBootstrapCustomizer> bootstrapCustomizers;

  private NettyServerProperties nettyConfig = new NettyServerProperties();

  private @Nullable ChannelConfigurer channelConfigurer;

  /** @since 5.0 */
  private @Nullable String workerPoolName;

  /** @since 5.0 */
  private @Nullable String acceptorPoolName;

  private @Nullable ChannelHandler httpTrafficHandler;

  /**
   * Sets the acceptor {@link EventLoopGroup} for the server.
   * The acceptor group is responsible for accepting incoming connections.
   * If not set, a default {@link MultiThreadIoEventLoopGroup} will be created during server initialization.
   *
   * @param acceptorGroup the acceptor {@link EventLoopGroup} to use, or {@code null} to use the default
   * @since 4.0
   */
  public void setAcceptorGroup(@Nullable EventLoopGroup acceptorGroup) {
    this.acceptorGroup = acceptorGroup;
  }

  /**
   * Sets the worker {@link EventLoopGroup} for the server.
   * The worker group is responsible for handling I/O operations for accepted connections.
   * If not set, a default {@link MultiThreadIoEventLoopGroup} will be created during server initialization.
   *
   * @param workerGroup the worker {@link EventLoopGroup} to use, or {@code null} to use the default
   * @since 4.0
   */
  public void setWorkerGroup(@Nullable EventLoopGroup workerGroup) {
    this.workerGroup = workerGroup;
  }

  /**
   * Sets the server socket channel class to be used by the Netty server.
   * This allows specifying the type of server socket channel to use, such as
   * {@link NioServerSocketChannel}, {@link EpollServerSocketChannel}, or {@link KQueueServerSocketChannel},
   * depending on the platform and transport mechanism desired.
   *
   * @param socketChannel the server socket channel class to use, or {@code null} to use the default
   * @see ServerSocketChannel
   * @see NioServerSocketChannel
   * @see EpollServerSocketChannel
   * @see KQueueServerSocketChannel
   */
  public void setSocketChannel(@Nullable Class<? extends ServerChannel> socketChannel) {
    this.socketChannel = socketChannel;
  }

  /**
   * Sets the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For parent {@link EventLoopGroup} which accepts incoming connections
   *
   * @param acceptorThreadCount the number of acceptor threads
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public void setAcceptorThreadCount(int acceptorThreadCount) {
    this.acceptorThreadCount = acceptorThreadCount;
  }

  /**
   * Gets the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For parent {@link EventLoopGroup}
   *
   * @return the number of acceptor threads
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public int getAcceptorThreadCount() {
    return acceptorThreadCount;
  }

  /**
   * Sets the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @param workThreadCount the number of worker threads
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public void setWorkerThreadCount(int workThreadCount) {
    this.workerThreadCount = workThreadCount;
  }

  /**
   * Sets the maximum number of pending connections that the server socket can hold in its backlog queue.
   * This corresponds to the {@code SO_BACKLOG} socket option.
   * <p>
   * The SOMAXCONN value of the current machine is typically used as a reference.
   * If failed to get the system value, {@code 200} is used as a default value for Windows
   * and {@code 128} for other operating systems.
   *
   * @param maxConnection the maximum number of pending connections in the server socket's backlog
   * @see #maxConnection
   * @see ChannelOption#SO_BACKLOG
   */
  public void setMaxConnection(int maxConnection) {
    this.maxConnection = maxConnection;
  }

  /**
   * Gets the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @return the number of worker threads
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public int getWorkThreadCount() {
    return workerThreadCount;
  }

  /**
   * Sets the logging level for the {@link LoggingHandler}.
   * <p>
   * If the provided {@code loggingLevel} is {@code null}, no logging handler will be registered.
   *
   * @param loggingLevel the logging level to set
   * @see LogLevel
   * @see LoggingHandler
   * @see ServerBootstrap#handler
   */
  public void setLoggingLevel(LogLevel loggingLevel) {
    this.loggingLevel = loggingLevel;
  }

  /**
   * Set {@link ChannelConfigurer} to customize channel configuration.
   * The channel configurer allows for additional customization of the channel
   * after it has been initialized but before it's registered with the event loop.
   *
   * @param channelConfigurer ChannelConfigurer instance to be used for configuring channels,
   * or {@code null} to clear the existing configurer
   * @since 4.0
   */
  public void setChannelConfigurer(@Nullable ChannelConfigurer channelConfigurer) {
    this.channelConfigurer = channelConfigurer;
  }

  /**
   * Sets the list of {@link ServerBootstrapCustomizer} instances that will be applied
   * to customize the Netty {@link ServerBootstrap} during web server initialization.
   * These customizers allow for additional configuration of the server bootstrap,
   * such as setting custom channel options or other Netty-specific settings.
   *
   * @param bootstrapCustomizers A list of {@link ServerBootstrapCustomizer} instances
   * that will be used to customize the {@link ServerBootstrap}, or {@code null}
   * to clear any previously set customizers
   * @since 4.0
   */
  public void setBootstrapCustomizers(@Nullable List<ServerBootstrapCustomizer> bootstrapCustomizers) {
    this.bootstrapCustomizers = bootstrapCustomizers;
  }

  /**
   * Set the worker thread pool name
   *
   * @param workerPoolName the name of the worker thread pool
   * @since 5.0
   */
  public void setWorkerPoolName(@Nullable String workerPoolName) {
    this.workerPoolName = workerPoolName;
  }

  /**
   * Set the acceptor thread pool name
   *
   * @param acceptorPoolName the name of the acceptor thread pool
   * @since 5.0
   */
  public void setAcceptorPoolName(@Nullable String acceptorPoolName) {
    this.acceptorPoolName = acceptorPoolName;
  }

  /**
   * Sets the HTTP traffic handler for processing incoming requests.
   *
   * @param trafficHandler the channel handler responsible for handling
   * HTTP traffic, or {@code null} to clear the handler
   * @since 5.0
   */
  public void setHttpTrafficHandler(@Nullable ChannelHandler trafficHandler) {
    this.httpTrafficHandler = trafficHandler;
  }

  /**
   * Returns the logging level for the {@link LoggingHandler}.
   *
   * @return the logging level, or {@code null} if no logging handler is configured
   * @see LogLevel
   * @see LoggingHandler
   * @see ServerBootstrap#handler
   */
  public @Nullable LogLevel getLoggingLevel() {
    return loggingLevel;
  }

  /**
   * Returns the worker {@link EventLoopGroup} used by the server.
   *
   * @return the worker {@link EventLoopGroup}, or {@code null} if not set
   * @since 4.0
   */
  public @Nullable EventLoopGroup getWorkerGroup() {
    return workerGroup;
  }

  /**
   * Returns the acceptor {@link EventLoopGroup} used by the server.
   *
   * @return the acceptor {@link EventLoopGroup}, or {@code null} if not set
   * @since 4.0
   */
  public @Nullable EventLoopGroup getAcceptorGroup() {
    return acceptorGroup;
  }

  /**
   * Returns the server socket channel class used by the Netty server.
   *
   * @return the server socket channel class, or {@code null} if not set
   * @see ServerSocketChannel
   */
  public @Nullable Class<? extends ServerChannel> getSocketChannel() {
    return socketChannel;
  }

  /**
   * Returns the HTTP traffic handler for processing incoming requests.
   *
   * @return the channel handler responsible for handling HTTP traffic, or {@code null} if not set
   * @since 5.0
   */
  public @Nullable ChannelHandler getHttpTrafficHandler() {
    return httpTrafficHandler;
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
  public WebServer createWebServer() {
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

    ChannelHandler httpTrafficHandler = getHttpTrafficHandler();
    Assert.state(httpTrafficHandler != null, "No 'httpTrafficHandler' set");
    bootstrap.childHandler(createChannelInitializer(nettyConfig, httpTrafficHandler));
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

    if (bootstrapCustomizers != null) {
      for (ServerBootstrapCustomizer customizer : bootstrapCustomizers) {
        customizer.customize(bootstrap);
      }
    }

    postBootstrap(bootstrap);

    boolean http2Enabled = isHttp2Enabled();
    SocketAddress bindAddress = bindAddress();
    return new NettyWebServer(acceptorGroup, workerGroup, bootstrap,
            bindAddress, nettyConfig.shutdown, Ssl.isEnabled(getSsl()), http2Enabled);
  }

  /**
   * Creates a Netty channel initializer for the given configuration and HTTP traffic handler.
   *
   * @param netty the Netty server properties configuration
   * @param httpTrafficHandler the channel handler responsible for processing HTTP traffic
   * @return a {@link ChannelInitializer} instance configured with the provided parameters
   */
  protected ChannelInitializer<Channel> createChannelInitializer(NettyServerProperties netty, ChannelHandler httpTrafficHandler) {
    return createInitializer(httpTrafficHandler, createHttpDecoderConfig(netty));
  }

  protected final HttpDecoderConfig createHttpDecoderConfig(NettyServerProperties netty) {
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
   * Called before initializing the Netty server bootstrap.
   * This method is invoked during the server creation process prior to setting up
   * the channel and event loop groups. It provides an opportunity to configure
   * global Netty settings or perform initialization tasks needed before the
   * server starts.
   *
   * @param bootstrap the Netty {@link ServerBootstrap} instance being prepared
   * @since 4.0
   */
  protected void preBootstrap(ServerBootstrap bootstrap) {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
  }

  /**
   * Called after the Netty server bootstrap has been fully configured but before
   * the server begins accepting connections. This method allows for final
   * modifications to the bootstrap configuration or additional setup tasks
   * that should occur after all standard configuration is complete.
   *
   * @param bootstrap the Netty {@link ServerBootstrap} instance that has been configured
   * @since 4.0
   */
  protected void postBootstrap(ServerBootstrap bootstrap) {
  }

  public void applyFrom(NettyServerProperties netty) {
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

  private HttpChannelInitializer createInitializer(ChannelHandler httpTrafficHandler, HttpDecoderConfig config) {
    Ssl ssl = getSsl();
    if (Ssl.isEnabled(ssl)) {
      SecuredHttpChannelInitializer initializer = new SecuredHttpChannelInitializer(httpTrafficHandler, config,
              channelConfigurer, isHttp2Enabled(), ssl, getSslBundle(), getServerNameSslBundles());
      addBundleUpdateHandler(ssl, initializer::updateSSLBundle);
      return initializer;
    }
    return new HttpChannelInitializer(httpTrafficHandler, isHttp2Enabled(), channelConfigurer, config);
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
