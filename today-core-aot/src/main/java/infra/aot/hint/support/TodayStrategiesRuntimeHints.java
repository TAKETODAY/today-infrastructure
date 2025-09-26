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

    ExtendedTodayStrategies(@Nullable ClassLoader classLoader, Map<String, List<String>> factories) {
      super(classLoader, factories);
    }

    static Map<String, List<String>> accessLoadFactoriesResource(ClassLoader classLoader, String resourceLocation) {
      return TodayStrategies.loadResource(classLoader, resourceLocation);
    }

  }

}
