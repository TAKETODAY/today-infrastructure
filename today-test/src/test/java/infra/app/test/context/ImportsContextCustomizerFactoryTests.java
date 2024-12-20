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

package infra.app.test.context;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.ComponentScan;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.stereotype.Component;
import infra.test.context.ContextCustomizer;
import infra.test.context.MergedContextConfiguration;

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
  void contextCustomizerEqualsAndHashCodeConsidersComponentScan() {
    ContextCustomizer customizer1 = this.factory
            .createContextCustomizer(TestWithImportAndComponentScanOfSomePackage.class, null);
    ContextCustomizer customizer2 = this.factory
            .createContextCustomizer(TestWithImportAndComponentScanOfSomePackage.class, null);
    ContextCustomizer customizer3 = this.factory
            .createContextCustomizer(TestWithImportAndComponentScanOfAnotherPackage.class, null);
    assertThat(customizer1.hashCode()).isEqualTo(customizer2.hashCode());
    assertThat(customizer1).isEqualTo(customizer2);
    assertThat(customizer3.hashCode()).isNotEqualTo(customizer2.hashCode()).isNotEqualTo(customizer1.hashCode());
    assertThat(customizer3).isNotEqualTo(customizer2).isNotEqualTo(customizer1);
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

  @Import(ImportedBean.class)
  @ComponentScan("some.package")
  static class TestWithImportAndComponentScanOfSomePackage {

  }

  @Import(ImportedBean.class)
  @ComponentScan("another.package")
  static class TestWithImportAndComponentScanOfAnotherPackage {

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