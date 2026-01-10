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

package infra.test.context;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.lang.TodayStrategies;
import infra.test.context.support.ContextCustomizerFactories;

/**
 * Factory for creating {@link ContextCustomizer ContextCustomizers}.
 *
 * <p>Factories are invoked after {@link ContextLoader ContextLoaders} have
 * processed context configuration attributes but before the
 * {@link MergedContextConfiguration} is created.
 *
 * <p>By default, the TestContext Framework will use the
 * {@link TodayStrategies TodayStrategies}
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
   * {@link infra.context.ConfigurableApplicationContext ConfigurableApplicationContext}
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
