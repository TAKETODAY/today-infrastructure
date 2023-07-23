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

package cn.taketoday.framework.test.context;

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.ContextCustomizerFactory;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link ContextCustomizerFactory} to allow {@code @Import} annotations to be used
 * directly on test classes.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ImportsContextCustomizer
 * @since 4.0
 */
class ImportsContextCustomizerFactory implements ContextCustomizerFactory {

  @Override
  public ContextCustomizer createContextCustomizer(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributes) {
    if (AotDetector.useGeneratedArtifacts()) {
      return null;
    }

    AnnotationDescriptor<Import> descriptor = TestContextAnnotationUtils.findAnnotationDescriptor(
            testClass, Import.class);
    if (descriptor != null) {
      assertHasNoBeanMethods(descriptor.getRootDeclaringClass());
      return new ImportsContextCustomizer(descriptor.getRootDeclaringClass());
    }
    return null;
  }

  private void assertHasNoBeanMethods(Class<?> testClass) {
    ReflectionUtils.doWithMethods(testClass, this::assertHasNoBeanMethods);
  }

  private void assertHasNoBeanMethods(Method method) {
    Assert.state(!MergedAnnotations.from(method).isPresent(Bean.class),
            "Test classes cannot include @Bean methods");
  }

}
