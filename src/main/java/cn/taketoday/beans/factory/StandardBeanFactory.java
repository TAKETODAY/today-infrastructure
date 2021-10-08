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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import cn.taketoday.beans.FactoryBean;
import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.core.Nullable;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;

/**
 * Standard {@link BeanFactory} implementation
 *
 * @author TODAY 2019-03-23 15:00
 */
public class StandardBeanFactory
        extends AbstractAutowireCapableBeanFactory implements ConfigurableBeanFactory, BeanDefinitionRegistry {

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

  /** Map of bean definition objects, keyed by bean name */
  private final ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

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

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionRegistry interface @since 4.0
  //---------------------------------------------------------------------

  @Override
  public Map<String, BeanDefinition> getBeanDefinitions() {
    return beanDefinitionMap;
  }

  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition def) {
    if (FactoryBean.class.isAssignableFrom(def.getBeanClass())) { // process FactoryBean
      registerFactoryBean(beanName, def);
    }
    else {
      this.beanDefinitionMap.put(beanName, def);
      postProcessRegisterBeanDefinition(def);
    }
  }

  @Override
  public void registerBeanDefinition(BeanDefinition def) {
    beanDefinitionMap.put(def.getName(), def);
  }

  @Override
  public void removeBeanDefinition(String beanName) {
    beanDefinitionMap.remove(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) {
    return beanDefinitionMap.get(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(Class<?> beanClass) {
    BeanDefinition def = getBeanDefinition(createBeanName(beanClass));
    if (def != null && def.isAssignableTo(beanClass)) {
      return def;
    }
    for (BeanDefinition definition : getBeanDefinitions().values()) {
      if (definition.isAssignableTo(beanClass)) {
        return definition;
      }
    }
    return null;
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type) {
    return containsBeanDefinition(type, false);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type, boolean equals) {
    Predicate<BeanDefinition> predicate = getPredicate(type, equals);
    BeanDefinition def = getBeanDefinition(createBeanName(type));
    if (def != null && predicate.test(def)) {
      return true;
    }

    for (BeanDefinition beanDef : getBeanDefinitions().values()) {
      if (predicate.test(beanDef)) {
        return true;
      }
    }
    return false;
  }

  private Predicate<BeanDefinition> getPredicate(Class<?> type, boolean equals) {
    return equals
           ? beanDef -> type == beanDef.getBeanClass()
           : beanDef -> type.isAssignableFrom(beanDef.getBeanClass());
  }

  @Override
  public boolean containsBeanDefinition(String beanName, Class<?> type) {
    return containsBeanDefinition(beanName) && containsBeanDefinition(type);
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    return beanDefinitionMap.containsKey(beanName);
  }

  @Override
  public Set<String> getBeanDefinitionNames() {
    return beanDefinitionMap.keySet();
  }

  @Override
  public Iterator<String> getBeanNamesIterator() {
    return beanDefinitionMap.keySet().iterator();
  }

  @Override
  public int getBeanDefinitionCount() {
    return beanDefinitionMap.size();
  }

  @Override
  public boolean isBeanNameInUse(String beanName) {
    return beanDefinitionMap.containsKey(beanName);
  }

  @Override
  public Iterator<BeanDefinition> iterator() {
    return beanDefinitionMap.values().iterator();
  }

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
  // Implementation of ConfigurableBeanFactory interface @since 4.0
  //---------------------------------------------------------------------

  @Override
  public void removeBean(String name) {
    removeBeanDefinition(name);
    super.removeBean(name);
  }

}
