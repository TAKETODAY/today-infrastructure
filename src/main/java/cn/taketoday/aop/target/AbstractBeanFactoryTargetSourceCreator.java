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

package cn.taketoday.aop.target;

import java.util.HashMap;

import cn.taketoday.aop.AopInfrastructureBean;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.support.AbstractBeanFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Convenient superclass for {@link cn.taketoday.aop.target.TargetSourceCreator}
 * implementations that require creating multiple instances of a prototype bean.
 *
 * <p>Uses an internal BeanFactory to manage the target instances,
 * copying the original bean definition to this internal factory.
 * This is necessary because the original BeanFactory will just
 * contain the proxy instance created through auto-proxying.
 *
 * <p>Requires running in an
 * {@link AbstractBeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractBeanFactoryTargetSource
 * @see AbstractBeanFactory
 * @since 4.0 2021/12/13 22:24
 */
public abstract class AbstractBeanFactoryTargetSourceCreator
        implements TargetSourceCreator, BeanFactoryAware, DisposableBean {
  private static final Logger log = LoggerFactory.getLogger(AbstractBeanFactoryTargetSourceCreator.class);

  private ConfigurableBeanFactory beanFactory;

  /** Internally used StandardBeanFactory instances, keyed by bean name. */
  private final HashMap<String, StandardBeanFactory> internalBeanFactories = new HashMap<>();

  @Override
  public final void setBeanFactory(BeanFactory beanFactory) {
    if (!(beanFactory instanceof ConfigurableBeanFactory)) {
      throw new IllegalStateException("Cannot do auto-TargetSource creation with a BeanFactory " +
              "that doesn't implement ConfigurableBeanFactory: " + beanFactory.getClass());
    }
    this.beanFactory = (ConfigurableBeanFactory) beanFactory;
  }

  /**
   * Return the BeanFactory that this TargetSourceCreators runs in.
   */
  protected final BeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  //---------------------------------------------------------------------
  // Implementation of the TargetSourceCreator interface
  //---------------------------------------------------------------------

  @Override
  @Nullable
  public final TargetSource getTargetSource(Class<?> beanClass, String beanName) {
    AbstractBeanFactoryTargetSource targetSource =
            createBeanFactoryTargetSource(beanClass, beanName);
    if (targetSource == null) {
      return null;
    }

    if (log.isDebugEnabled()) {
      log.debug("Configuring AbstractBeanFactoryBasedTargetSource: {}", targetSource);
    }

    StandardBeanFactory internalBeanFactory = getInternalBeanFactoryForBean(beanName);
    // We need to override just this bean definition, as it may reference other beans
    // and we're happy to take the parent's definition for those.
    // Always use prototype scope if demanded.
    BeanDefinition bd = BeanFactoryUtils.requiredDefinition(beanFactory, beanName);
    BeanDefinition bdCopy = bd.cloneDefinition();
    if (isPrototypeBased()) {
      bdCopy.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    }
    internalBeanFactory.registerBeanDefinition(beanName, bdCopy);

    // Complete configuring the PrototypeTargetSource.
    targetSource.setTargetBeanName(beanName);
    targetSource.setBeanFactory(internalBeanFactory);

    return targetSource;
  }

  /**
   * Return the internal BeanFactory to be used for the specified bean.
   *
   * @param beanName the name of the target bean
   * @return the internal BeanFactory to be used
   */
  protected StandardBeanFactory getInternalBeanFactoryForBean(String beanName) {
    synchronized(this.internalBeanFactories) {
      return this.internalBeanFactories.computeIfAbsent(
              beanName, name -> buildInternalBeanFactory(this.beanFactory));
    }
  }

  /**
   * Build an internal BeanFactory for resolving target beans.
   *
   * @param containingFactory the containing BeanFactory that originally defines the beans
   * @return an independent internal BeanFactory to hold copies of some target beans
   */
  protected StandardBeanFactory buildInternalBeanFactory(ConfigurableBeanFactory containingFactory) {
    // Set parent so that references (up container hierarchies) are correctly resolved.
    StandardBeanFactory internalBeanFactory = new StandardBeanFactory(containingFactory);

    // Required so that all BeanPostProcessors, Scopes, etc become available.
    internalBeanFactory.copyConfigurationFrom(containingFactory);

    // Filter out BeanPostProcessors that are part of the AOP infrastructure,
    // since those are only meant to apply to beans defined in the original factory.
    internalBeanFactory.getBeanPostProcessors().removeIf(beanPostProcessor ->
            beanPostProcessor instanceof AopInfrastructureBean);

    return internalBeanFactory;
  }

  /**
   * Destroys the internal BeanFactory on shutdown of the TargetSourceCreator.
   *
   * @see #getInternalBeanFactoryForBean
   */
  @Override
  public void destroy() {
    synchronized(this.internalBeanFactories) {
      for (StandardBeanFactory bf : this.internalBeanFactories.values()) {
        bf.destroySingletons();
      }
    }
  }

  //---------------------------------------------------------------------
  // Template methods to be implemented by subclasses
  //---------------------------------------------------------------------

  /**
   * Return whether this TargetSourceCreator is prototype-based.
   * The scope of the target bean definition will be set accordingly.
   * <p>Default is "true".
   *
   * @see BeanDefinition#isSingleton()
   */
  protected boolean isPrototypeBased() {
    return true;
  }

  /**
   * Subclasses must implement this method to return a new AbstractPrototypeTargetSource
   * if they wish to create a custom TargetSource for this bean, or {@code null} if they are
   * not interested it in, in which case no special target source will be created.
   * Subclasses should not call {@code setTargetBeanName} or {@code setBeanFactory}
   * on the AbstractPrototypeTargetSource: This class' implementation of
   * {@code getTargetSource()} will do that.
   *
   * @param beanClass the class of the bean to create a TargetSource for
   * @param beanName the name of the bean
   * @return the AbstractPrototypeBasedTargetSource, or {@code null} if we don't match this
   */
  @Nullable
  protected abstract AbstractBeanFactoryTargetSource createBeanFactoryTargetSource(
          Class<?> beanClass, String beanName);

}
