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

package infra.util.concurrent;

import infra.lang.TodayStrategies;

/**
 * A factory interface for creating instances of {@link Scheduler}. This interface
 * provides a standard way to instantiate schedulers, allowing for flexible and
 * customizable task execution strategies.
 *
 * <p>The {@code SchedulerFactory} is typically used to decouple the creation of
 * scheduler instances from their implementation. This enables applications to
 * dynamically choose or configure scheduler implementations based on runtime
 * conditions or configuration.
 *
 * <p><b>Usage Examples:</b>
 *
 * <p>1. Implementing a custom {@code SchedulerFactory}:
 * <pre>{@code
 * static class ExecutorFactory implements SchedulerFactory {
 *
 *   @Override
 *   public Scheduler create() {
 *     return new MyExecutor();
 *   }
 * }
 * }</pre>
 *
 * <p>2. Using the factory to create a scheduler:
 * <pre>{@code
 * SchedulerFactory factory = new ExecutorFactory();
 * Scheduler scheduler = factory.create();
 * scheduler.execute(() -> System.out.println("Task executed"));
 * }</pre>
 *
 * <p><b>Note:</b> The {@link Scheduler#lookup()} method can be used as an alternative
 * to directly using a {@code SchedulerFactory}. It provides a built-in mechanism to
 * locate and create scheduler instances based on predefined strategies.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Scheduler
 * @see TodayStrategies
 * @since 4.0 2024/2/28 13:24
 */
public interface SchedulerFactory {

  /**
   * Creates and returns a new instance of {@link Scheduler}. This method is typically
   * implemented by a factory to provide a customizable or strategy-based approach to
   * scheduler creation.
   *
   * <p><b>Usage Examples:</b>
   *
   * <p>1. Implementing a custom factory to create a scheduler:
   * <pre>{@code
   * static class CustomSchedulerFactory implements SchedulerFactory {
   *
   *   @Override
   *   public Scheduler create() {
   *     return new MyCustomScheduler();
   *   }
   * }
   * }</pre>
   *
   * <p>2. Using the factory to create and execute tasks with a scheduler:
   * <pre>{@code
   * SchedulerFactory factory = new CustomSchedulerFactory();
   * Scheduler scheduler = factory.create();
   * scheduler.execute(() -> System.out.println("Task executed"));
   * }</pre>
   *
   * <p><b>Note:</b> The behavior of the returned {@code Scheduler} depends on the
   * implementation provided by the factory. Ensure that the factory is properly
   * configured to meet the application's scheduling requirements.
   *
   * @return a new instance of {@link Scheduler}, which can be used to execute tasks
   * asynchronously or after a specified delay
   * @see Scheduler
   * @see Scheduler#lookup()
   */
  Scheduler create();

}
