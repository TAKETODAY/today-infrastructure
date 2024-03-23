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

package cn.taketoday.scheduling.concurrent;

import java.util.concurrent.ThreadFactory;

import cn.taketoday.util.CustomizableThreadCreator;

/**
 * Implementation of the {@link ThreadFactory} interface,
 * allowing for customizing the created threads (name, priority, etc).
 *
 * <p>See the base class {@link cn.taketoday.util.CustomizableThreadCreator}
 * for details on the available configuration options.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setThreadNamePrefix
 * @see #setThreadPriority
 * @since 4.0
 */
@SuppressWarnings("serial")
public class CustomizableThreadFactory extends CustomizableThreadCreator implements ThreadFactory {

  /**
   * Create a new CustomizableThreadFactory with default thread name prefix.
   */
  public CustomizableThreadFactory() {
    super();
  }

  /**
   * Create a new CustomizableThreadFactory with the given thread name prefix.
   *
   * @param threadNamePrefix the prefix to use for the names of newly created threads
   */
  public CustomizableThreadFactory(String threadNamePrefix) {
    super(threadNamePrefix);
  }

  @Override
  public Thread newThread(Runnable runnable) {
    return createThread(runnable);
  }

}
