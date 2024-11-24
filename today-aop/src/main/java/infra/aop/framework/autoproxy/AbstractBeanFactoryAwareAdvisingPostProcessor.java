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

package infra.aop.framework.autoproxy;

import java.io.Serial;

import infra.aop.framework.AbstractAdvisingBeanPostProcessor;
import infra.aop.framework.ProxyFactory;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.lang.Nullable;

/**
 * Extension of {@link AbstractAutoProxyCreator} which implements {@link BeanFactoryAware},
 * adds exposure of the original target class for each proxied bean
 * ({@link AutoProxyUtils#ORIGINAL_TARGET_CLASS_ATTRIBUTE}),
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
  private ConfigurableBeanFactory beanFactory;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory instanceof ConfigurableBeanFactory
                       ? (ConfigurableBeanFactory) beanFactory : null;
  }

  @Override
  protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
    if (beanFactory != null) {
      AutoProxyUtils.exposeTargetClass(beanFactory, beanName, bean.getClass());
    }

    ProxyFactory proxyFactory = super.prepareProxyFactory(bean, beanName);
    if (!proxyFactory.isProxyTargetClass()
            && beanFactory != null && AutoProxyUtils.shouldProxyTargetClass(beanFactory, beanName)) {
      proxyFactory.setProxyTargetClass(true);
    }
    return proxyFactory;
  }

  @Override
  protected boolean isEligible(Object bean, String beanName) {
    return !AutoProxyUtils.isOriginalInstance(beanName, bean.getClass())
            && super.isEligible(bean, beanName);
  }

}
