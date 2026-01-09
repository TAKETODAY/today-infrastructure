/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app;

import infra.context.ApplicationContext;

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
