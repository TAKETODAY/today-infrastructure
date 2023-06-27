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

package cn.taketoday.framework.test.context;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.stereotype.Component;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/27 21:37
 */
class ImportsContextCustomizerFactoryTests {

  private final ImportsContextCustomizerFactory factory = new ImportsContextCustomizerFactory();

  @Test
  void getContextCustomizerWhenHasNoImportAnnotationShouldReturnNull() {
    ContextCustomizer customizer = this.factory.createContextCustomizer(TestWithNoImport.class, null);
    assertThat(customizer).isNull();
  }

  @Test
  void getContextCustomizerWhenHasImportAnnotationShouldReturnCustomizer() {
    ContextCustomizer customizer = this.factory.createContextCustomizer(TestWithImport.class, null);
    assertThat(customizer).isNotNull();
  }

  @Test
  void getContextCustomizerWhenHasMetaImportAnnotationShouldReturnCustomizer() {
    ContextCustomizer customizer = this.factory.createContextCustomizer(TestWithMetaImport.class, null);
    assertThat(customizer).isNotNull();
  }

  @Test
  void contextCustomizerEqualsAndHashCode() {
    ContextCustomizer customizer1 = this.factory.createContextCustomizer(TestWithImport.class, null);
    ContextCustomizer customizer2 = this.factory.createContextCustomizer(TestWithImport.class, null);
    ContextCustomizer customizer3 = this.factory.createContextCustomizer(TestWithImportAndMetaImport.class, null);
    ContextCustomizer customizer4 = this.factory.createContextCustomizer(TestWithSameImportAndMetaImport.class,
            null);
    assertThat(customizer1).hasSameHashCodeAs(customizer1);
    assertThat(customizer1).hasSameHashCodeAs(customizer2);
    assertThat(customizer1).isEqualTo(customizer1).isEqualTo(customizer2).isNotEqualTo(customizer3);
    assertThat(customizer3).isEqualTo(customizer4);
  }

  @Test
  void getContextCustomizerWhenClassHasBeanMethodsShouldThrowException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.factory.createContextCustomizer(TestWithImportAndBeanMethod.class, null))
            .withMessageContaining("Test classes cannot include @Bean methods");
  }

  @Test
  void contextCustomizerImportsBeans() {
    ContextCustomizer customizer = this.factory.createContextCustomizer(TestWithImport.class, null);
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    customizer.customizeContext(context, mock(MergedContextConfiguration.class));
    context.refresh();
    assertThat(context.getBean(ImportedBean.class)).isNotNull();
  }

  @Test
  void selfAnnotatingAnnotationDoesNotCauseStackOverflow() {
    assertThat(this.factory.createContextCustomizer(TestWithImportAndSelfAnnotatingAnnotation.class, null))
            .isNotNull();
  }

  static class TestWithNoImport {

  }

  @Import(ImportedBean.class)
  static class TestWithImport {

  }

  @MetaImport
  static class TestWithMetaImport {

  }

  @MetaImport
  @Import(AnotherImportedBean.class)
  static class TestWithImportAndMetaImport {

  }

  @MetaImport
  @Import(AnotherImportedBean.class)
  static class TestWithSameImportAndMetaImport {

  }

  @Configuration(proxyBeanMethods = false)
  @Import(ImportedBean.class)
  static class TestWithImportAndBeanMethod {

    @Bean
    String bean() {
      return "bean";
    }

  }

  @SelfAnnotating
  @Import(ImportedBean.class)
  static class TestWithImportAndSelfAnnotatingAnnotation {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Import(ImportedBean.class)
  @interface MetaImport {

  }

  @Component
  static class ImportedBean {

  }

  @Component
  static class AnotherImportedBean {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @SelfAnnotating
  @interface SelfAnnotating {

  }

}