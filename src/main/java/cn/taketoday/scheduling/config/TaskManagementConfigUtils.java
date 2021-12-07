/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.scheduling.config;

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
          "cn.taketoday.context.annotation.internalScheduledAnnotationProcessor";

  /**
   * The bean name of the internally managed Async annotation processor.
   */
  public static final String ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME =
          "cn.taketoday.context.annotation.internalAsyncAnnotationProcessor";

  /**
   * The bean name of the internally managed AspectJ async execution aspect.
   */
  public static final String ASYNC_EXECUTION_ASPECT_BEAN_NAME =
          "cn.taketoday.scheduling.config.internalAsyncExecutionAspect";

}
