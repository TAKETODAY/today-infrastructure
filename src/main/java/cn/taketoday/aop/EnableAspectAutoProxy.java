/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionImporter;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;

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

class AutoProxyConfiguration implements BeanDefinitionImporter {

  /**
   * ProxyCreator Bean
   *
   * @param sourceCreators
   *         Custom {@link TargetSourceCreator}s
   */
  @MissingBean(type = ProxyCreator.class)
  static AspectAutoProxyCreator aspectAutoProxyCreator(TargetSourceCreator[] sourceCreators) {
    final AspectAutoProxyCreator proxyCreator = new AspectAutoProxyCreator();

    if (ObjectUtils.isNotEmpty(sourceCreators)) {
      proxyCreator.setTargetSourceCreators(sourceCreators);
    }
    return proxyCreator;
  }

  @Override
  public void registerBeanDefinitions(BeanDefinition annotatedMetadata, BeanDefinitionRegistry registry) {
    final BeanDefinition proxyCreatorDef = registry.getBeanDefinition(ProxyCreator.class);
    Assert.state(proxyCreatorDef != null, "No ProxyCreator bean definition.");

    if (ProxyConfig.class.isAssignableFrom(proxyCreatorDef.getBeanClass())) {
      final AnnotationAttributes aspectAutoProxy = ClassUtils.getAnnotationAttributes(EnableAspectAutoProxy.class, annotatedMetadata);
      if (aspectAutoProxy != null) {
        proxyCreatorDef.addPropertyValue("exposeProxy", aspectAutoProxy.getBoolean("exposeProxy"));
        proxyCreatorDef.addPropertyValue("proxyTargetClass", aspectAutoProxy.getBoolean("proxyTargetClass"));
      }
    }

  }
}
