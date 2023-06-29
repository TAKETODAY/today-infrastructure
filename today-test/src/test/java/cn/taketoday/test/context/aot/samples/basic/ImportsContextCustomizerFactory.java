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

package cn.taketoday.test.context.aot.samples.basic;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.ContextCustomizerFactory;
import cn.taketoday.test.context.MergedContextConfiguration;

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
    if (testClass.getName().startsWith("cn.taketoday.test.context.aot.samples") &&
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
