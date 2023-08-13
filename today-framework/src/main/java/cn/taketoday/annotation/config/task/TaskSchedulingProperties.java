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

package cn.taketoday.annotation.config.task;

import java.time.Duration;

import cn.taketoday.context.properties.ConfigurationProperties;

/**
 * Configuration properties for task scheduling.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties("infra.task.scheduling")
public class TaskSchedulingProperties {

  private final Pool pool = new Pool();

  private final Simple simple = new Simple();

  private final Shutdown shutdown = new Shutdown();

  /**
   * Prefix to use for the names of newly created threads.
   */
  private String threadNamePrefix = "scheduling-";

  public Pool getPool() {
    return this.pool;
  }

  public Simple getSimple() {
    return this.simple;
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
     * Maximum allowed number of threads.
     */
    private int size = 1;

    public int getSize() {
      return this.size;
    }

    public void setSize(int size) {
      this.size = size;
    }

  }

  public static class Simple {

    /**
     * Set the maximum number of parallel accesses allowed. -1 indicates no
     * concurrency limit at all.
     */
    private Integer concurrencyLimit;

    public Integer getConcurrencyLimit() {
      return this.concurrencyLimit;
    }

    public void setConcurrencyLimit(Integer concurrencyLimit) {
      this.concurrencyLimit = concurrencyLimit;
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
