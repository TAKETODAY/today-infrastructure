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

package cn.taketoday.beans.factory.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.core.DefaultAliasRegistry;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Default implementation of the {@link BeanDefinitionRegistry} interface.
 * Provides registry capabilities only, with no factory capabilities built in.
 * Can for example be used for testing bean definition readers.
 *
 * @author TODAY 2021/10/1 14:56
 * @since 4.0
 */
public class SimpleBeanDefinitionRegistry
        extends DefaultAliasRegistry implements BeanDefinitionRegistry {

  /** Map of bean definition objects, keyed by bean name. */
  private final ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

  /** Whether to allow re-registration of a different definition with the same name. */
  private boolean allowBeanDefinitionOverriding = true;

  @Override
  public Map<String, BeanDefinition> getBeanDefinitions() {
    return beanDefinitionMap;
  }

  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
          throws BeanDefinitionStoreException {

    Assert.hasText(beanName, "'beanName' must not be empty");
    Assert.notNull(beanDefinition, "BeanDefinition must not be null");
    this.beanDefinitionMap.put(beanName, beanDefinition);
  }

  @Override
  public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
    if (this.beanDefinitionMap.remove(beanName) == null) {
      throw new NoSuchBeanDefinitionException(beanName);
    }
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
    BeanDefinition bd = this.beanDefinitionMap.get(beanName);
    if (bd == null) {
      throw new NoSuchBeanDefinitionException(beanName);
    }
    return bd;
  }

  @Override
  public BeanDefinition getBeanDefinition(Class<?> beanClass) {
    for (final BeanDefinition definition : getBeanDefinitions().values()) {
      if (definition instanceof AbstractBeanDefinition abd
              && beanClass.isAssignableFrom(abd.getBeanClass())) {
        return definition;
      }
    }
    return null;
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    return this.beanDefinitionMap.containsKey(beanName);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type) {
    return containsBeanDefinition(type, false);
  }

  @Override
  public boolean containsBeanDefinition(final Class<?> type, final boolean equals) {
    final Predicate<AbstractBeanDefinition> predicate = getPredicate(type, equals);
    for (final BeanDefinition beanDef : getBeanDefinitions().values()) {
      if (beanDef instanceof AbstractBeanDefinition abd && predicate.test(abd)) {
        return true;
      }
    }
    return false;
  }

  private Predicate<AbstractBeanDefinition> getPredicate(final Class<?> type, final boolean equals) {
    return equals
           ? beanDef -> type == beanDef.getBeanClass()
           : beanDef -> type.isAssignableFrom(beanDef.getBeanClass());
  }

  @Override
  public String[] getBeanDefinitionNames() {
    return StringUtils.toStringArray(this.beanDefinitionMap.keySet());
  }

  @Override
  public int getBeanDefinitionCount() {
    return this.beanDefinitionMap.size();
  }

  @Override
  public boolean isBeanNameInUse(String beanName) {
    return isAlias(beanName) || containsBeanDefinition(beanName);
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

  @Override
  public boolean isAllowBeanDefinitionOverriding() {
    return allowBeanDefinitionOverriding;
  }

}
