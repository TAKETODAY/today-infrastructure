/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.test.context.aot.samples.basic;

import infra.aot.AotDetector;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.Import;
import infra.context.support.GenericApplicationContext;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.MergedContextConfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * Emulates {@code ImportsContextCustomizerFactory} from Infra testing support.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class ImportsContextCustomizerFactory implements ContextCustomizerFactory {

  @Override
  public ContextCustomizer createContextCustomizer(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributes) {

    if (AotDetector.useGeneratedArtifacts()) {
      return null;
    }
    if (testClass.getName().startsWith("infra.test.context.aot.samples") &&
            testClass.isAnnotationPresent(Import.class)) {
      return new ImportsContextCustomizer(testClass);
    }
    return null;
  }

  /**
   * Emulates {@code ImportsContextCustomizer} from Infra testing support.
   */
  private static class ImportsContextCustomizer implements ContextCustomizer {

    private final Class<?> testClass;

    ImportsContextCustomizer(Class<?> testClass) {
      this.testClass = testClass;
    }

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
      AnnotatedBeanDefinitionReader annotatedBeanDefinitionReader =
              new AnnotatedBeanDefinitionReader((GenericApplicationContext) context);
      Arrays.stream(this.testClass.getAnnotation(Import.class).value())
              .forEach(annotatedBeanDefinitionReader::register);
    }
  }

}
