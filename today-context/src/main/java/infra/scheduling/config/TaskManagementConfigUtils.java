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

/**
 * Configuration constants for internal sharing across subpackages.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class TaskManagementConfigUtils {

  /**
   * The bean name of the internally managed Scheduled annotation processor.
   */
  public static final String SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME =
          "infra.context.annotation.internalScheduledAnnotationProcessor";

  /**
   * The bean name of the internally managed Async annotation processor.
   */
  public static final String ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME =
          "infra.context.annotation.internalAsyncAnnotationProcessor";

  /**
   * The bean name of the internally managed AspectJ async execution aspect.
   */
  public static final String ASYNC_EXECUTION_ASPECT_BEAN_NAME =
          "infra.scheduling.config.internalAsyncExecutionAspect";

}
