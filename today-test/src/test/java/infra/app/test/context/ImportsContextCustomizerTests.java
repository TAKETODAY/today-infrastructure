/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.context;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.spockframework.runtime.model.SpecMetadata;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Set;

import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.ImportSelector;
import infra.context.annotation.config.DeterminableImports;
import infra.core.type.AnnotationMetadata;
import spock.lang.Issue;
import spock.lang.Stepwise;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/27 21:37
 */
class ImportsContextCustomizerTests {

  @Test
  void importSelectorsCouldUseAnyAnnotations() {
    assertThat(new ImportsContextCustomizer(FirstImportSelectorAnnotatedClass.class))
            .isNotEqualTo(new ImportsContextCustomizer(SecondImportSelectorAnnotatedClass.class));
  }

  @Test
  void determinableImportSelector() {
    assertThat(new ImportsContextCustomizer(FirstDeterminableImportSelectorAnnotatedClass.class))
            .isEqualTo(new ImportsContextCustomizer(SecondDeterminableImportSelectorAnnotatedClass.class));
  }

  @Test
  void customizersForTestClassesWithDifferentSpockFrameworkAnnotationsAreEqual() {
    assertThat(new ImportsContextCustomizer(FirstSpockFrameworkAnnotatedTestClass.class))
            .isEqualTo(new ImportsContextCustomizer(SecondSpockFrameworkAnnotatedTestClass.class));
  }

  @Test
  void customizersForTestClassesWithDifferentSpockLangAnnotationsAreEqual() {
    assertThat(new ImportsContextCustomizer(FirstSpockLangAnnotatedTestClass.class))
            .isEqualTo(new ImportsContextCustomizer(SecondSpockLangAnnotatedTestClass.class));
  }

  @Test
  void customizersForTestClassesWithDifferentJUnitAnnotationsAreEqual() {
    assertThat(new ImportsContextCustomizer(FirstJUnitAnnotatedTestClass.class))
            .isEqualTo(new ImportsContextCustomizer(SecondJUnitAnnotatedTestClass.class));
  }

  @Import(TestImportSelector.class)
  @Indicator1
  static class FirstImportSelectorAnnotatedClass {

  }

  @Import(TestImportSelector.class)
  @Indicator2
  static class SecondImportSelectorAnnotatedClass {

  }

  @Import(TestDeterminableImportSelector.class)
  @Indicator1
  static class FirstDeterminableImportSelectorAnnotatedClass {

  }

  @Import(TestDeterminableImportSelector.class)
  @Indicator2
  static class SecondDeterminableImportSelectorAnnotatedClass {

  }

  @SpecMetadata(filename = "foo", line = 10)
  @Import(TestImportSelector.class)
  static class FirstSpockFrameworkAnnotatedTestClass {

  }

  @SpecMetadata(filename = "bar", line = 10)
  @Import(TestImportSelector.class)
  static class SecondSpockFrameworkAnnotatedTestClass {

  }

  @Stepwise
  @Import(TestImportSelector.class)
  static class FirstSpockLangAnnotatedTestClass {

  }

  @Issue("1234")
  @Import(TestImportSelector.class)
  static class SecondSpockLangAnnotatedTestClass {

  }

  @Nested
  @Import(TestImportSelector.class)
  class FirstJUnitAnnotatedTestClass {

  }

  @Tag("test")
  @Import(TestImportSelector.class)
  static class SecondJUnitAnnotatedTestClass {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Indicator1 {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Indicator2 {

  }

  static class TestImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importMetadata) {
      return new String[] {};
    }

  }

  static class TestDeterminableImportSelector implements ImportSelector, DeterminableImports {

    @Override
    public String[] selectImports(AnnotationMetadata importMetadata) {
      return new String[] { TestConfig.class.getName() };
    }

    @Override
    public Set<Object> determineImports(AnnotationMetadata metadata) {
      return Collections.singleton(TestConfig.class.getName());
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfig {

  }

}