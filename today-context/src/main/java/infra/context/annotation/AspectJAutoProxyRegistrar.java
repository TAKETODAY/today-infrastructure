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

package infra.context.annotation;

import infra.aop.config.AopConfigUtils;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
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
