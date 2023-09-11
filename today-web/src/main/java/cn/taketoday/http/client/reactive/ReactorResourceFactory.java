/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.http.client.reactive;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.Lifecycle;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

/**
 * Factory to manage Reactor Netty resources, i.e. {@link LoopResources} for
 * event loop threads, and {@link ConnectionProvider} for the connection pool,
 * within the lifecycle of a Infra {@code ApplicationContext}.
 *
 * <p>This factory implements {@link InitializingBean}, {@link DisposableBean}
 * and {@link Lifecycle} and is expected typically to be declared as a
 * Infra-managed bean.
 *
 * <p>Notice that after a {@link Lifecycle} stop/restart, new instances of
 * the configured {@link LoopResources} and {@link ConnectionProvider} are
 * created, so any references to those should be updated.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactorResourceFactory implements InitializingBean, DisposableBean, Lifecycle {

  private boolean useGlobalResources = true;

  @Nullable
  private LoopResources loopResources;

  @Nullable
  private ConnectionProvider connectionProvider;

  @Nullable
  private Consumer<HttpResources> globalResourcesConsumer;

  private Supplier<LoopResources> loopResourcesSupplier = () -> LoopResources.create("reactive-http");
  private Supplier<ConnectionProvider> connectionProviderSupplier = () -> ConnectionProvider.create("reactive", 500);

  private boolean manageLoopResources = false;
  private boolean manageConnectionProvider = false;
  private Duration shutdownTimeout = Duration.ofSeconds(LoopResources.DEFAULT_SHUTDOWN_TIMEOUT);
  private Duration shutdownQuietPeriod = Duration.ofSeconds(LoopResources.DEFAULT_SHUTDOWN_QUIET_PERIOD);

  private volatile boolean running;

  private final Object lifecycleMonitor = new Object();

  /**
   * Whether to use global Reactor Netty resources via {@link HttpResources}.
   * <p>Default is "true" in which case this factory initializes and stops the
   * global Reactor Netty resources within {@code ApplicationContext}
   * lifecycle. If set to "false" the factory manages its resources independent
   * of the global ones.
   *
   * @param useGlobalResources whether to expose and manage the global resources
   * @see #addGlobalResourcesConsumer(Consumer)
   */
  public void setUseGlobalResources(boolean useGlobalResources) {
    this.useGlobalResources = useGlobalResources;
  }

  /**
   * Whether this factory exposes the global
   * {@link reactor.netty.http.HttpResources HttpResources} holder.
   */
  public boolean isUseGlobalResources() {
    return this.useGlobalResources;
  }

  /**
   * Add a Consumer for configuring the global Reactor Netty resources on
   * startup. When this option is used, {@link #setUseGlobalResources} is also
   * enabled.
   *
   * @param consumer the consumer to apply
   * @see #setUseGlobalResources(boolean)
   */
  public void addGlobalResourcesConsumer(Consumer<HttpResources> consumer) {
    this.useGlobalResources = true;
    this.globalResourcesConsumer = this.globalResourcesConsumer != null
                                   ? this.globalResourcesConsumer.andThen(consumer)
                                   : consumer;
  }

  /**
   * Use this when you don't want to participate in global resources and
   * you want to customize the creation of the managed {@code ConnectionProvider}.
   * <p>By default, {@code ConnectionProvider.elastic("http")} is used.
   * <p>Note that this option is ignored if {@code userGlobalResources=false} or
   * {@link #setConnectionProvider(ConnectionProvider)} is set.
   *
   * @param supplier the supplier to use
   */
  public void setConnectionProviderSupplier(Supplier<ConnectionProvider> supplier) {
    this.connectionProviderSupplier = supplier;
  }

  /**
   * Use this when you want to provide an externally managed
   * {@link ConnectionProvider} instance.
   *
   * @param connectionProvider the connection provider to use as is
   */
  public void setConnectionProvider(ConnectionProvider connectionProvider) {
    this.connectionProvider = connectionProvider;
  }

  /**
   * Return the configured {@link ConnectionProvider}.
   */
  public ConnectionProvider getConnectionProvider() {
    Assert.state(this.connectionProvider != null, "ConnectionProvider not initialized yet");
    return this.connectionProvider;
  }

  /**
   * Use this when you don't want to participate in global resources and
   * you want to customize the creation of the managed {@code LoopResources}.
   * <p>By default, {@code LoopResources.create("reactor-http")} is used.
   * <p>Note that this option is ignored if {@code userGlobalResources=false} or
   * {@link #setLoopResources(LoopResources)} is set.
   *
   * @param supplier the supplier to use
   */
  public void setLoopResourcesSupplier(Supplier<LoopResources> supplier) {
    this.loopResourcesSupplier = supplier;
  }

  /**
   * Use this option when you want to provide an externally managed
   * {@link LoopResources} instance.
   *
   * @param loopResources the loop resources to use as is
   */
  public void setLoopResources(LoopResources loopResources) {
    this.loopResources = loopResources;
  }

  /**
   * Return the configured {@link LoopResources}.
   */
  public LoopResources getLoopResources() {
    Assert.state(this.loopResources != null, "LoopResources not initialized yet");
    return this.loopResources;
  }

  /**
   * Configure the amount of time we'll wait before shutting down resources.
   * If a task is submitted during the {@code shutdownQuietPeriod}, it is guaranteed
   * to be accepted and the {@code shutdownQuietPeriod} will start over.
   * <p>By default, this is set to
   * {@link LoopResources#DEFAULT_SHUTDOWN_QUIET_PERIOD} which is 2 seconds but
   * can also be overridden with the system property
   * {@link reactor.netty.ReactorNetty#SHUTDOWN_QUIET_PERIOD
   * ReactorNetty.SHUTDOWN_QUIET_PERIOD}.
   *
   * @see #setShutdownTimeout(Duration)
   */
  public void setShutdownQuietPeriod(Duration shutdownQuietPeriod) {
    Assert.notNull(shutdownQuietPeriod, "shutdownQuietPeriod should not be null");
    this.shutdownQuietPeriod = shutdownQuietPeriod;
  }

  /**
   * Configure the maximum amount of time to wait until the disposal of the
   * underlying resources regardless if a task was submitted during the
   * {@code shutdownQuietPeriod}.
   * <p>By default, this is set to
   * {@link LoopResources#DEFAULT_SHUTDOWN_TIMEOUT} which is 15 seconds but
   * can also be overridden with the system property
   * {@link reactor.netty.ReactorNetty#SHUTDOWN_TIMEOUT
   * ReactorNetty.SHUTDOWN_TIMEOUT}.
   *
   * @see #setShutdownQuietPeriod(Duration)
   */
  public void setShutdownTimeout(Duration shutdownTimeout) {
    Assert.notNull(shutdownTimeout, "shutdownTimeout should not be null");
    this.shutdownTimeout = shutdownTimeout;
  }

  @Override
  public void afterPropertiesSet() {
    start();
  }

  @Override
  public void destroy() {
    stop();
  }

  @Override
  public void start() {
    synchronized(this.lifecycleMonitor) {
      if (!isRunning()) {
        if (this.useGlobalResources) {
          Assert.isTrue(this.loopResources == null && this.connectionProvider == null,
                  "'useGlobalResources' is mutually exclusive with explicitly configured resources");
          HttpResources httpResources = HttpResources.get();
          if (this.globalResourcesConsumer != null) {
            this.globalResourcesConsumer.accept(httpResources);
          }
          this.connectionProvider = httpResources;
          this.loopResources = httpResources;
        }
        else {
          if (this.loopResources == null) {
            this.manageLoopResources = true;
            this.loopResources = this.loopResourcesSupplier.get();
          }
          if (this.connectionProvider == null) {
            this.manageConnectionProvider = true;
            this.connectionProvider = this.connectionProviderSupplier.get();
          }
        }
        this.running = true;
      }
    }

  }

  @Override
  public void stop() {
    synchronized(this.lifecycleMonitor) {
      if (isRunning()) {
        if (this.useGlobalResources) {
          HttpResources.disposeLoopsAndConnectionsLater(this.shutdownQuietPeriod, this.shutdownTimeout).block();
          this.connectionProvider = null;
          this.loopResources = null;
        }
        else {
          try {
            ConnectionProvider provider = this.connectionProvider;
            if (provider != null && this.manageConnectionProvider) {
              this.connectionProvider = null;
              provider.disposeLater().block();
            }
          }
          catch (Throwable ex) {
            // ignore
          }

          try {
            LoopResources resources = this.loopResources;
            if (resources != null && this.manageLoopResources) {
              this.loopResources = null;
              resources.disposeLater(this.shutdownQuietPeriod, this.shutdownTimeout).block();
            }
          }
          catch (Throwable ex) {
            // ignore
          }
        }
        this.running = false;
      }
    }
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }
}
