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
import cn.taketoday.aop.support.annotation.AspectAutoProxyCreator;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.context.loader.ImportBeanDefinitionRegistrar;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;

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
   * The bean name of the internally managed auto-proxy creator.
   */
  String AUTO_PROXY_CREATOR_BEAN_NAME = "cn.taketoday.aop.internalAutoProxyCreator";

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

class AutoProxyConfiguration implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(
          AnnotationMetadata importMetadata, DefinitionLoadingContext context) {
    BeanDefinitionRegistry registry = context.getRegistry();

    MergedAnnotation<EnableAspectAutoProxy> aspectAutoProxy
            = importMetadata.getAnnotation(EnableAspectAutoProxy.class);

    BeanDefinition proxyCreator = registry.getBeanDefinition(EnableAspectAutoProxy.AUTO_PROXY_CREATOR_BEAN_NAME);
    if (proxyCreator == null) {
      proxyCreator = new BeanDefinition(
              EnableAspectAutoProxy.AUTO_PROXY_CREATOR_BEAN_NAME, AspectAutoProxyCreator.class);

      if (aspectAutoProxy.isPresent()) {
        proxyCreator.addPropertyValue("exposeProxy", aspectAutoProxy.getBoolean("exposeProxy"));
        proxyCreator.addPropertyValue("proxyTargetClass", aspectAutoProxy.getBoolean("proxyTargetClass"));
      }

      registry.registerBeanDefinition(proxyCreator);
    }
    else {
      // check is a ProxyConfig? don't use BeanDefinition#getBeanClass()
      if (context.getBeanFactory().isTypeMatch(
              proxyCreator.getName(), ProxyConfig.class)) {

        if (aspectAutoProxy.isPresent()) {
          proxyCreator.addPropertyValue("exposeProxy", aspectAutoProxy.getBoolean("exposeProxy"));
          proxyCreator.addPropertyValue("proxyTargetClass", aspectAutoProxy.getBoolean("proxyTargetClass"));
        }
      }
    }
  }

}
