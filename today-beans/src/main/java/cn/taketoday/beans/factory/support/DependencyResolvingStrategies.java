/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ArrayHolder;

/**
 * Composite DependencyResolvingStrategy
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/16 22:50
 */
public class DependencyResolvingStrategies implements DependencyResolvingStrategy {

  private final ArrayHolder<DependencyResolvingStrategy> strategies
          = ArrayHolder.forClass(DependencyResolvingStrategy.class);

  @Override
  public Object resolveDependency(DependencyDescriptor descriptor, Context context) {
    for (DependencyResolvingStrategy resolvingStrategy : strategies) {
      Object dependency = resolvingStrategy.resolveDependency(descriptor, context);
      if (dependency != null) {
        return dependency;
      }
    }
    return null;
  }

  public void initStrategies(@Nullable BeanFactory beanFactory) {
    LoggerFactory.getLogger(DependencyResolvingStrategies.class)
            .debug("Initialize dependency-resolving-strategies");

    List<DependencyResolvingStrategy> strategies;
    if (beanFactory != null) {
      ClassLoader beanClassLoader = null;
      if (beanFactory instanceof ConfigurableBeanFactory configurable) {
        beanClassLoader = configurable.getBeanClassLoader();
      }
      strategies = TodayStrategies.find(DependencyResolvingStrategy.class,
              beanClassLoader, BeanFactoryAwareInstantiator.from(beanFactory));
    }
    else {
      strategies = TodayStrategies.find(DependencyResolvingStrategy.class);
    }

    this.strategies.addAll(strategies);
  }

  public ArrayHolder<DependencyResolvingStrategy> getStrategies() {
    return strategies;
  }

}
