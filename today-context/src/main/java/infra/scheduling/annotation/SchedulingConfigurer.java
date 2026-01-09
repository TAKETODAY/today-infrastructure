/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
