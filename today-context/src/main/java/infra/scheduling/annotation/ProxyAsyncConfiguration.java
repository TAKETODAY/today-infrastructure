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

package infra.scheduling.annotation;

import java.lang.annotation.Annotation;

import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.Configuration;
import infra.context.annotation.Role;
import infra.lang.Assert;
import infra.scheduling.config.TaskManagementConfigUtils;
import infra.stereotype.Component;

/**
 * {@code @Configuration} class that registers the Framework infrastructure beans necessary
 * to enable proxy-based asynchronous method execution.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableAsync
 * @see AsyncConfigurationSelector
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyAsyncConfiguration extends AbstractAsyncConfiguration {

  @SuppressWarnings("NullAway")
  @DisableDependencyInjection
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Component(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME)
  public AsyncAnnotationBeanPostProcessor asyncAdvisor() {
    Assert.notNull(this.enableAsync, "@EnableAsync annotation metadata was not injected");
    AsyncAnnotationBeanPostProcessor bpp = new AsyncAnnotationBeanPostProcessor();
    bpp.configure(this.executor, this.exceptionHandler);
    Class<? extends Annotation> customAsyncAnnotation = this.enableAsync.getClass("annotation");
    if (customAsyncAnnotation != enableAsync.getDefaultValue("annotation")) {
      bpp.setAsyncAnnotationType(customAsyncAnnotation);
    }
    bpp.setProxyTargetClass(this.enableAsync.getBoolean("proxyTargetClass"));
    bpp.setOrder(this.enableAsync.getInt("order"));
    return bpp;
  }

}
