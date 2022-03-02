/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.support;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.lang.Nullable;

/**
 * Base class for {@link cn.taketoday.context.ApplicationContext}
 * implementations which are supposed to support multiple calls to {@link #refresh()},
 * creating a new internal bean factory instance every time.
 * Typically (but not necessarily), such a context will be driven by
 * a set of config locations to load bean definitions from.
 *
 * <p>The only method to be implemented by subclasses is {@link #loadBeanDefinitions},
 * which gets invoked on each refresh. A concrete implementation is supposed to load
 * bean definitions into the given
 * {@link cn.taketoday.beans.factory.support.StandardBeanFactory},
 * typically delegating to one or more specific bean definition readers.
 *
 * <p><b>Note that there is a similar base class for WebApplicationContexts.</b>
 * {@link cn.taketoday.web.context.support.AbstractRefreshableWebApplicationContext}
 * provides the same subclassing strategy, but additionally pre-implements
 * all context functionality for web environments. There is also a
 * pre-defined way to receive config locations for a web context.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #loadBeanDefinitions
 * @see cn.taketoday.beans.factory.support.StandardBeanFactory
 * @see cn.taketoday.web.context.support.AbstractRefreshableWebApplicationContext
 * @since 4.0 2022/2/20 17:36
 */
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext
        implements BeanDefinitionRegistry {

  @Nullable
  private Boolean allowBeanDefinitionOverriding;

  @Nullable
  private Boolean allowCircularReferences;

  /** Bean factory for this context. */
  @Nullable
  private volatile StandardBeanFactory beanFactory;

  /**
   * Create a new AbstractRefreshableApplicationContext with no parent.
   */
  public AbstractRefreshableApplicationContext() { }

  /**
   * Create a new AbstractRefreshableApplicationContext with the given parent context.
   *
   * @param parent the parent context
   */
  public AbstractRefreshableApplicationContext(@Nullable ApplicationContext parent) {
    super(parent);
  }

  @Override
  protected BootstrapContext createBootstrapContext() {
    return new BootstrapContext(Objects.requireNonNullElse(beanFactory, this), this);
  }

  /**
   * Set whether it should be allowed to override bean definitions by registering
   * a different definition with the same name, automatically replacing the former.
   * If not, an exception will be thrown. Default is "true".
   *
   * @see cn.taketoday.beans.factory.support.StandardBeanFactory#setAllowBeanDefinitionOverriding
   */
  public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
    this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
  }

  /**
   * Set whether to allow circular references between beans - and automatically
   * try to resolve them.
   * <p>Default is "true". Turn this off to throw an exception when encountering
   * a circular reference, disallowing them completely.
   *
   * @see cn.taketoday.beans.factory.support.StandardBeanFactory#setAllowCircularReferences
   */
  public void setAllowCircularReferences(boolean allowCircularReferences) {
    this.allowCircularReferences = allowCircularReferences;
  }

  /**
   * This implementation performs an actual refresh of this context's underlying
   * bean factory, shutting down the previous bean factory (if any) and
   * initializing a fresh bean factory for the next phase of the context's lifecycle.
   */
  @Override
  protected final void refreshBeanFactory() throws BeansException {
    if (hasBeanFactory()) {
      destroyBeans();
      closeBeanFactory();
    }
    try {
      StandardBeanFactory beanFactory = createBeanFactory();
      customizeBeanFactory(beanFactory);
      loadBeanDefinitions(beanFactory);
      this.beanFactory = beanFactory;
    }
    catch (IOException ex) {
      throw new ApplicationContextException(
              "I/O error parsing bean definition source for " + getDisplayName(), ex);
    }
  }

  @Override
  protected final void closeBeanFactory() {
    this.beanFactory = null;
    this.setBootstrapContext(null);
  }

  /**
   * Determine whether this context currently holds a bean factory,
   * i.e. has been refreshed at least once and not been closed yet.
   */
  protected final boolean hasBeanFactory() {
    return beanFactory != null;
  }

  @Override
  public final StandardBeanFactory getBeanFactory() {
    StandardBeanFactory beanFactory = this.beanFactory;
    if (beanFactory == null) {
      throw new IllegalStateException("BeanFactory not initialized or already closed - " +
              "call 'refresh' before accessing beans via the ApplicationContext");
    }
    return beanFactory;
  }

  /**
   * Overridden to turn it into a no-op: With AbstractRefreshableApplicationContext,
   * {@link #getBeanFactory()} serves a strong assertion for an active context anyway.
   */
  @Override
  protected void assertBeanFactoryActive() { }

  /**
   * Create an internal bean factory for this context.
   * Called for each {@link #refresh()} attempt.
   * <p>The default implementation creates a
   * {@link cn.taketoday.beans.factory.support.StandardBeanFactory}
   * with the {@linkplain #getInternalParentBeanFactory() internal bean factory} of this
   * context's parent as parent bean factory. Can be overridden in subclasses,
   * for example to customize StandardBeanFactory's settings.
   *
   * @return the bean factory for this context
   * @see cn.taketoday.beans.factory.support.StandardBeanFactory#setAllowBeanDefinitionOverriding
   * @see cn.taketoday.beans.factory.support.StandardBeanFactory#setAllowEagerClassLoading
   * @see cn.taketoday.beans.factory.support.StandardBeanFactory#setAllowCircularReferences
   * @see cn.taketoday.beans.factory.support.StandardBeanFactory#setAllowRawInjectionDespiteWrapping
   */
  protected StandardBeanFactory createBeanFactory() {
    return new StandardBeanFactory(getInternalParentBeanFactory());
  }

  /**
   * Customize the internal bean factory used by this context.
   * Called for each {@link #refresh()} attempt.
   * <p>The default implementation applies this context's
   * {@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"}
   * and {@linkplain #setAllowCircularReferences "allowCircularReferences"} settings,
   * if specified. Can be overridden in subclasses to customize any of
   * {@link StandardBeanFactory}'s settings.
   *
   * @param beanFactory the newly created bean factory for this context
   * @see StandardBeanFactory#setAllowBeanDefinitionOverriding
   * @see StandardBeanFactory#setAllowCircularReferences
   * @see StandardBeanFactory#setAllowRawInjectionDespiteWrapping
   * @see StandardBeanFactory#setAllowEagerClassLoading
   */
  protected void customizeBeanFactory(StandardBeanFactory beanFactory) {
    if (this.allowBeanDefinitionOverriding != null) {
      beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
    }
    if (this.allowCircularReferences != null) {
      beanFactory.setAllowCircularReferences(this.allowCircularReferences);
    }
  }

  /**
   * Load bean definitions into the given bean factory, typically through
   * delegating to one or more bean definition readers.
   *
   * @param beanFactory the bean factory to load bean definitions into
   * @throws BeansException if parsing of the bean definitions failed
   * @throws IOException if loading of bean definition files failed
   */
  protected abstract void loadBeanDefinitions(StandardBeanFactory beanFactory)
          throws BeansException, IOException;

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionRegistry interface
  //---------------------------------------------------------------------

  @Override
  public Map<String, BeanDefinition> getBeanDefinitions() {
    return getBeanFactory().getBeanDefinitions();
  }

  @Override
  public void registerBeanDefinition(BeanDefinition def) {
    getBeanFactory().registerBeanDefinition(def);
  }

  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition def) {
    getBeanFactory().registerBeanDefinition(beanName, def);
  }

  @Override
  public void removeBeanDefinition(String beanName) {
    getBeanFactory().removeBeanDefinition(beanName);
  }

  @Nullable
  @Override
  public BeanDefinition getBeanDefinition(String beanName) {
    return getBeanFactory().getBeanDefinition(beanName);
  }

  @Nullable
  @Override
  public BeanDefinition getBeanDefinition(Class<?> requiredType) {
    return getBeanFactory().getBeanDefinition(requiredType);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type) {
    return getBeanFactory().containsBeanDefinition(type);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type, boolean equals) {
    return getBeanFactory().containsBeanDefinition(type, equals);
  }

  @Override
  public boolean containsBeanDefinition(String beanName, Class<?> type) {
    return getBeanFactory().containsBeanDefinition(beanName, type);
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    return getBeanFactory().containsBeanDefinition(beanName);
  }

  @Override
  public String[] getBeanDefinitionNames() {
    return getBeanFactory().getBeanDefinitionNames();
  }

  @Override
  public Iterator<String> getBeanNamesIterator() {
    return getBeanFactory().getBeanNamesIterator();
  }

  @Override
  public int getBeanDefinitionCount() {
    return getBeanFactory().getBeanDefinitionCount();
  }

  @Override
  public boolean isAllowBeanDefinitionOverriding() {
    return getBeanFactory().isAllowBeanDefinitionOverriding();
  }

  @Override
  public void registerAlias(String name, String alias) {
    getBeanFactory().registerAlias(name, alias);
  }

  @Override
  public void removeAlias(String alias) {
    getBeanFactory().removeAlias(alias);
  }

  @Override
  public boolean isAlias(String name) {
    return getBeanFactory().isAlias(name);
  }

  @Override
  public List<String> getAliasList(String name) {
    return getBeanFactory().getAliasList(name);
  }

  @Override
  public boolean isBeanNameInUse(String beanName) {
    return getBeanFactory().isBeanNameInUse(beanName);
  }

}

