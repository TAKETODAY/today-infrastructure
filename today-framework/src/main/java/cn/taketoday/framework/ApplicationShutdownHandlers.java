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

package cn.taketoday.framework;

import cn.taketoday.context.ApplicationContext;

/**
 * Interface that can be used to add or remove code that should run when the JVM is
 * shutdown. Shutdown handlers are similar to JVM {@link Runtime#addShutdownHook(Thread)
 * shutdown hooks} except that they run sequentially rather than concurrently.
 * <p>
 * Shutdown handlers are guaranteed to be called only after registered
 * {@link ApplicationContext} instances have been closed and are no longer active.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Application#getShutdownHandlers()
 * @see Application#setRegisterShutdownHook(boolean)
 * @since 4.0 2022/3/30 11:32
 */
public interface ApplicationShutdownHandlers {

  /**
   * Add an action to the handlers that will be run when the JVM exits.
   *
   * @param action the action to add
   */
  void add(Runnable action);

  /**
   * Remove a previously added an action so that it no longer runs when the JVM exits.
   *
   * @param action the action to remove
   */
  void remove(Runnable action);

}
