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

package cn.taketoday.test.context;

import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.support.ContextCustomizerFactories;

/**
 * Factory for creating {@link ContextCustomizer ContextCustomizers}.
 *
 * <p>Factories are invoked after {@link ContextLoader ContextLoaders} have
 * processed context configuration attributes but before the
 * {@link MergedContextConfiguration} is created.
 *
 * <p>By default, the TestContext Framework will use the
 * {@link cn.taketoday.lang.TodayStrategies TodayStrategies}
 * mechanism for loading factories configured in all {@code META-INF/today.strategies}
 * files on the classpath.
 *
 * <p>it is also possible to register factories
 * declaratively via the {@link ContextCustomizerFactories @ContextCustomizerFactories}
 * annotation.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ContextCustomizer
 * @see ContextCustomizerFactories @ContextCustomizerFactories
 * @since 4.0
 */
@FunctionalInterface
public interface ContextCustomizerFactory {

  /**
   * Create a {@link ContextCustomizer} that should be used to customize a
   * {@link cn.taketoday.context.ConfigurableApplicationContext ConfigurableApplicationContext}
   * before it is refreshed.
   *
   * @param testClass the test class
   * @param configAttributes the list of context configuration attributes for
   * the test class, ordered <em>bottom-up</em> (i.e., as if we were traversing
   * up the class hierarchy); never {@code null} or empty
   * @return a {@link ContextCustomizer} or {@code null} if no customizer should
   * be used
   */
  @Nullable
  ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes);

}
