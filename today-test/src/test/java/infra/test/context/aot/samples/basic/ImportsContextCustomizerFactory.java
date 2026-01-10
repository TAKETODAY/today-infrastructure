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

package infra.test.context.aot.samples.basic;

import java.util.Arrays;
import java.util.List;

import infra.aot.AotDetector;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.Import;
import infra.context.support.GenericApplicationContext;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.MergedContextConfiguration;

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
