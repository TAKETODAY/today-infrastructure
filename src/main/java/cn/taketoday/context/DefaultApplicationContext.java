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

package cn.taketoday.context;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.DefaultBeanDefinition;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.beans.factory.StandardBeanFactory;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.core.Assert;
import cn.taketoday.util.CollectionUtils;

/**
 * ApplicationContext default implementation
 *
 * @author TODAY 2021/10/1 16:25
 * @since 4.0
 */
public class DefaultApplicationContext
        extends AbstractApplicationContext implements BeanDefinitionRegistry {

  private final StandardBeanFactory beanFactory;

  /**
   * Default Constructor
   */
  public DefaultApplicationContext() {
    this.beanFactory = new StandardBeanFactory();
  }

  public DefaultApplicationContext(StandardBeanFactory beanFactory) {
    Assert.notNull(beanFactory, "beanFactory must not be null");
    this.beanFactory = beanFactory;
  }

  @Override
  public StandardBeanFactory getBeanFactory() {
    return beanFactory;
  }

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionRegistry
  //---------------------------------------------------------------------

  @Override
  public void registerBeanDefinition(String name, BeanDefinition def) {
    beanFactory.registerBeanDefinition(name, def);
  }

  @Override
  public void removeBeanDefinition(String beanName) {
    beanFactory.removeBeanDefinition(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(Class<?> beanClass) {
    return beanFactory.getBeanDefinition(beanClass);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type) {
    return beanFactory.containsBeanDefinition(type);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type, boolean equals) {
    return beanFactory.containsBeanDefinition(type, equals);
  }

  @Override
  public boolean isBeanNameInUse(String beanName) {
    return beanFactory.isBeanNameInUse(beanName);
  }

  @Override
  public boolean isAllowBeanDefinitionOverriding() {
    return beanFactory.isAllowBeanDefinitionOverriding();
  }

  @Override
  public int getBeanDefinitionCount() {
    return beanFactory.getBeanDefinitionCount();
  }

  @Override
  public Set<String> getBeanDefinitionNames() {
    return beanFactory.getBeanDefinitionNames();
  }

  @Override
  public Iterator<String> getBeanNamesIterator() {
    return beanFactory.getBeanNamesIterator();
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    return beanFactory.containsBeanDefinition(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) {
    return beanFactory.getBeanDefinition(beanName);
  }

  @Override
  public Iterator<BeanDefinition> iterator() {
    return beanFactory.iterator();
  }
  // extra

  /**
   * register a bean with the given bean class
   *
   * @since 3.0
   */
  public void registerBean(Class<?> clazz) {
    registerBean(createBeanName(clazz), clazz);
  }

  public void registerBean(Set<Class<?>> candidates) {
    for (Class<?> candidate : candidates) {
      registerBean(createBeanName(candidate), candidate);
    }
  }

  public BeanDefinition registerBean(String name, Class<?> clazz) {
    DefaultBeanDefinition defaults = BeanDefinitionBuilder.defaults(name, clazz);
    registerBeanDefinition(name, defaults);
    return defaults;
  }

  /**
   * Register a bean with the bean instance
   * <p>
   *
   * @param obj
   *         bean instance
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   */
  public void registerBean(Object obj) {
    registerBean(createBeanName(obj.getClass()), obj);
  }

  /**
   * Register a bean with the given name and bean instance
   *
   * @param name
   *         bean name (must not be null)
   * @param obj
   *         bean instance (must not be null)
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   */
  public void registerBean(String name, Object obj) {
    Assert.notNull(name, "bean-name must not be null");
    Assert.notNull(obj, "bean-instance must not be null");
    getBeanFactory().registerSingleton(name, obj);
  }

  /**
   * Register a bean with the given type and instance supplier
   *
   * @param clazz
   *         bean class
   * @param supplier
   *         bean instance supplier
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 4.0
   */
  public <T> void registerBean(Class<T> clazz, Supplier<T> supplier) throws BeanDefinitionStoreException {
    registerBean(clazz, supplier, false);
  }

  /**
   * Register a bean with the given type and instance supplier
   *
   * @param clazz
   *         bean class
   * @param supplier
   *         bean instance supplier
   * @param prototype
   *         register as prototype?
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 4.0
   */
  public <T> void registerBean(
          Class<T> clazz, Supplier<T> supplier, boolean prototype) throws BeanDefinitionStoreException {
    registerBean(clazz, supplier, prototype, true);
  }

  /**
   * Register a bean with the given type and instance supplier
   *
   * @param clazz
   *         bean class
   * @param supplier
   *         bean instance supplier
   * @param prototype
   *         register as prototype?
   * @param ignoreAnnotation
   *         ignore {@link Component} scanning
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 4.0
   */
  public <T> void registerBean(
          Class<T> clazz, Supplier<T> supplier, boolean prototype, boolean ignoreAnnotation)
          throws BeanDefinitionStoreException //
  {
    Assert.notNull(clazz, "bean-class must not be null");
    Assert.notNull(supplier, "bean-instance-supplier must not be null");
    if (ignoreAnnotation) {
      DefaultBeanDefinition defaults = BeanDefinitionBuilder.defaults(clazz);
      if (prototype) {
        defaults.setScope(Scope.PROTOTYPE);
      }
      registerBeanDefinition(defaults);
    }
    else {
      List<BeanDefinition> loaded = BeanDefinitionBuilder.from(clazz);
      if (CollectionUtils.isNotEmpty(loaded)) {
        for (BeanDefinition def : loaded) {
          def.setSupplier(supplier);
          if (prototype) {
            def.setScope(Scope.PROTOTYPE);
          }
          registerBeanDefinition(def);
        }
      }
    }
  }

}
