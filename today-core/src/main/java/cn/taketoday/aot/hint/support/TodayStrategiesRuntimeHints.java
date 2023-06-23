/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.aot.hint.support;

import java.util.List;
import java.util.Map;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

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
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    for (String resourceLocation : RESOURCE_LOCATIONS) {
      registerHints(hints, classLoader, resourceLocation);
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
        logger.trace(LogMessage.format("Skipping factories for [%s]", factoryClassName));
      }
      return;
    }
    if (logger.isTraceEnabled()) {
      logger.trace(LogMessage.format("Processing factories for [%s]", factoryClassName));
    }
    hints.reflection().registerType(factoryClass, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    for (String implementationClassName : implementationClassNames) {
      Class<?> implementationType = resolveClassName(classLoader, implementationClassName);
      if (logger.isTraceEnabled()) {
        logger.trace(LogMessage.format("%s factory type [%s] and implementation [%s]",
                (implementationType != null ? "Processing" : "Skipping"), factoryClassName,
                implementationClassName));
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
