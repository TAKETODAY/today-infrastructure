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

package cn.taketoday.test.context.customizers;

import java.util.List;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.ContextCustomizerFactory;
import cn.taketoday.test.context.MergedContextConfiguration;

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
