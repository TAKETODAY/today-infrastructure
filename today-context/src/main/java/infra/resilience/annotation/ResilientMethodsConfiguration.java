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

package infra.resilience.annotation;

import org.jspecify.annotations.Nullable;

import infra.aop.framework.ProxyProcessorSupport;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.ImportAware;
import infra.context.annotation.Role;
import infra.core.annotation.AnnotationAttributes;
import infra.core.type.AnnotationMetadata;

/**
 * {@code @Configuration} class that registers the Spring infrastructure beans necessary
 * to enable proxy-based method invocations with retry and concurrency limit behavior.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EnableResilientMethods
 * @see ConcurrencyLimitBeanPostProcessor
 * @since 5.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ResilientMethodsConfiguration implements ImportAware {

  private @Nullable AnnotationAttributes enableResilientMethods;

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    this.enableResilientMethods = AnnotationAttributes.fromMap(
            importMetadata.getAnnotationAttributes(EnableResilientMethods.class.getName()));
  }

  @Bean(name = "infra.resilience.annotation.internalConcurrencyLimitProcessor")
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public ConcurrencyLimitBeanPostProcessor concurrencyLimitAdvisor() {
    ConcurrencyLimitBeanPostProcessor bpp = new ConcurrencyLimitBeanPostProcessor();
    configureProxySupport(bpp);
    return bpp;
  }

  private void configureProxySupport(ProxyProcessorSupport proxySupport) {
    if (this.enableResilientMethods != null) {
      if (this.enableResilientMethods.getBoolean("proxyTargetClass")) {
        proxySupport.setProxyTargetClass(true);
      }
      proxySupport.setOrder(this.enableResilientMethods.getNumber("order"));
    }
  }

}
