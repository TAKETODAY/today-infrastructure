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

package infra.scheduling.config;

import java.util.Set;

import infra.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

/**
 * Common interface for exposing locally scheduled tasks.
 *
 * @author Juergen Hoeller
 * @see ScheduledTaskRegistrar
 * @see ScheduledAnnotationBeanPostProcessor
 * @since 4.0
 */
public interface ScheduledTaskHolder {

  /**
   * Return an overview of the tasks that have been scheduled by this instance.
   */
  Set<ScheduledTask> getScheduledTasks();

}
