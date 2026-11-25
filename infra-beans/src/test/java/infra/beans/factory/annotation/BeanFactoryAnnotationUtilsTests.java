/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.beans.factory.support.AutowireCandidateQualifier;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;

import static infra.beans.factory.annotation.BeanFactoryAnnotationUtils.getQualifierValue;
import static infra.beans.factory.annotation.BeanFactoryAnnotationUtils.isQualifierMatch;
import static infra.beans.factory.annotation.BeanFactoryAnnotationUtils.qualifiedBeanOfType;
import static infra.beans.factory.annotation.BeanFactoryAnnotationUtils.qualifiedBeansOfType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class BeanFactoryAnnotationUtilsTests {

  @Test
  void matchesAliasAndExplicitDefinitionQualifier() {
    var factory = new StandardBeanFactory();
    var definition = new RootBeanDefinition(Sample.class);
    definition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "explicit"));
    factory.registerBeanDefinition("sample", definition);
    factory.registerAlias("sample", "alias");

    assertThat(qualifiedBeanOfType(factory, Sample.class, "alias")).isSameAs(factory.getBean("sample"));
    assertThat(qualifiedBeanOfType(factory, Sample.class, "explicit")).isSameAs(factory.getBean("sample"));
    assertThat(qualifiedBeansOfType(factory, Sample.class, "other")).isEmpty();
  }

  @Test
  void matchesFactoryMethodQualifierRatherThanImplementationQualifier() throws Exception {
    var factory = new StandardBeanFactory();
    var method = BeanFactoryAnnotationUtilsTests.class.getDeclaredMethod("create");
    var definition = new RootBeanDefinition(BeanFactoryAnnotationUtilsTests.class);
    definition.setUniqueFactoryMethodName("create");
    definition.setResolvedFactoryMethod(method);
    factory.registerBeanDefinition("created", definition);

    assertThat(qualifiedBeanOfType(factory, Sample.class, "method")).isSameAs(factory.getBean("created"));
    assertThat(isQualifierMatch("implementation"::equals, "created", factory)).isFalse();
    assertThat(getQualifierValue(method)).isEqualTo("method");
    assertThat(getQualifierValue(Sample.class)).isEqualTo("implementation");
    assertThat(getQualifierValue(Object.class)).isNull();
  }

  @Test
  void detectsAmbiguousMatchesAndReturnsAllQualifiedBeans() {
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition("first", new RootBeanDefinition(Sample.class));
    factory.registerBeanDefinition("second", new RootBeanDefinition(Sample.class));

    assertThat(qualifiedBeansOfType(factory, Sample.class, "implementation"))
            .containsEntry("first", factory.getBean("first", Sample.class))
            .containsEntry("second", factory.getBean("second", Sample.class)).hasSize(2);
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
            .isThrownBy(() -> qualifiedBeanOfType(factory, Sample.class, "implementation"));
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> qualifiedBeanOfType(factory, Sample.class, "missing"));
  }

  @Test
  void supportsNameFallbackForFactoriesWithNoEnumeratedCandidates() {
    BeanFactory factory = mock(BeanFactory.class);
    var sample = new Sample();
    given(factory.getBeanNamesForType(Sample.class)).willReturn(new String[0]);
    given(factory.containsBean("sample")).willReturn(true);
    given(factory.isTypeMatch("sample", Sample.class)).willReturn(true);
    given(factory.getBean("sample", Sample.class)).willReturn(sample);

    assertThat(qualifiedBeanOfType(factory, Sample.class, "sample")).isSameAs(sample);
    given(factory.isTypeMatch("sample", Sample.class)).willReturn(false);
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> qualifiedBeanOfType(factory, Sample.class, "sample"))
            .withMessageContaining("neither qualifier match nor bean name match");
    assertThat(isQualifierMatch("sample"::equals, "sample", null)).isTrue();
    assertThat(isQualifierMatch("other"::equals, "sample", null)).isFalse();
    given(factory.getAliases("sample")).willReturn(new String[0]);
    assertThat(isQualifierMatch("implementation"::equals, "sample", factory)).isFalse();
    given(factory.getType("sample")).willAnswer(invocation -> Sample.class);
    assertThat(isQualifierMatch("implementation"::equals, "sample", factory)).isTrue();
  }

  @Test
  void toleratesManuallyRegisteredSingletonWithoutDefinition() {
    var factory = new StandardBeanFactory();
    factory.registerSingleton("sample", new Sample());

    assertThat(isQualifierMatch("other"::equals, "sample", factory)).isFalse();
    assertThat(qualifiedBeanOfType(factory, Sample.class, "sample")).isSameAs(factory.getBean("sample"));
  }

  @Qualifier("method")
  static Sample create() {
    return new Sample();
  }

  @Qualifier("implementation")
  static class Sample { }

}
