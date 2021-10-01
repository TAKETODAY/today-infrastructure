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
package cn.taketoday.beans.factory;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.FactoryBean;
import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.core.Nullable;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;

/**
 * Standard {@link BeanFactory} implementation
 *
 * @author TODAY 2019-03-23 15:00
 */
public class StandardBeanFactory
        extends AbstractBeanFactory implements ConfigurableBeanFactory {

  private static final Logger log = LoggerFactory.getLogger(StandardBeanFactory.class);

  /** Map from serialized id to factory instance. */
  private static final ConcurrentHashMap<String, Reference<StandardBeanFactory>>
          serializableFactories = new ConcurrentHashMap<>(8);

  /** Optional id for this factory, for serialization purposes. */
  @Nullable
  private String serializationId;

  /**
   * @since 2.1.7 Preventing repeated initialization of beans(Prevent duplicate
   * initialization) , Prevent Cycle Dependency
   */
  private final HashSet<String> currentInitializingBeanName = new HashSet<>();

  /** Whether to allow re-registration of a different definition with the same name. */
  private boolean allowBeanDefinitionOverriding = true;

  /**
   * Specify an id for serialization purposes, allowing this BeanFactory to be
   * deserialized from this id back into the BeanFactory object, if needed.
   *
   * @since 4.0
   */
  public void setSerializationId(@Nullable String serializationId) {
    if (serializationId != null) {
      serializableFactories.put(serializationId, new WeakReference<>(this));
    }
    else if (this.serializationId != null) {
      serializableFactories.remove(this.serializationId);
    }
    this.serializationId = serializationId;
  }

  /**
   * Return an id for serialization purposes, if specified, allowing this BeanFactory
   * to be deserialized from this id back into the BeanFactory object, if needed.
   *
   * @since 4.0
   */
  @Nullable
  public String getSerializationId() {
    return this.serializationId;
  }

  @Override
  protected void awareInternal(final Object bean, final BeanDefinition def) {
    super.awareInternal(bean, def);

    if (bean instanceof ApplicationContextAware) {
      ((ApplicationContextAware) bean).setApplicationContext(getApplicationContext());
    }

    if (bean instanceof EnvironmentAware) {
      ((EnvironmentAware) bean).setEnvironment(getApplicationContext().getEnvironment());
    }
    if (bean instanceof ImportAware) { // @since 3.0
      final Object attribute = def.getAttribute(ImportAnnotatedMetadata);
      if (attribute instanceof BeanDefinition) {
        ((ImportAware) bean).setImportBeanDefinition((BeanDefinition) attribute);
      }
    }
  }

  /**
   * Preventing Cycle Dependency expected {@link Prototype} beans
   */
  @Override
  public Object initializeBean(final Object bean, final BeanDefinition def) {
    if (def.isPrototype()) {
      return super.initializeBean(bean, def);
    }

    final String name = def.getName();
    if (currentInitializingBeanName.contains(name)) {
      return bean;
    }
    currentInitializingBeanName.add(name);
    final Object initializingBean = super.initializeBean(bean, def);
    currentInitializingBeanName.remove(name);
    return initializingBean;
  }

  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition def) {
    if (FactoryBean.class.isAssignableFrom(def.getBeanClass())) { // process FactoryBean
      registerFactoryBean(beanName, def);
    }
    else {
      super.registerBeanDefinition(beanName, def);
    }
  }

  /**
   * Register {@link FactoryBeanDefinition} to the {@link BeanFactory}
   *
   * @param oldBeanName
   *         Target old bean name
   * @param factoryDef
   *         {@link FactoryBean} Bean definition
   */
  protected void registerFactoryBean(final String oldBeanName, final BeanDefinition factoryDef) {

    final FactoryBeanDefinition<?> def = //
            factoryDef instanceof FactoryBeanDefinition
            ? (FactoryBeanDefinition<?>) factoryDef
            : new FactoryBeanDefinition<>(factoryDef, this);

    registerBeanDefinition(oldBeanName, def);
  }

  //

  /**
   * Set whether it should be allowed to override bean definitions by registering
   * a different definition with the same name, automatically replacing the former.
   * If not, an exception will be thrown. This also applies to overriding aliases.
   * <p>Default is "true".
   *
   * @see #registerBeanDefinition
   * @since 4.0
   */
  public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
    this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
  }

  /**
   * Return whether it should be allowed to override bean definitions by registering
   * a different definition with the same name, automatically replacing the former.
   *
   * @since 4.0
   */
  @Override
  public boolean isAllowBeanDefinitionOverriding() {
    return this.allowBeanDefinitionOverriding;
  }

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionRegistry interface
  //---------------------------------------------------------------------

}
