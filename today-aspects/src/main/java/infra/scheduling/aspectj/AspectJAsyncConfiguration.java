/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package infra.scheduling.aspectj;

import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.Configuration;
import infra.context.annotation.Role;
import infra.scheduling.annotation.AbstractAsyncConfiguration;
import infra.scheduling.annotation.EnableAsync;
import infra.scheduling.config.TaskManagementConfigUtils;
import infra.stereotype.Component;
import infra.scheduling.annotation.AsyncConfigurationSelector;
import infra.scheduling.annotation.ProxyAsyncConfiguration;

/**
 * {@code @Configuration} class that registers the infrastructure beans necessary
 * to enable AspectJ-based asynchronous method execution.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @see EnableAsync
 * @see AsyncConfigurationSelector
 * @see ProxyAsyncConfiguration
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJAsyncConfiguration extends AbstractAsyncConfiguration {

  @DisableDependencyInjection
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Component(name = TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME)
  public AnnotationAsyncExecutionAspect asyncAdvisor() {
    AnnotationAsyncExecutionAspect asyncAspect = AnnotationAsyncExecutionAspect.aspectOf();
    asyncAspect.configure(this.executor, this.exceptionHandler);
    return asyncAspect;
  }

}
