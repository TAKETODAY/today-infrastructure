/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.aop;

import cn.taketoday.aop.config.AopConfigUtils;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.EnableAspectJAutoProxy;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingClass;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Infra AOP support.
 * Equivalent to enabling {@link EnableAspectJAutoProxy @EnableAspectJAutoProxy}
 * in your configuration.
 * <p>
 * The configuration will not be activated if {@literal infra.aop.auto=false}. The
 * {@literal proxyTargetClass} attribute will be {@literal true}, by default, but can be
 * overridden by specifying {@literal infra.aop.proxy-target-class=false}.
 *
 * @author Dave Syer
 * @author Josh Long
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableAspectJAutoProxy
 * @since 4.0
 */
@DisableDIAutoConfiguration
@ConditionalOnProperty(prefix = "infra.aop", name = "auto", havingValue = "true", matchIfMissing = true)
public class AopAutoConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(org.aspectj.weaver.Advice.class)
  static class AspectJAutoProxyingConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = false)
    @ConditionalOnProperty(prefix = "infra.aop", name = "proxy-target-class", havingValue = "false")
    static class JdkDynamicAutoProxyConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @ConditionalOnProperty(
            prefix = "infra.aop", name = "proxy-target-class", havingValue = "true", matchIfMissing = true)
    static class CglibAutoProxyConfiguration {

    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingClass("org.aspectj.weaver.Advice")
  @ConditionalOnProperty(
          prefix = "infra.aop", name = "proxy-target-class", havingValue = "true", matchIfMissing = true)
  static class ClassProxyingConfiguration {

    @Component
    static BeanFactoryPostProcessor forceAutoProxyCreatorToUseClassProxying() {
      return (beanFactory) -> {
        if (beanFactory instanceof BeanDefinitionRegistry registry) {
          AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
          AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
        }
      };
    }

  }

}
