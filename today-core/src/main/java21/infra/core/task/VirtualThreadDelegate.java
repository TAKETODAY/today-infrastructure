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

package infra.core.task;

import java.util.concurrent.ThreadFactory;

/**
 * Internal delegate for virtual thread handling on JDK 21.
 * This is the actual version compiled against JDK 21.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see VirtualThreadTaskExecutor
 * @since 4.0
 */
class VirtualThreadDelegate {

  private final Thread.Builder threadBuilder = Thread.ofVirtual();

  public ThreadFactory virtualThreadFactory() {
    return this.threadBuilder.factory();
  }

  public ThreadFactory virtualThreadFactory(String threadNamePrefix) {
    return this.threadBuilder.name(threadNamePrefix, 0).factory();
  }

  public Thread newVirtualThread(String name, Runnable task) {
    return this.threadBuilder.name(name).unstarted(task);
  }

}