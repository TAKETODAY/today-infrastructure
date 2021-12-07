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

package cn.taketoday.aop.proxy;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.lang.Nullable;

/**
 * Extension of {@link AbstractAutoProxyCreator} which implements {@link BeanFactoryAware},
 * adds exposure of the original target class for each proxied bean
 * ({@link ProxyUtils#ORIGINAL_TARGET_CLASS_ATTRIBUTE}),
 * and participates in an externally enforced target-class mode for any given bean
 * ({@link ProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE}).
 * This post-processor is therefore aligned with {@link AbstractAutoProxyCreator}.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanFactoryAwareAdvisingPostProcessor
        extends AbstractAdvisingBeanPostProcessor implements BeanFactoryAware {

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory instanceof ConfigurableBeanFactory
                       ? (ConfigurableBeanFactory) beanFactory : null;
  }

  @Override
  protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
    if (this.beanFactory != null) {
      ProxyUtils.exposeTargetClass(this.beanFactory, beanName, bean.getClass());
    }

    ProxyFactory proxyFactory = super.prepareProxyFactory(bean, beanName);
    if (!proxyFactory.isProxyTargetClass() && this.beanFactory != null &&
            ProxyUtils.shouldProxyTargetClass(this.beanFactory, beanName)) {
      proxyFactory.setProxyTargetClass(true);
    }
    return proxyFactory;
  }

}
