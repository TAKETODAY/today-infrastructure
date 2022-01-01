/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanInstantiationException;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.aware.ResourceLoaderAware;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.ConstructorNotFoundException;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;

/**
 * Tests for {@link ParserStrategyUtils}.
 *
 * @author Phillip Webb
 */
public class ParserStrategyUtilsTests {

  @Mock
  private Environment environment;

  @Mock(extraInterfaces = BeanFactory.class)
  private BeanDefinitionRegistry registry;

  @Mock
  private ClassLoader beanClassLoader;

  @Mock
  private PatternResourceLoader resourceLoader;

  @Mock
  DefinitionLoadingContext loadingContext;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    given(this.loadingContext.getRegistry()).willReturn(this.registry);
    given(this.loadingContext.getEnvironment()).willReturn(this.environment);
    given(this.loadingContext.getResourceLoader()).willReturn(resourceLoader);
    given(this.loadingContext.getBeanFactory()).willReturn((BeanFactory) this.registry);
    given(this.loadingContext.getClassLoader()).willReturn(beanClassLoader);
    given(this.resourceLoader.getClassLoader()).willReturn(this.beanClassLoader);
  }

  @Test
  public void instantiateClassWhenHasNoArgsConstructorCallsAware() {
    NoArgsConstructor instance = instantiateClass(NoArgsConstructor.class);
    assertThat(instance.setEnvironment).isSameAs(this.environment);
    assertThat(instance.setBeanFactory).isSameAs(this.registry);
    assertThat(instance.setBeanClassLoader).isSameAs(this.beanClassLoader);
    assertThat(instance.setResourceLoader).isSameAs(this.resourceLoader);
  }

  @Test
  public void instantiateClassWhenHasSingleContructorInjectsParams() {
    ArgsConstructor instance = instantiateClass(ArgsConstructor.class);
    assertThat(instance.environment).isSameAs(this.environment);
    assertThat(instance.beanFactory).isSameAs(this.registry);
    assertThat(instance.beanClassLoader).isSameAs(this.beanClassLoader);
    assertThat(instance.resourceLoader).isSameAs(this.resourceLoader);
  }

  @Test
  public void instantiateClassWhenHasSingleContructorAndAwareInjectsParamsAndCallsAware() {
    ArgsConstructorAndAware instance = instantiateClass(ArgsConstructorAndAware.class);
    assertThat(instance.environment).isSameAs(this.environment);
    assertThat(instance.setEnvironment).isSameAs(this.environment);
    assertThat(instance.beanFactory).isSameAs(this.registry);
    assertThat(instance.setBeanFactory).isSameAs(this.registry);
    assertThat(instance.beanClassLoader).isSameAs(this.beanClassLoader);
    assertThat(instance.setBeanClassLoader).isSameAs(this.beanClassLoader);
    assertThat(instance.resourceLoader).isSameAs(this.resourceLoader);
    assertThat(instance.setResourceLoader).isSameAs(this.resourceLoader);
  }

  @Test
  public void instantiateClassWhenHasMultipleConstructorsUsesNoArgsConstructor() {
    // Remain back-compatible by using the default constructor if there's more then one
    MultipleConstructors instance = instantiateClass(MultipleConstructors.class);
    assertThat(instance.usedDefaultConstructor).isTrue();
  }

  @Test
  public void instantiateClassWhenHasMutlipleConstructorsAndNotDefaultThrowsException() {
    assertThatExceptionOfType(ConstructorNotFoundException.class).isThrownBy(() ->
            instantiateClass(MultipleConstructorsWithNoDefault.class));
  }

  @Test
  public void instantiateClassWhenHasUnsupportedParameterThrowsException() {
    assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() ->
                    instantiateClass(InvalidConstructorParameterType.class))
            .withCauseInstanceOf(IllegalStateException.class)
            .withMessageContaining("No suitable constructor found");
  }

  @Test
  public void instantiateClassHasSubclassParameterThrowsException() {
    // To keep the algorithm simple we don't support subtypes
    assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() ->
                    instantiateClass(InvalidConstructorParameterSubType.class))
            .withCauseInstanceOf(IllegalStateException.class)
            .withMessageContaining("No suitable constructor found");
  }

  @Test
  public void instantiateClassWhenHasNoBeanClassLoaderDoesNotCallAware() {
    reset(this.resourceLoader);
    NoArgsConstructor instance = instantiateClass(NoArgsConstructor.class);
    assertThat(instance.setBeanClassLoader).isNull();
    assertThat(instance.setBeanClassLoaderCalled).isFalse();
  }

  private <T> T instantiateClass(Class<T> clazz) {

    return ParserStrategyUtils.instantiateClass(clazz, clazz, loadingContext);
  }

  static class NoArgsConstructor
          implements BeanClassLoaderAware, BeanFactoryAware, EnvironmentAware, ResourceLoaderAware {

    Environment setEnvironment;

    BeanFactory setBeanFactory;

    ClassLoader setBeanClassLoader;

    boolean setBeanClassLoaderCalled;

    ResourceLoader setResourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
      this.setEnvironment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
      this.setBeanFactory = beanFactory;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
      this.setBeanClassLoader = classLoader;
      this.setBeanClassLoaderCalled = true;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
      this.setResourceLoader = resourceLoader;
    }

  }

  static class ArgsConstructor {

    final Environment environment;
    final BeanFactory beanFactory;
    final ClassLoader beanClassLoader;
    final ResourceLoader resourceLoader;

    ArgsConstructor(Environment environment, BeanFactory beanFactory,
                    ClassLoader beanClassLoader, ResourceLoader resourceLoader) {
      this.environment = environment;
      this.beanFactory = beanFactory;
      this.beanClassLoader = beanClassLoader;
      this.resourceLoader = resourceLoader;
    }

  }

  static class ArgsConstructorAndAware extends NoArgsConstructor {

    final Environment environment;

    final BeanFactory beanFactory;

    final ClassLoader beanClassLoader;

    final ResourceLoader resourceLoader;

    ArgsConstructorAndAware(Environment environment, BeanFactory beanFactory,
                            ClassLoader beanClassLoader, ResourceLoader resourceLoader) {
      this.environment = environment;
      this.beanFactory = beanFactory;
      this.beanClassLoader = beanClassLoader;
      this.resourceLoader = resourceLoader;
    }

  }

  static class MultipleConstructors {

    final boolean usedDefaultConstructor;

    MultipleConstructors() {
      this.usedDefaultConstructor = true;
    }

    MultipleConstructors(Environment environment) {
      this.usedDefaultConstructor = false;
    }

  }

  static class MultipleConstructorsWithNoDefault {

    MultipleConstructorsWithNoDefault(Environment environment, BeanFactory beanFactory) {
    }

    MultipleConstructorsWithNoDefault(Environment environment) {
    }

  }

  static class InvalidConstructorParameterType {

    InvalidConstructorParameterType(Environment environment, InputStream inputStream) {
    }

  }

  static class InvalidConstructorParameterSubType {

    InvalidConstructorParameterSubType(ConfigurableEnvironment environment) {
    }

  }

}
