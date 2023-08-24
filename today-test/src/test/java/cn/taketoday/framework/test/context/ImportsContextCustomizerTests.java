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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.spockframework.runtime.model.SpecMetadata;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportSelector;
import cn.taketoday.context.annotation.config.DeterminableImports;
import cn.taketoday.core.type.AnnotationMetadata;
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
  static class FirstJUnitAnnotatedTestClass {

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
    public String[] selectImports(AnnotationMetadata arg0) {
      return new String[] {};
    }

  }

  static class TestDeterminableImportSelector implements ImportSelector, DeterminableImports {

    @Override
    public String[] selectImports(AnnotationMetadata arg0) {
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