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

package cn.taketoday.framework.test.context.filter;

import java.io.IOException;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.annotation.config.TypeExcludeFilter;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.framework.test.context.TestComponent;

/**
 * {@link TypeExcludeFilter} to exclude classes annotated with
 * {@link TestComponent @TestComponent} as well as inner-classes of tests.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class TestTypeExcludeFilter extends TypeExcludeFilter {

  private static final String BEAN_NAME = TestTypeExcludeFilter.class.getName();

  private static final String[] CLASS_ANNOTATIONS = {
          "org.junit.runner.RunWith",
          "org.junit.jupiter.api.extension.ExtendWith",
          "org.junit.platform.commons.annotation.Testable",
          "org.testng.annotations.Test"
  };

  private static final String[] METHOD_ANNOTATIONS = {
          "org.junit.Test",
          "org.junit.platform.commons.annotation.Testable",
          "org.testng.annotations.Test"
  };

  private static final TestTypeExcludeFilter INSTANCE = new TestTypeExcludeFilter();

  @Override
  public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
          throws IOException {
    if (isTestConfiguration(metadataReader)) {
      return true;
    }
    if (isTestClass(metadataReader)) {
      return true;
    }
    String enclosing = metadataReader.getClassMetadata().getEnclosingClassName();
    if (enclosing != null) {
      try {
        if (match(metadataReaderFactory.getMetadataReader(enclosing), metadataReaderFactory)) {
          return true;
        }
      }
      catch (Exception ex) {
        // Ignore
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj != null) && (getClass() == obj.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  private boolean isTestConfiguration(MetadataReader metadataReader) {
    return (metadataReader.getAnnotationMetadata().isAnnotated(TestComponent.class.getName()));
  }

  private boolean isTestClass(MetadataReader metadataReader) {
    for (String annotation : CLASS_ANNOTATIONS) {
      if (metadataReader.getAnnotationMetadata().hasAnnotation(annotation)) {
        return true;
      }

    }
    for (String annotation : METHOD_ANNOTATIONS) {
      if (metadataReader.getAnnotationMetadata().hasAnnotatedMethods(annotation)) {
        return true;
      }
    }
    return false;
  }

  static void registerWith(ConfigurableBeanFactory beanFactory) {
    if (!beanFactory.containsSingleton(BEAN_NAME)) {
      beanFactory.registerSingleton(BEAN_NAME, INSTANCE);
    }
  }

}
