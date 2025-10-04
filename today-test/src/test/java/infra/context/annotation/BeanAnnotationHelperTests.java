/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.util.Objects;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.BeanNameGenerator;
import infra.beans.factory.support.StandardBeanFactory;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/3 22:08
 */
class BeanAnnotationHelperTests {

  @BeforeEach
  void clearCache() {
    BeanAnnotationHelper.clearCaches();
  }

  @Test
  void determineBeanNameWhenNoGeneratorAndNoBeanName() {
    String beanName = BeanAnnotationHelper.determineBeanNameFor(
            sampleMethod("beanWithoutName"), createBeanFactoryWithBeanNameGenerator(null));
    assertThat(beanName).isEqualTo("beanWithoutName");
  }

  @ParameterizedTest
  @ValueSource(strings = { "beanWithName", "beanWithMultipleNames" })
  void determineBeanNameWhenNoGeneratorAndBeanName(String methodName) {
    String beanName = BeanAnnotationHelper.determineBeanNameFor(
            sampleMethod(methodName), createBeanFactoryWithBeanNameGenerator(null));
    assertThat(beanName).isEqualTo("specificName");
  }

  @Test
  void determineBeanNameWhenBeanNameGeneratorAndNoBeanName() {
    BeanNameGenerator beanNameGenerator = mock(BeanNameGenerator.class);
    String beanName = BeanAnnotationHelper.determineBeanNameFor(
            sampleMethod("beanWithoutName"), createBeanFactoryWithBeanNameGenerator(beanNameGenerator));
    assertThat(beanName).isEqualTo("beanWithoutName");
    verifyNoInteractions(beanNameGenerator);
  }

  @ParameterizedTest
  @ValueSource(strings = { "beanWithName", "beanWithMultipleNames" })
  void determineBeanNameWhenBeanNameGeneratorAndBeanName(String methodName) {
    BeanNameGenerator beanNameGenerator = mock(BeanNameGenerator.class);
    String beanName = BeanAnnotationHelper.determineBeanNameFor(
            sampleMethod(methodName), createBeanFactoryWithBeanNameGenerator(beanNameGenerator));
    assertThat(beanName).isEqualTo("specificName");
    verifyNoInteractions(beanNameGenerator);
  }

  @Test
  void determineBeanNameWhenConfigurationBeanNameGeneratorAndNoBeanName() {
    ConfigurationBeanNameGenerator beanNameGenerator = mock(ConfigurationBeanNameGenerator.class);
    when(beanNameGenerator.deriveBeanName(any(), isNull())).thenReturn("generatedBeanName");
    String beanName = BeanAnnotationHelper.determineBeanNameFor(
            sampleMethod("beanWithoutName"), createBeanFactoryWithBeanNameGenerator(beanNameGenerator));
    assertThat(beanName).isEqualTo("generatedBeanName");
    verify(beanNameGenerator).deriveBeanName(any(), isNull());
  }

  @ParameterizedTest
  @ValueSource(strings = { "beanWithName", "beanWithMultipleNames" })
  void determineBeanNameWhenConfigurationBeanNameGeneratorAndBeanName(String methodName) {
    ConfigurationBeanNameGenerator beanNameGenerator = mock(ConfigurationBeanNameGenerator.class);
    given(beanNameGenerator.deriveBeanName(any(), eq("specificName"))).willReturn("generatedBeanName");
    String beanName = BeanAnnotationHelper.determineBeanNameFor(
            sampleMethod(methodName), createBeanFactoryWithBeanNameGenerator(beanNameGenerator));
    assertThat(beanName).isEqualTo("generatedBeanName");
    verify(beanNameGenerator).deriveBeanName(any(), eq("specificName"));
  }

  @Test
  void determineBeanNameInCacheWhenNoGeneratorAndNoBeanName() {
    Method method = sampleMethod("beanWithoutName");
    ConfigurableBeanFactory beanFactory = createBeanFactoryWithBeanNameGenerator(null);
    String beanName = BeanAnnotationHelper.determineBeanNameFor(method, beanFactory);
    assertThat(BeanAnnotationHelper.determineBeanNameFor(method, beanFactory)).isEqualTo(beanName);
  }

