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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andy Wilkinson
 */
class FactoryMethodResolutionTests {

  @Test
  void factoryMethodCanBeResolvedWithBeanMetadataCachingEnabled() {
    assertThatFactoryMethodCanBeResolved(true);
  }

  @Test
  void factoryMethodCanBeResolvedWithBeanMetadataCachingDisabled() {
    assertThatFactoryMethodCanBeResolved(false);
  }

  private void assertThatFactoryMethodCanBeResolved(boolean cache) {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.getBeanFactory().setCacheBeanMetadata(cache);
      context.register(ImportSelectorConfiguration.class);
      context.refresh();
      BeanDefinition definition = context.getBeanFactory().getMergedBeanDefinition("exampleBean");
      assertThat(((RootBeanDefinition) definition).getResolvedFactoryMethod()).isNotNull();
    }
  }

  @Configuration
  @Import(ExampleImportSelector.class)
  static class ImportSelectorConfiguration {
  }

  static class ExampleImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      return new String[] { TestConfiguration.class.getName() };
    }
  }

  @Configuration
  static class TestConfiguration {

    @Bean
    @ExampleAnnotation
    public ExampleBean exampleBean() {
      return new ExampleBean();
    }
  }

  static class ExampleBean {
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface ExampleAnnotation {
  }

}
