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

package cn.taketoday.framework.jdbc;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import com.zaxxer.hikari.pool.HikariPool;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import javax.sql.DataSource;

import cn.taketoday.context.Lifecycle;
import cn.taketoday.jdbc.config.DataSourceUnwrapper;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;

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

  private final HikariDataSource dataSource;

  /**
   * Creates a new {@code HikariCheckpointRestoreLifecycle} that will allow the given
   * {@code dataSource} to participate in checkpoint-restore. The {@code dataSource} is
   * {@link DataSourceUnwrapper#unwrap unwrapped} to a {@link HikariDataSource}. If such
   * unwrapping is not possible, the lifecycle will have no effect.
   *
   * @param dataSource the checkpoint-restore participant
   */
  public HikariCheckpointRestoreLifecycle(DataSource dataSource) {
    this.dataSource = DataSourceUnwrapper.unwrap(dataSource, HikariConfigMXBean.class, HikariDataSource.class);
    this.hasOpenConnections = pool -> {
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
    closeConnections(Duration.ofMillis(this.dataSource.getConnectionTimeout() + 250));
  }

  private void closeConnections(Duration shutdownTimeout) {
    logger.info("Evicting Hikari connections");
    this.dataSource.getHikariPoolMXBean().softEvictConnections();
    logger.debug("Waiting for Hikari connections to be closed");
    CompletableFuture<Void> allConnectionsClosed = CompletableFuture.runAsync(this::waitForConnectionsToClose);
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

  private void waitForConnectionsToClose() {
    while (this.hasOpenConnections.apply((HikariPool) this.dataSource.getHikariPoolMXBean())) {
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
