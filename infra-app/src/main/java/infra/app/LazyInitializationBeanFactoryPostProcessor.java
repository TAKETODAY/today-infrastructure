/*
 * Copyright 2012-present the original author or authors.
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

package infra.app;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

import infra.beans.BeansException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.SmartInitializingSingleton;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.core.Ordered;

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
