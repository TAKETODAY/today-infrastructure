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

package infra.context.annotation;

import infra.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import infra.aop.config.AopConfigUtils;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.BootstrapContext;
import infra.core.annotation.AnnotationAttributes;
import infra.core.type.AnnotationMetadata;

/**
 * Registers an {@link AnnotationAwareAspectJAutoProxyCreator
 * AnnotationAwareAspectJAutoProxyCreator} against the current {@link BeanDefinitionRegistry}
 * as appropriate based on a given @{@link EnableAspectJAutoProxy} annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableAspectJAutoProxy
 * @since 4.0 2022/3/9 21:52
 */
class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

  /**
   * Register, escalate, and configure the AspectJ auto proxy creator based on the value
   * of the @{@link EnableAspectJAutoProxy#proxyTargetClass()} attribute on the importing
   * {@code @Configuration} class.
   */
  @Override
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
    AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(context.getRegistry());

    AnnotationAttributes enableAspectJAutoProxy =
            AnnotationAttributes.fromMetadata(importMetadata, EnableAspectJAutoProxy.class);
    if (enableAspectJAutoProxy != null) {
      if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
        AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(context.getRegistry());
      }
      if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
        AopConfigUtils.forceAutoProxyCreatorToExposeProxy(context.getRegistry());
      }
    }
  }

}
