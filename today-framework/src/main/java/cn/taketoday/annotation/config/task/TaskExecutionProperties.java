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

package cn.taketoday.annotation.config.task;

import java.time.Duration;

import cn.taketoday.context.properties.ConfigurationProperties;

/**
 * Configuration properties for task execution.
 *
 * @author Stephane Nicoll
 * @author Filip Hrisafov
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties("infra.task.execution")
public class TaskExecutionProperties {

  private final Pool pool = new Pool();

  private final Shutdown shutdown = new Shutdown();

  /**
   * Prefix to use for the names of newly created threads.
   */
  private String threadNamePrefix = "task-";

  public Pool getPool() {
    return this.pool;
  }

  public Shutdown getShutdown() {
    return this.shutdown;
  }

  public String getThreadNamePrefix() {
    return this.threadNamePrefix;
  }

  public void setThreadNamePrefix(String threadNamePrefix) {
    this.threadNamePrefix = threadNamePrefix;
  }

  public static class Pool {

    /**
     * Queue capacity. An unbounded capacity does not increase the pool and therefore
     * ignores the "max-size" property.
     */
    private int queueCapacity = Integer.MAX_VALUE;

    /**
     * Core number of threads.
     */
    private int coreSize = 8;

    /**
     * Maximum allowed number of threads. If tasks are filling up the queue, the pool
     * can expand up to that size to accommodate the load. Ignored if the queue is
     * unbounded.
     */
    private int maxSize = Integer.MAX_VALUE;

    /**
     * Whether core threads are allowed to time out. This enables dynamic growing and
     * shrinking of the pool.
     */
    private boolean allowCoreThreadTimeout = true;

    /**
     * Time limit for which threads may remain idle before being terminated.
     */
    private Duration keepAlive = Duration.ofSeconds(60);

    public int getQueueCapacity() {
      return this.queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
      this.queueCapacity = queueCapacity;
    }

    public int getCoreSize() {
      return this.coreSize;
    }

    public void setCoreSize(int coreSize) {
      this.coreSize = coreSize;
    }

    public int getMaxSize() {
      return this.maxSize;
    }

    public void setMaxSize(int maxSize) {
      this.maxSize = maxSize;
    }

    public boolean isAllowCoreThreadTimeout() {
      return this.allowCoreThreadTimeout;
    }

    public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
      this.allowCoreThreadTimeout = allowCoreThreadTimeout;
    }

    public Duration getKeepAlive() {
      return this.keepAlive;
    }

    public void setKeepAlive(Duration keepAlive) {
      this.keepAlive = keepAlive;
    }

  }

  public static class Shutdown {

    /**
     * Whether the executor should wait for scheduled tasks to complete on shutdown.
     */
    private boolean awaitTermination;

    /**
     * Maximum time the executor should wait for remaining tasks to complete.
     */
    private Duration awaitTerminationPeriod;

    public boolean isAwaitTermination() {
      return this.awaitTermination;
    }

    public void setAwaitTermination(boolean awaitTermination) {
      this.awaitTermination = awaitTermination;
    }

    public Duration getAwaitTerminationPeriod() {
      return this.awaitTerminationPeriod;
    }

    public void setAwaitTerminationPeriod(Duration awaitTerminationPeriod) {
      this.awaitTerminationPeriod = awaitTerminationPeriod;
    }

  }

}
