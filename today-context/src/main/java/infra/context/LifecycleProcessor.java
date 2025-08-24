/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context;

/**
 * Strategy interface for processing Lifecycle beans within the ApplicationContext.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface LifecycleProcessor extends Lifecycle {

  /**
   * Notification of context refresh for auto-starting components.
   *
   * @see ConfigurableApplicationContext#refresh()
   */
  default void onRefresh() {
    start();
  }

  /**
   * Notification of context close phase for auto-stopping components
   * before destruction.
   *
   * @see ConfigurableApplicationContext#close()
   */
  default void onClose() {
    stop();
  }

  /**
   * Notification of context restart for auto-stopping and subsequently
   * auto-starting components.
   *
   * @see ConfigurableApplicationContext#restart()
   * @since 5.0
   */
  default void onRestart() {
    stop();
    start();
  }

  /**
   * Notification of context pause for auto-stopping components.
   *
   * @see ConfigurableApplicationContext#pause()
   * @since 5.0
   */
  default void onPause() {
    stop();
  }

}
