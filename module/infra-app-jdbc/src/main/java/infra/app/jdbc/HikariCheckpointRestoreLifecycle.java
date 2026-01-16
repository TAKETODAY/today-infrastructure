/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.jdbc;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import com.zaxxer.hikari.pool.HikariPool;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import javax.sql.DataSource;

import infra.context.ConfigurableApplicationContext;
import infra.context.Lifecycle;
import infra.jdbc.config.DataSourceUnwrapper;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ReflectionUtils;

/**
 * {@link Lifecycle} for a {@link HikariDataSource} allowing it to participate in
 * checkpoint-restore. When {@link #stop() stopped}, and the data source
 * {@link HikariDataSource#isAllowPoolSuspension() allows it}, its pool is suspended,
 * blocking any attempts to borrow connections. Open and idle connections are then
 * evicted. When subsequently {@link #start() started}, the pool is
 * {@link HikariPoolMXBean#resumePool() resumed} if necessary.
 *
 * @author Christoph Strobl
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HikariCheckpointRestoreLifecycle implements Lifecycle {

  private static final Logger logger = LoggerFactory.getLogger(HikariCheckpointRestoreLifecycle.class);

  private static final Field CLOSE_CONNECTION_EXECUTOR;

  static {
    Field closeConnectionExecutor = ReflectionUtils.findField(HikariPool.class, "closeConnectionExecutor");
    Assert.notNull(closeConnectionExecutor, "Unable to locate closeConnectionExecutor for HikariPool");
    Assert.isAssignable(ThreadPoolExecutor.class, closeConnectionExecutor.getType(),
            "Expected ThreadPoolExecutor for closeConnectionExecutor but found %s".formatted(closeConnectionExecutor.getType()));
    ReflectionUtils.makeAccessible(closeConnectionExecutor);
    CLOSE_CONNECTION_EXECUTOR = closeConnectionExecutor;
  }

  private final Function<HikariPool, Boolean> hasOpenConnections;

  @Nullable
  private final HikariDataSource dataSource;

  @Nullable
  private final ConfigurableApplicationContext applicationContext;

  /**
   * Creates a new {@code HikariCheckpointRestoreLifecycle} that will allow the given
   * {@code dataSource} to participate in checkpoint-restore. The {@code dataSource} is
   * {@link DataSourceUnwrapper#unwrap unwrapped} to a {@link HikariDataSource}. If such
   * unwrapping is not possible, the lifecycle will have no effect.
   *
   * @param dataSource the checkpoint-restore participant
   * @param applicationContext the application context
   */
  public HikariCheckpointRestoreLifecycle(DataSource dataSource, @Nullable ConfigurableApplicationContext applicationContext) {
    this.dataSource = DataSourceUnwrapper.unwrap(dataSource, HikariConfigMXBean.class, HikariDataSource.class);
    this.applicationContext = applicationContext;
    this.hasOpenConnections = (pool) -> {
      ThreadPoolExecutor closeConnectionExecutor = (ThreadPoolExecutor) ReflectionUtils
              .getField(CLOSE_CONNECTION_EXECUTOR, pool);
      Assert.notNull(closeConnectionExecutor, "CloseConnectionExecutor was null");
      return closeConnectionExecutor.getActiveCount() > 0;
    };
  }

  @Override
  public void start() {
    if (this.dataSource == null || this.dataSource.isRunning()) {
      return;
    }
    Assert.state(!this.dataSource.isClosed(), "DataSource has been closed and cannot be restarted");
    if (this.dataSource.isAllowPoolSuspension()) {
      logger.info("Resuming Hikari pool");
      this.dataSource.getHikariPoolMXBean().resumePool();
    }
  }

  @Override
  public void stop() {
    if (this.dataSource == null || !this.dataSource.isRunning()) {
      return;
    }
    if (this.dataSource.isAllowPoolSuspension()) {
      logger.info("Suspending Hikari pool");
      this.dataSource.getHikariPoolMXBean().suspendPool();
    }
    else {
      if (this.applicationContext != null && !this.applicationContext.isClosed()) {
        logger.warn("{} is not configured to allow pool suspension. "
                + "This will cause problems when the application is check-pointed. "
                + "Please configure allow-pool-suspension to fix this!", dataSource);
      }
    }
    closeConnections(dataSource, Duration.ofMillis(this.dataSource.getConnectionTimeout() + 250));
  }

  private void closeConnections(HikariDataSource dataSource, Duration shutdownTimeout) {
    logger.info("Evicting Hikari connections");
    dataSource.getHikariPoolMXBean().softEvictConnections();
    logger.debug("Waiting {} seconds for Hikari connections to be closed", shutdownTimeout.toSeconds());
    CompletableFuture<Void> allConnectionsClosed = CompletableFuture.runAsync(
            () -> waitForConnectionsToClose(dataSource));
    try {
      allConnectionsClosed.get(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS);
      logger.debug("Hikari connections closed");
    }
    catch (InterruptedException ex) {
      logger.warn("Interrupted while waiting for connections to be closed", ex);
      Thread.currentThread().interrupt();
    }
    catch (TimeoutException ex) {
      logger.warn("Hikari connections could not be closed within {}", shutdownTimeout, ex);
    }
    catch (ExecutionException ex) {
      throw new IllegalStateException("Failed to close Hikari connections", ex);
    }
  }

  private void waitForConnectionsToClose(HikariDataSource dataSource) {
    while (this.hasOpenConnections.apply((HikariPool) dataSource.getHikariPoolMXBean())) {
      try {
        TimeUnit.MILLISECONDS.sleep(50);
      }
      catch (InterruptedException ex) {
        logger.error("Interrupted while waiting for datasource connections to be closed", ex);
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public boolean isRunning() {
    return this.dataSource != null && this.dataSource.isRunning();
  }

}
