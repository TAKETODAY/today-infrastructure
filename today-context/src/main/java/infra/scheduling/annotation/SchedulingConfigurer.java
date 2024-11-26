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

package infra.scheduling.annotation;

import infra.context.annotation.Configuration;
import infra.scheduling.TaskScheduler;
import infra.scheduling.Trigger;
import infra.scheduling.config.ScheduledTaskRegistrar;
import infra.scheduling.config.Task;

/**
 * Optional interface to be implemented by {@link
 * Configuration @Configuration} classes annotated
 * with {@link EnableScheduling @EnableScheduling}. Typically used for setting a specific
 * {@link TaskScheduler TaskScheduler} bean to be used when
 * executing scheduled tasks or for registering scheduled tasks in a <em>programmatic</em>
 * fashion as opposed to the <em>declarative</em> approach of using the
 * {@link Scheduled @Scheduled} annotation. For example, this may be necessary
 * when implementing {@link Trigger Trigger}-based
 * tasks, which are not supported by the {@code @Scheduled} annotation.
 *
 * <p>See {@link EnableScheduling @EnableScheduling} for detailed usage examples.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableScheduling
 * @see ScheduledTaskRegistrar
 * @since 4.0
 */
@FunctionalInterface
public interface SchedulingConfigurer {

  /**
   * Callback allowing a {@link TaskScheduler}
   * and specific {@link Task} instances
   * to be registered against the given the {@link ScheduledTaskRegistrar}.
   *
   * @param taskRegistrar the registrar to be configured
   */
  void configureTasks(ScheduledTaskRegistrar taskRegistrar);

}
