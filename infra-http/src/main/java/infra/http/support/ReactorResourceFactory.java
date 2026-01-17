/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.support;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.SmartLifecycle;
import infra.lang.Assert;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

/**
 * Factory to manage Reactor Netty resources, i.e. {@link LoopResources} for
 * event loop threads and {@link ConnectionProvider} for the connection pool,
 * within the lifecycle of a Spring {@code ApplicationContext}.
 *
 * <p>This factory implements {@link SmartLifecycle} and is expected typically
 * to be declared as a Spring-managed bean.
 *
 * <p>Note that after a {@link SmartLifecycle} stop/restart, new instances of
 * the configured {@link LoopResources} and {@link ConnectionProvider} are
 * created, so any references to those should be updated. However, this factory
 * does not participate in {@linkplain #isPausable() pause} scenarios.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactorResourceFactory implements ApplicationContextAware, InitializingBean, DisposableBean, SmartLifecycle {

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

  @Nullable
  private ApplicationContext applicationContext;

  /**
   * Whether to use global Reactor Netty resources via {@link HttpResources}.
   * <p>Default is "true" in which case this factory initializes and stops the
   * global Reactor Netty resources within Infra {@code ApplicationContext}
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
    this.globalResourcesConsumer = (this.globalResourcesConsumer != null ?
            this.globalResourcesConsumer.andThen(consumer) : consumer);
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
   * <p>Lazily tries to start the resources on demand if not initialized yet.
   *
   * @see #start()
   */
  public ConnectionProvider getConnectionProvider() {
    if (this.connectionProvider == null) {
      start();
    }
    ConnectionProvider connectionProvider = this.connectionProvider;
    Assert.state(connectionProvider != null, "ConnectionProvider not initialized");
    return connectionProvider;
  }

  /**
   * Use this when you don't want to participate in global resources and
   * you want to customize the creation of the managed {@code LoopResources}.
   * <p>By default, {@code LoopResources.create("reactive-http")} is used.
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
   * <p>Lazily tries to start the resources on demand if not initialized yet.
   *
   * @see #start()
   */
  public LoopResources getLoopResources() {
    if (this.loopResources == null) {
      start();
    }
    LoopResources loopResources = this.loopResources;
    Assert.state(loopResources != null, "LoopResources not initialized");
    return loopResources;
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

  /**
   * Setting an {@link ApplicationContext} is optional: If set, Reactor resources
   * will be initialized in the {@link #start() lifecycle start} phase and closed
   * in the {@link #stop() lifecycle stop} phase. If not set, it will happen in
   * {@link #afterPropertiesSet()} and {@link #destroy()}, respectively.
   */
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Starts the resources if initialized outside an ApplicationContext.
   * This is for backwards compatibility; the preferred way is to rely on
   * the ApplicationContext's {@link SmartLifecycle lifecycle management}.
   *
   * @see #start()
   */
  @Override
  public void afterPropertiesSet() {
    if (this.applicationContext == null) {
      start();
    }
  }

  /**
   * Stops the resources if initialized outside an ApplicationContext.
   * This is for backwards compatibility; the preferred way is to rely on
   * the ApplicationContext's {@link SmartLifecycle lifecycle management}.
   *
   * @see #stop()
   */
  @Override
  public void destroy() {
    if (this.applicationContext == null) {
      stop();
    }
  }

  @Override
  public void start() {
    synchronized(this.lifecycleMonitor) {
      if (!this.running) {
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
      if (this.running) {
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

  /**
   * Returns {@code false} to indicate that a {@code ReactorResourceFactory}
   * should be skipped in a pause scenario.
   *
   * @since 5.0
   */
  @Override
  public boolean isPausable() {
    return false;
  }

  @Override
  public int getPhase() {
    // Same as plain Lifecycle
    return 0;
  }

}
