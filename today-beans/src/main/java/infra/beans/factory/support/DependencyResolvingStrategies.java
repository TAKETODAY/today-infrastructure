/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.beans.factory.config.DependencyDescriptor;
import infra.lang.TodayStrategies;
import infra.logging.LoggerFactory;

/**
 * Composite DependencyResolvingStrategy
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/16 22:50
 */
public class DependencyResolvingStrategies implements DependencyResolvingStrategy {

  final ArrayList<DependencyResolvingStrategy> strategies = new ArrayList<>();

  public DependencyResolvingStrategies() {
  }

  public DependencyResolvingStrategies(List<DependencyResolvingStrategy> strategyList) {
    strategies.addAll(strategyList);
    strategies.trimToSize();
  }

  @Override
  public @Nullable Object resolveDependency(DependencyDescriptor descriptor, Context context) {
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
    strategies.trimToSize();
  }

}
