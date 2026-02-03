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

package infra.aot.hint.support;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} to register hints for {@code infra.factories}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TodayStrategies
 * @since 4.0
 */
class TodayStrategiesRuntimeHints implements RuntimeHintsRegistrar {

  private static final List<String> RESOURCE_LOCATIONS = List.of(TodayStrategies.STRATEGIES_LOCATION);

  private static final Logger logger = LoggerFactory.getLogger(TodayStrategiesRuntimeHints.class);

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    ClassLoader classLoaderToUse = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    if (classLoaderToUse != null) {
      for (String resourceLocation : RESOURCE_LOCATIONS) {
        registerHints(hints, classLoaderToUse, resourceLocation);
      }
    }
  }

  private void registerHints(RuntimeHints hints, ClassLoader classLoader, String resourceLocation) {
    hints.resources().registerPattern(resourceLocation);
    Map<String, List<String>> factories =
            ExtendedTodayStrategies.accessLoadFactoriesResource(classLoader, resourceLocation);
    factories.forEach((factoryClassName, implementationClassNames) ->
            registerHints(hints, classLoader, factoryClassName, implementationClassNames));
  }

  private void registerHints(RuntimeHints hints, ClassLoader classLoader,
          String factoryClassName, List<String> implementationClassNames) {
    Class<?> factoryClass = resolveClassName(classLoader, factoryClassName);
    if (factoryClass == null) {
      if (logger.isTraceEnabled()) {
        logger.trace("Skipping factories for [{}]", factoryClassName);
      }
      return;
    }
    if (logger.isTraceEnabled()) {
      logger.trace("Processing factories for [{}]", factoryClassName);
    }
    hints.reflection().registerType(factoryClass, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    for (String implementationClassName : implementationClassNames) {
      Class<?> implementationType = resolveClassName(classLoader, implementationClassName);
      if (logger.isTraceEnabled()) {
        logger.trace("{} factory type [{}] and implementation [{}]",
                (implementationType != null ? "Processing" : "Skipping"), factoryClassName,
                implementationClassName);
      }
      if (implementationType != null) {
        hints.reflection().registerType(implementationType, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
      }
    }
  }

  @Nullable
  private Class<?> resolveClassName(ClassLoader classLoader, String factoryClassName) {
    try {
      Class<?> clazz = ClassUtils.resolveClassName(factoryClassName, classLoader);
      // Force resolution of all constructors to cache
      clazz.getDeclaredConstructors();
      return clazz;
    }
    catch (Throwable ex) {
      return null;
    }
  }

  private static class ExtendedTodayStrategies extends TodayStrategies {

    ExtendedTodayStrategies(ClassLoader classLoader, Map<String, List<String>> factories) {
      super(classLoader, factories);
    }

    static Map<String, List<String>> accessLoadFactoriesResource(ClassLoader classLoader, String resourceLocation) {
      return TodayStrategies.loadResource(classLoader, resourceLocation);
    }

  }

}
