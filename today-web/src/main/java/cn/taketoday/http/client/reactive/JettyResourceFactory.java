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

import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.ProcessorUtils;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Factory to manage Jetty resources, i.e. {@link Executor}, {@link ByteBufferPool} and
 * {@link Scheduler}, within the lifecycle of a {@code ApplicationContext}.
 *
 * <p>This factory implements {@link InitializingBean} and {@link DisposableBean}
 * and is expected typically to be declared as a managed bean.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JettyResourceFactory implements InitializingBean, DisposableBean {

  @Nullable
  private Executor executor;

  @Nullable
  private ByteBufferPool byteBufferPool;

  @Nullable
  private Scheduler scheduler;

  private String threadPrefix = "jetty-http";

  /**
   * Configure the {@link Executor} to use.
   * <p>By default, initialized with a {@link QueuedThreadPool}.
   *
   * @param executor the executor to use
   */
  public void setExecutor(@Nullable Executor executor) {
    this.executor = executor;
  }

  /**
   * Configure the {@link ByteBufferPool} to use.
   * <p>By default, initialized with a {@link ArrayByteBufferPool}.
   *
   * @param byteBufferPool the {@link ByteBuffer} pool to use
   */
  public void setByteBufferPool(@Nullable ByteBufferPool byteBufferPool) {
    this.byteBufferPool = byteBufferPool;
  }

  /**
   * Configure the {@link Scheduler} to use.
   * <p>By default, initialized with a {@link ScheduledExecutorScheduler}.
   *
   * @param scheduler the {@link Scheduler} to use
   */
  public void setScheduler(@Nullable Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  /**
   * Configure the thread prefix to initialize {@link QueuedThreadPool} executor with. This
   * is used only when a {@link Executor} instance isn't
   * {@link #setExecutor(Executor) provided}.
   * <p>By default set to "jetty-http".
   *
   * @param threadPrefix the thread prefix to use
   */
  public void setThreadPrefix(String threadPrefix) {
    Assert.notNull(threadPrefix, "Thread prefix is required");
    this.threadPrefix = threadPrefix;
  }

  /**
   * Return the configured {@link Executor}.
   */
  @Nullable
  public Executor getExecutor() {
    return this.executor;
  }

  /**
   * Return the configured {@link ByteBufferPool}.
   */
  @Nullable
  public ByteBufferPool getByteBufferPool() {
    return this.byteBufferPool;
  }

  /**
   * Return the configured {@link Scheduler}.
   */
  @Nullable
  public Scheduler getScheduler() {
    return this.scheduler;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    String name = this.threadPrefix + "@" + Integer.toHexString(hashCode());
    if (this.executor == null) {
      QueuedThreadPool threadPool = new QueuedThreadPool();
      threadPool.setName(name);
      this.executor = threadPool;
    }
    if (this.byteBufferPool == null) {
      this.byteBufferPool = new ArrayByteBufferPool(0, 2048, 65536, // from HttpClient:202
              this.executor instanceof ThreadPool.SizedThreadPool sizedThreadPool ?
              sizedThreadPool.getMaxThreads() / 2 :
              ProcessorUtils.availableProcessors() * 2);
    }
    if (scheduler == null) {
      this.scheduler = new ScheduledExecutorScheduler(name + "-scheduler", false);
    }

    if (executor instanceof LifeCycle lifeCycle) {
      lifeCycle.start();
    }
    scheduler.start();
  }

  @Override
  public void destroy() throws Exception {
    try {
      if (this.executor instanceof LifeCycle lifeCycle) {
        lifeCycle.stop();
      }
    }
    catch (Throwable ex) {
      // ignore
    }
    try {
      if (this.scheduler != null) {
        this.scheduler.stop();
      }
    }
    catch (Throwable ex) {
      // ignore
    }
  }

}
