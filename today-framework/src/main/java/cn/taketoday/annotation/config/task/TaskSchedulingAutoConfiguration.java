/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.task;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.framework.LazyInitializationExcludeFilter;
import cn.taketoday.scheduling.TaskScheduler;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskScheduler;
import cn.taketoday.scheduling.config.TaskManagementConfigUtils;
import cn.taketoday.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskScheduler}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConditionalOnClass(ThreadPoolTaskScheduler.class)
@AutoConfiguration(after = TaskExecutionAutoConfiguration.class)
@EnableConfigurationProperties(TaskSchedulingProperties.class)
@Import({ TaskSchedulingConfigurations.ThreadPoolTaskSchedulerBuilderConfiguration.class,
    TaskSchedulingConfigurations.TaskSchedulerBuilderConfiguration.class,
    TaskSchedulingConfigurations.SimpleAsyncTaskSchedulerBuilderConfiguration.class,
    TaskSchedulingConfigurations.TaskSchedulerConfiguration.class })
public class TaskSchedulingAutoConfiguration {

  @Component
  @ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
  public static LazyInitializationExcludeFilter scheduledBeanLazyInitializationExcludeFilter() {
    return new ScheduledBeanLazyInitializationExcludeFilter();
  }

}
