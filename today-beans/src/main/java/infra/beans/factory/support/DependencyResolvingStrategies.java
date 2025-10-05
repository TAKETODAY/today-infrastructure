/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.beans.factory.config.DependencyDescriptor;
import infra.lang.TodayStrategies;
import infra.logging.LoggerFactory;
import infra.util.ArrayHolder;

/**
 * Composite DependencyResolvingStrategy
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/16 22:50
 */
public class DependencyResolvingStrategies implements DependencyResolvingStrategy {

  private final ArrayHolder<DependencyResolvingStrategy> strategies
          = ArrayHolder.forClass(DependencyResolvingStrategy.class);

  public DependencyResolvingStrategies() {
  }

  public DependencyResolvingStrategies(List<DependencyResolvingStrategy> strategyList) {
    getStrategies().addAll(strategyList);
  }

  @Nullable
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

  public void initStrategies(@Nullable ClassLoader classLoader) {
    LoggerFactory.getLogger(DependencyResolvingStrategies.class)
            .debug("Initialize dependency-resolving-strategies");
    this.strategies.addAll(TodayStrategies.find(DependencyResolvingStrategy.class, classLoader));
  }

  public ArrayHolder<DependencyResolvingStrategy> getStrategies() {
    return strategies;
  }

}
