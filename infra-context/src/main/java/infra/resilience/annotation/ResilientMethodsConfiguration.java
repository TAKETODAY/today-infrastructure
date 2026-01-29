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
 * {@code @Configuration} class that registers the Infra infrastructure beans necessary
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
