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

package cn.taketoday.core.task;

import java.util.concurrent.ThreadFactory;

/**
 * Internal delegate for virtual thread handling on JDK 21.
 * This is a dummy version for reachability on JDK less than 21.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see VirtualThreadTaskExecutor
 * @since 4.0
 */
class VirtualThreadDelegate {

  public VirtualThreadDelegate() {
    throw new UnsupportedOperationException("Virtual threads not supported on JDK <21");
  }

  public ThreadFactory virtualThreadFactory() {
    throw new UnsupportedOperationException();
  }

  public ThreadFactory virtualThreadFactory(String threadNamePrefix) {
    throw new UnsupportedOperationException();
  }

  public Thread startVirtualThread(String name, Runnable task) {
    throw new UnsupportedOperationException();
  }

}
