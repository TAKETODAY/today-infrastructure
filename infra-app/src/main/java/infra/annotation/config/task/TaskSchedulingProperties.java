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

package infra.annotation.config.task;

import org.jspecify.annotations.Nullable;

import java.time.Duration;

import infra.context.properties.ConfigurationProperties;

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
     * Maximum allowed number of threads. Doesn't have an effect if virtual threads
     * are enabled.
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
    @Nullable
    private Integer concurrencyLimit;

    @Nullable
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
    @Nullable
    private Duration awaitTerminationPeriod;

    public boolean isAwaitTermination() {
      return this.awaitTermination;
    }

    public void setAwaitTermination(boolean awaitTermination) {
      this.awaitTermination = awaitTermination;
    }

    @Nullable
    public Duration getAwaitTerminationPeriod() {
      return this.awaitTerminationPeriod;
    }

    public void setAwaitTerminationPeriod(Duration awaitTerminationPeriod) {
      this.awaitTerminationPeriod = awaitTerminationPeriod;
    }

  }

}
