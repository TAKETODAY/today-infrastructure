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

package infra.test.context.customizers;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.context.ConfigurableApplicationContext;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.MergedContextConfiguration;

/**
 * @author Sam Brannen
 * @since 4.0
 */
class GlobalFruitContextCustomizerFactory implements ContextCustomizerFactory {

  @Override
  public ContextCustomizer createContextCustomizer(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributes) {

    if (testClass.isAnnotationPresent(CustomizeWithFruit.class)) {
      return new GlobalFruitContextCustomizer();
    }
    return null;
  }

}

class GlobalFruitContextCustomizer implements ContextCustomizer {

  @Override
  public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
    // Use "global$fruit" as the bean name instead of something simple like "fruit"
    // to avoid bean name clashes with any test that registers a bean named "fruit".
    context.getBeanFactory().registerSingleton("global$fruit", "apple, banana, cherry");
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other != null && getClass() == other.getClass()));
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

}
