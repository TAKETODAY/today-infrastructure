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
package cn.taketoday.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aop.proxy.ProxyConfig;
import cn.taketoday.aop.proxy.ProxyCreator;
import cn.taketoday.aop.support.annotation.AspectAutoProxyCreator;
import cn.taketoday.aop.target.TargetSourceCreator;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.loader.BeanDefinitionImporter;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.annotation.AnnotationProvider;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.util.ObjectUtils;

/**
 * Enable Aspect Oriented Programming
 *
 * @author TODAY <br>
 * 2020-02-06 20:02
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Import(AutoProxyConfiguration.class)
public @interface EnableAspectAutoProxy {

  /**
   * Return whether the AOP proxy will expose the AOP proxy for
   * each invocation.
   */
  boolean exposeProxy() default false;

  /**
   * Return whether to proxy the target class directly as well as any interfaces.
   */
  boolean proxyTargetClass() default true;
}

@Configuration
class AutoProxyConfiguration implements BeanDefinitionImporter, AnnotationProvider<EnableAspectAutoProxy> {

  /**
   * ProxyCreator Bean
   *
   * @param sourceCreators Custom {@link TargetSourceCreator}s
   */
  @Component
  @ConditionalOnMissingBean(ProxyCreator.class)
  static AspectAutoProxyCreator aspectAutoProxyCreator(TargetSourceCreator[] sourceCreators) {
    AspectAutoProxyCreator proxyCreator = new AspectAutoProxyCreator();

    if (ObjectUtils.isNotEmpty(sourceCreators)) {
      proxyCreator.setTargetSourceCreators(sourceCreators);
    }
    return proxyCreator;
  }

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, DefinitionLoadingContext context) {
    BeanDefinition proxyCreatorDef = context.getRegistry().getBeanDefinition(ProxyCreator.class);
    Assert.state(proxyCreatorDef != null, "No ProxyCreator bean definition.");

    // check is a ProxyConfig? don't use BeanDefinition#getBeanClass()
    if (context.getBeanFactory().isTypeMatch(
            proxyCreatorDef.getName(), ProxyConfig.class)) {

      MergedAnnotation<EnableAspectAutoProxy> aspectAutoProxy = getMergedAnnotation(importMetadata);
      if (aspectAutoProxy != null) {
        proxyCreatorDef.addPropertyValue("exposeProxy", aspectAutoProxy.getBoolean("exposeProxy"));
        proxyCreatorDef.addPropertyValue("proxyTargetClass", aspectAutoProxy.getBoolean("proxyTargetClass"));
      }
    }

  }
}
