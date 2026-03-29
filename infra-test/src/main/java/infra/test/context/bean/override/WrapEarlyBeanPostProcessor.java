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

package infra.test.context.bean.override;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.beans.BeansException;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import infra.core.Ordered;
import infra.core.PriorityOrdered;
import infra.util.StringUtils;

/**
 * {@link SmartInstantiationAwareBeanPostProcessor} implementation that wraps
 * beans in order to support the {@link BeanOverrideStrategy#WRAP WRAP} bean
 * override strategy.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @since 5.0
 */
class WrapEarlyBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor, InitializationBeanPostProcessor, PriorityOrdered {

  private final Map<String, Object> earlyReferences = new ConcurrentHashMap<>(16);

  private final BeanOverrideRegistry beanOverrideRegistry;

  WrapEarlyBeanPostProcessor(BeanOverrideRegistry beanOverrideRegistry) {
    this.beanOverrideRegistry = beanOverrideRegistry;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
    if (bean instanceof FactoryBean) {
      return bean;
    }
    this.earlyReferences.put(getCacheKey(bean, beanName), bean);
    return this.beanOverrideRegistry.wrapBeanIfNecessary(bean, beanName);
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof FactoryBean) {
      return bean;
    }
    if (this.earlyReferences.remove(getCacheKey(bean, beanName)) != bean) {
      return this.beanOverrideRegistry.wrapBeanIfNecessary(bean, beanName);
    }
    return bean;
  }

  private String getCacheKey(Object bean, String beanName) {
    return StringUtils.isNotEmpty(beanName) ? beanName : bean.getClass().getName();
  }

}
