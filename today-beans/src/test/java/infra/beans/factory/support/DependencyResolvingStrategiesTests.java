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

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.beans.factory.config.DependencyDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 17:38
 */
class DependencyResolvingStrategiesTests {

  @Test
  void defaultConstructorCreatesEmptyStrategies() {
    DependencyResolvingStrategies strategies = new DependencyResolvingStrategies();
    assertThat(strategies.strategies).isEmpty();
  }

  @Test
  void constructorWithStrategyListInitializesStrategies() {
    DependencyResolvingStrategy strategy1 = mock(DependencyResolvingStrategy.class);
    DependencyResolvingStrategy strategy2 = mock(DependencyResolvingStrategy.class);
    List<DependencyResolvingStrategy> strategyList = List.of(strategy1, strategy2);

    DependencyResolvingStrategies strategies = new DependencyResolvingStrategies(strategyList);

    assertThat(strategies.strategies).containsExactly(strategy1, strategy2);
  }

  @Test
  void resolveDependencyReturnsFirstNonNullResult() {
    DependencyDescriptor descriptor = mock(DependencyDescriptor.class);
    DependencyResolvingStrategy.Context context = mock(DependencyResolvingStrategy.Context.class);

    DependencyResolvingStrategy strategy1 = mock(DependencyResolvingStrategy.class);
    DependencyResolvingStrategy strategy2 = mock(DependencyResolvingStrategy.class);
    List<DependencyResolvingStrategy> strategyList = List.of(strategy1, strategy2);

    when(strategy1.resolveDependency(descriptor, context)).thenReturn(null);
    when(strategy2.resolveDependency(descriptor, context)).thenReturn("dependency");

    DependencyResolvingStrategies strategies = new DependencyResolvingStrategies(strategyList);

    Object result = strategies.resolveDependency(descriptor, context);

    assertThat(result).isEqualTo("dependency");
    verify(strategy1).resolveDependency(descriptor, context);
    verify(strategy2).resolveDependency(descriptor, context);
  }

  @Test
  void resolveDependencyReturnsNullWhenAllStrategiesReturnNull() {
    DependencyDescriptor descriptor = mock(DependencyDescriptor.class);
    DependencyResolvingStrategy.Context context = mock(DependencyResolvingStrategy.Context.class);

    DependencyResolvingStrategy strategy1 = mock(DependencyResolvingStrategy.class);
    DependencyResolvingStrategy strategy2 = mock(DependencyResolvingStrategy.class);
    List<DependencyResolvingStrategy> strategyList = List.of(strategy1, strategy2);

    when(strategy1.resolveDependency(descriptor, context)).thenReturn(null);
    when(strategy2.resolveDependency(descriptor, context)).thenReturn(null);

    DependencyResolvingStrategies strategies = new DependencyResolvingStrategies(strategyList);

    Object result = strategies.resolveDependency(descriptor, context);

    assertThat(result).isNull();
    verify(strategy1).resolveDependency(descriptor, context);
    verify(strategy2).resolveDependency(descriptor, context);
  }

  @Test
  void resolveDependencyStopsAtFirstNonNullResult() {
    DependencyDescriptor descriptor = mock(DependencyDescriptor.class);
    DependencyResolvingStrategy.Context context = mock(DependencyResolvingStrategy.Context.class);

    DependencyResolvingStrategy strategy1 = mock(DependencyResolvingStrategy.class);
    DependencyResolvingStrategy strategy2 = mock(DependencyResolvingStrategy.class);
    DependencyResolvingStrategy strategy3 = mock(DependencyResolvingStrategy.class);
    List<DependencyResolvingStrategy> strategyList = List.of(strategy1, strategy2, strategy3);

    when(strategy1.resolveDependency(descriptor, context)).thenReturn(null);
    when(strategy2.resolveDependency(descriptor, context)).thenReturn("dependency");
    // strategy3 should not be called

    DependencyResolvingStrategies strategies = new DependencyResolvingStrategies(strategyList);

    Object result = strategies.resolveDependency(descriptor, context);

    assertThat(result).isEqualTo("dependency");
    verify(strategy1).resolveDependency(descriptor, context);
    verify(strategy2).resolveDependency(descriptor, context);
    verify(strategy3, never()).resolveDependency(any(), any());
  }

  @Test
  void initStrategiesAddsStrategiesFromTodayStrategies() {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    DependencyResolvingStrategies strategies = new DependencyResolvingStrategies();

    strategies.initStrategies(classLoader);

    // Should not throw exception and strategies list should be populated
    assertThat(strategies.strategies).isNotNull();
  }

}