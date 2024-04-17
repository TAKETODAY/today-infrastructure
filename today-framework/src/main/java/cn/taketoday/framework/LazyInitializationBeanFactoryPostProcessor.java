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

package cn.taketoday.framework;

import java.util.ArrayList;
import java.util.Collection;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Nullable;

/**
 * {@link BeanFactoryPostProcessor} to set lazy-init on bean definitions that are not
 * {@link LazyInitializationExcludeFilter excluded} and have not already had a value
 * explicitly set.
 * <p>
 * Note that {@link SmartInitializingSingleton SmartInitializingSingletons} are
 * automatically excluded from lazy initialization to ensure that their
 * {@link SmartInitializingSingleton#afterSingletonsInstantiated() callback method} is
 * invoked.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Tyler Van Gorder
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LazyInitializationExcludeFilter
 * @since 4.0 2022/3/29 17:52
 */
public final class LazyInitializationBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    Collection<LazyInitializationExcludeFilter> filters = getFilters(beanFactory);
    for (String beanName : beanFactory.getBeanDefinitionNames()) {
      BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
      if (beanDefinition instanceof AbstractBeanDefinition) {
        postProcess(beanFactory, filters, beanName, (AbstractBeanDefinition) beanDefinition);
      }
    }
  }

  private Collection<LazyInitializationExcludeFilter> getFilters(ConfigurableBeanFactory beanFactory) {
    // Take care not to force the eager init of factory beans when getting filters
    var filters = new ArrayList<>(beanFactory.getBeansOfType(
            LazyInitializationExcludeFilter.class, false, false).values());
    filters.add(LazyInitializationExcludeFilter.forBeanTypes(SmartInitializingSingleton.class));
    filters.add(new InfrastructureRoleLazyInitializationExcludeFilter());
    return filters;
  }

  private void postProcess(ConfigurableBeanFactory beanFactory, Collection<LazyInitializationExcludeFilter> filters,
          String beanName, AbstractBeanDefinition beanDefinition) {
    Boolean lazyInit = beanDefinition.getLazyInit();
    if (lazyInit != null) {
      return;
    }
    Class<?> beanType = getBeanType(beanFactory, beanName);
    if (!isExcluded(filters, beanName, beanDefinition, beanType)) {
      beanDefinition.setLazyInit(true);
    }
  }

  @Nullable
  private Class<?> getBeanType(ConfigurableBeanFactory beanFactory, String beanName) {
    try {
      return beanFactory.getType(beanName, false);
    }
    catch (NoSuchBeanDefinitionException ex) {
      return null;
    }
  }

  private boolean isExcluded(Collection<LazyInitializationExcludeFilter> filters,
          String beanName, AbstractBeanDefinition beanDefinition, @Nullable Class<?> beanType) {
    if (beanType != null) {
      for (LazyInitializationExcludeFilter filter : filters) {
        if (filter.isExcluded(beanName, beanDefinition, beanType)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  /**
   * Excludes all {@link BeanDefinition bean definitions} which have the infrastructure
   * role from lazy initialization.
   */
  private static final class InfrastructureRoleLazyInitializationExcludeFilter implements LazyInitializationExcludeFilter {

    @Override
    public boolean isExcluded(String beanName, BeanDefinition def, Class<?> beanType) {
      return def.getRole() == BeanDefinition.ROLE_INFRASTRUCTURE;
    }

  }

}
