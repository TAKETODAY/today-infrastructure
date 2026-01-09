/*
 * Copyright 2012-present the original author or authors.
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

package infra.annotation.config.aop;

import infra.aop.config.AopConfigUtils;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.annotation.Configuration;
import infra.context.annotation.EnableAspectJAutoProxy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBooleanProperty;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingClass;
import infra.stereotype.Component;

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
@ConditionalOnBooleanProperty(name = "infra.aop.auto", matchIfMissing = true)
public class AopAutoConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(org.aspectj.weaver.Advice.class)
  static class AspectJAutoProxyingConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = false)
    @ConditionalOnBooleanProperty(name = "infra.aop.proxy-target-class", havingValue = false)
    static class JdkDynamicAutoProxyConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @ConditionalOnBooleanProperty(name = "infra.aop.proxy-target-class", matchIfMissing = true)
    static class CglibAutoProxyConfiguration {

    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingClass("org.aspectj.weaver.Advice")
  @ConditionalOnBooleanProperty(name = "infra.aop.proxy-target-class", matchIfMissing = true)
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
