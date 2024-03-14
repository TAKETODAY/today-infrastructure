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

package cn.taketoday.annotation.config.web.embedded;

import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.lang.Nullable;

/**
 * Creates a {@link ThreadPool} for Jetty, applying
 * {@link ServerProperties.Jetty.Threads
 * ServerProperties.Jetty.Threads Jetty thread properties}.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class JettyThreadPool {

  private JettyThreadPool() { }

  static QueuedThreadPool create(ServerProperties.Jetty.Threads properties) {
    BlockingQueue<Runnable> queue = determineBlockingQueue(properties.maxQueueCapacity);
    int maxThreadCount = (properties.max > 0) ? properties.max : 200;
    int minThreadCount = (properties.min > 0) ? properties.min : 8;
    int threadIdleTimeout = (properties.idleTimeout != null)
            ? (int) properties.idleTimeout.toMillis() : 60000;
    return new QueuedThreadPool(maxThreadCount, minThreadCount, threadIdleTimeout, queue);
  }

  @Nullable
  private static BlockingQueue<Runnable> determineBlockingQueue(@Nullable Integer maxQueueCapacity) {
    if (maxQueueCapacity == null) {
      return null;
    }
    if (maxQueueCapacity == 0) {
      return new SynchronousQueue<>();
    }
    return new BlockingArrayQueue<>(maxQueueCapacity);
  }

}
