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

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.core.DefaultAliasRegistry;
import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * Default implementation of the {@link BeanDefinitionRegistry} interface.
 * Provides registry capabilities only, with no factory capabilities built in.
 * Can for example be used for testing bean definition readers.
 *
 * @author TODAY 2021/10/1 14:56
 * @since 4.0
 */
public class SimpleBeanDefinitionRegistry extends DefaultAliasRegistry implements BeanDefinitionRegistry {

  /** Map of bean definition objects, keyed by bean name. */
  private final ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

  /** Whether to allow re-registration of a different definition with the same name. */
  private boolean allowBeanDefinitionOverriding = true;

  public Map<String, BeanDefinition> getBeanDefinitions() {
    return beanDefinitionMap;
  }

  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
          throws BeanDefinitionStoreException {

    Assert.hasText(beanName, "'beanName' must not be empty");
    Assert.notNull(beanDefinition, "BeanDefinition is required");
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

  @Nullable
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

  @Override
  public boolean isBeanDefinitionOverridable(String beanName) {
    return isAllowBeanDefinitionOverriding();
  }
}
