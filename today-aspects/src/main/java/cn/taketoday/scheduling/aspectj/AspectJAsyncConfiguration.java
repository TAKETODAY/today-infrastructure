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

package cn.taketoday.scheduling.aspectj;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.scheduling.annotation.AbstractAsyncConfiguration;
import cn.taketoday.scheduling.annotation.EnableAsync;
import cn.taketoday.scheduling.config.TaskManagementConfigUtils;

/**
 * {@code @Configuration} class that registers the Spring infrastructure beans necessary
 * to enable AspectJ-based asynchronous method execution.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @see EnableAsync
 * @see cn.taketoday.scheduling.annotation.AsyncConfigurationSelector
 * @see cn.taketoday.scheduling.annotation.ProxyAsyncConfiguration
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJAsyncConfiguration extends AbstractAsyncConfiguration {

  @Bean(name = TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public AnnotationAsyncExecutionAspect asyncAdvisor() {
    AnnotationAsyncExecutionAspect asyncAspect = AnnotationAsyncExecutionAspect.aspectOf();
    asyncAspect.configure(this.executor, this.exceptionHandler);
    return asyncAspect;
  }

}
