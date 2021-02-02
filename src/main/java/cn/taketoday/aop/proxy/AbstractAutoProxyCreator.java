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

package cn.taketoday.aop.proxy;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.aop.target.TargetSourceCreator;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.InstantiationAwareBeanPostProcessor;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;

/**
 * @author TODAY 2021/2/1 21:31
 */
public abstract class AbstractAutoProxyCreator
        extends ProxyConfig implements InstantiationAwareBeanPostProcessor, BeanFactoryAware {

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  private BeanFactory beanFactory;

  /**
   * Indicates whether or not the proxy should be frozen. Overridden from super
   * to prevent the configuration from becoming frozen too early.
   */
  private boolean freezeProxy = false;

  private transient TargetSourceCreator[] targetSourceCreators;

  /**
   * Set custom {@code TargetSourceCreators} to be applied in this order.
   * If the list is empty, or they all return null, a {@link SingletonTargetSource}
   * will be created for each bean.
   * <p>Note that TargetSourceCreators will kick in even for target beans
   * where no advices or advisors have been found. If a {@code TargetSourceCreator}
   * returns a {@link TargetSource} for a specific bean, that bean will be proxied
   * in any case.
   * <p>{@code TargetSourceCreators} can only be invoked if this post processor is used
   * in a {@link BeanFactory} and its {@link BeanFactoryAware} callback is triggered.
   *
   * @param targetSourceCreators
   *         the list of {@code TargetSourceCreators}.
   *         Ordering is significant: The {@code TargetSource} returned from the first matching
   *         {@code TargetSourceCreator} (that is, the first that returns non-null) will be used.
   */
  public void setTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
    this.targetSourceCreators = targetSourceCreators;
  }

  /**
   * Set whether or not the proxy should be frozen, preventing advice
   * from being added to it once it is created.
   * <p>Overridden from the super class to prevent the proxy configuration
   * from being frozen before the proxy is created.
   */
  @Override
  public void setFrozen(boolean frozen) {
    this.freezeProxy = frozen;
  }

  @Override
  public boolean isFrozen() {
    return this.freezeProxy;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  protected BeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  @Override
  public Object postProcessBeforeInstantiation(final BeanDefinition def) {
    // Create proxy here if we have a custom TargetSource.
    // Suppresses unnecessary default instantiation of the target bean:
    // The TargetSource will handle target instances in a custom fashion.
    TargetSource targetSource = getCustomTargetSource(def);
    if (targetSource != null) {
      Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(def, targetSource);
      return createProxy(def, specificInterceptors, targetSource);
    }
    return null;
  }

  protected abstract Object[] getAdvicesAndAdvisorsForBean(BeanDefinition def, TargetSource targetSource);

  protected TargetSource getCustomTargetSource(BeanDefinition def) {
    // We can't create fancy target sources for directly registered singletons.
    if (this.targetSourceCreators != null) {
      for (TargetSourceCreator creator : this.targetSourceCreators) {
        TargetSource source = creator.getTargetSource(def);
        if (source != null) {
          // Found a matching TargetSource.
          if (logger.isTraceEnabled()) {
            logger.trace("TargetSourceCreator [{}] found custom TargetSource for bean with BeanDefinition '{}'",
                         creator, def);
          }
          return source;
        }
      }
    }
    // No custom TargetSource found.
    return null;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, BeanDefinition def) {

    return null;
  }

  protected Object createProxy(BeanDefinition def, Object[] specificInterceptors, TargetSource targetSource) {

    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.copyFrom(this);

    if (!proxyFactory.isProxyTargetClass()) {
      if (shouldProxyTargetClass(def)) {
        proxyFactory.setProxyTargetClass(true);
      }
      else {
//        evaluateProxyInterfaces(beanClass, proxyFactory);
      }
    }

    Advisor[] advisors = buildAdvisors(def, specificInterceptors);
    proxyFactory.addAdvisors(advisors);
    proxyFactory.setTargetSource(targetSource);
    customizeProxyFactory(proxyFactory);

    proxyFactory.setFrozen(this.freezeProxy);
    if (advisorsPreFiltered()) {
      proxyFactory.setPreFiltered(true);
    }

    return proxyFactory.getProxy(getProxyClassLoader());
  }

  private ClassLoader getProxyClassLoader() {
    return null;
  }

  protected boolean advisorsPreFiltered() {
    return false;
  }

  protected void customizeProxyFactory(ProxyFactory proxyFactory) {

  }

  protected Advisor[] buildAdvisors(BeanDefinition def, Object[] specificInterceptors) {
    return null;
  }

  /**
   * Determine whether the given bean should be proxied with its target class rather than its interfaces.
   * <p>Checks the {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
   * of the corresponding bean definition.
   *
   * @param def
   *         the BeanDefinition of the bean
   *
   * @return whether the given bean should be proxied with its target class
   *
   * @see AutoProxyUtils#shouldProxyTargetClass
   */
  protected boolean shouldProxyTargetClass(BeanDefinition def) {
//    return def.getAttribute();
    return false;
  }

}