  @ParameterizedTest
  @ValueSource(strings = { "beanWithName", "beanWithMultipleNames" })
  void determineBeanNameInCacheWhenNoGeneratorAndBeanName(String methodName) {
    Method method = sampleMethod(methodName);
    ConfigurableBeanFactory beanFactory = createBeanFactoryWithBeanNameGenerator(null);
    String beanName = BeanAnnotationHelper.determineBeanNameFor(method, beanFactory);
    assertThat(BeanAnnotationHelper.determineBeanNameFor(method, beanFactory)).isEqualTo(beanName);
  }

  @Test
  void determineBeanNameInCacheWhenBeanNameGeneratorAndNoBeanName() {
    BeanNameGenerator beanNameGenerator = mock(BeanNameGenerator.class);
    Method method = sampleMethod("beanWithoutName");
    ConfigurableBeanFactory beanFactory = createBeanFactoryWithBeanNameGenerator(beanNameGenerator);
    String beanName = BeanAnnotationHelper.determineBeanNameFor(method, beanFactory);
    assertThat(BeanAnnotationHelper.determineBeanNameFor(method, beanFactory)).isEqualTo(beanName);
    verifyNoInteractions(beanNameGenerator);
  }

  @ParameterizedTest
  @ValueSource(strings = { "beanWithName", "beanWithMultipleNames" })
  void determineBeanNameInCacheWhenBeanNameGeneratorAndBeanName(String methodName) {
    BeanNameGenerator beanNameGenerator = mock(BeanNameGenerator.class);
    Method method = sampleMethod(methodName);
    ConfigurableBeanFactory beanFactory = createBeanFactoryWithBeanNameGenerator(beanNameGenerator);
    String beanName = BeanAnnotationHelper.determineBeanNameFor(method, beanFactory);
    assertThat(BeanAnnotationHelper.determineBeanNameFor(method, beanFactory)).isEqualTo(beanName);
    verifyNoInteractions(beanNameGenerator);
  }

  @Test
  void determineBeanNameInCacheWhenConfigurationBeanNameGeneratorAndNoBeanName() {
    ConfigurationBeanNameGenerator beanNameGenerator = mock(ConfigurationBeanNameGenerator.class);
    when(beanNameGenerator.deriveBeanName(any(), isNull()))
            .thenReturn("generatedBeanName").thenReturn("generatedBeanName");
    Method method = sampleMethod("beanWithoutName");
    ConfigurableBeanFactory beanFactory = createBeanFactoryWithBeanNameGenerator(beanNameGenerator);
    String beanName = BeanAnnotationHelper.determineBeanNameFor(method, beanFactory);
    assertThat(BeanAnnotationHelper.determineBeanNameFor(method, beanFactory)).isEqualTo(beanName);
    verify(beanNameGenerator, times(2)).deriveBeanName(any(), isNull());
  }

  @ParameterizedTest
  @ValueSource(strings = { "beanWithName", "beanWithMultipleNames" })
  void determineBeanNameInCacheWhenConfigurationBeanNameGeneratorAndBeanName(String methodName) {
    ConfigurationBeanNameGenerator beanNameGenerator = mock(ConfigurationBeanNameGenerator.class);
    given(beanNameGenerator.deriveBeanName(any(), eq("specificName")))
            .willReturn("generatedBeanName").willReturn("generatedBeanName");
    Method method = sampleMethod(methodName);
    ConfigurableBeanFactory beanFactory = createBeanFactoryWithBeanNameGenerator(beanNameGenerator);
    String beanName = BeanAnnotationHelper.determineBeanNameFor(method, beanFactory);
    assertThat(BeanAnnotationHelper.determineBeanNameFor(method, beanFactory)).isEqualTo(beanName);
    verify(beanNameGenerator, times(2)).deriveBeanName(any(), eq("specificName"));
  }

  private static Method sampleMethod(String name) {
    return Objects.requireNonNull(ReflectionUtils.findMethod(Samples.class, name));
  }

  private static ConfigurableBeanFactory createBeanFactoryWithBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
    ConfigurableBeanFactory beanFactory = new StandardBeanFactory();
    if (beanNameGenerator != null) {
      beanFactory.registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
    }
    return beanFactory;
  }

  static class Samples {

    @Bean
    private void beanWithoutName() {
    }

    @Bean(name = "specificName")
    private void beanWithName() {
    }

    @Bean(name = { "specificName", "specificName2", "specificName3" })
    private void beanWithMultipleNames() {
    }

  }
}