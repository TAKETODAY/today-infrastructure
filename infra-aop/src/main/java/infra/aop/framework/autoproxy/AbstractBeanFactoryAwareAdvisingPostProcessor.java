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

package infra.aop.framework.autoproxy;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

import infra.aop.framework.AbstractAdvisingBeanPostProcessor;
import infra.aop.framework.ProxyFactory;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.config.ConfigurableBeanFactory;

/**
 * Extension of {@link AbstractAdvisingBeanPostProcessor} which implements
 * {@link BeanFactoryAware}, adds exposure of the original target class for each
 * proxied bean ({@link AutoProxyUtils#ORIGINAL_TARGET_CLASS_ATTRIBUTE}),
 * and participates in an externally enforced target-class mode for any given bean
 * ({@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE}).
 * This post-processor is therefore aligned with {@link AbstractAutoProxyCreator}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractBeanFactoryAwareAdvisingPostProcessor
        extends AbstractAdvisingBeanPostProcessor implements BeanFactoryAware {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  protected ConfigurableBeanFactory beanFactory;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory instanceof ConfigurableBeanFactory
            ? (ConfigurableBeanFactory) beanFactory : null;
    AutoProxyUtils.applyDefaultProxyConfig(this, beanFactory);
  }

  @Override
  protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
    if (beanFactory != null) {
      AutoProxyUtils.exposeTargetClass(beanFactory, beanName, bean.getClass());
    }

    ProxyFactory proxyFactory = super.prepareProxyFactory(bean, beanName);
    if (this.beanFactory != null) {
      if (AutoProxyUtils.shouldProxyTargetClass(this.beanFactory, beanName)) {
        proxyFactory.setProxyTargetClass(true);
      }
      else {
        Class<?>[] ifcs = AutoProxyUtils.determineExposedInterfaces(this.beanFactory, beanName);
        if (ifcs != null) {
          proxyFactory.setProxyTargetClass(false);
          for (Class<?> ifc : ifcs) {
            proxyFactory.addInterface(ifc);
          }
        }
      }
    }
    return proxyFactory;
  }

  @Override
  protected boolean isEligible(Object bean, String beanName) {
    return !AutoProxyUtils.isOriginalInstance(beanName, bean.getClass())
            && super.isEligible(bean, beanName);
  }

}
