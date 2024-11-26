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

package infra.context.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;

import infra.beans.BeanInstantiationException;
import infra.beans.BeansException;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.BootstrapContext;
import infra.context.EnvironmentAware;
import infra.context.ResourceLoaderAware;
import infra.context.support.GenericApplicationContext;
import infra.core.ConstructorNotFoundException;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.io.PatternResourceLoader;
import infra.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

/**
 * @author Phillip Webb
 */
public class BootstrapContextInstantiateTests {
  final GenericApplicationContext context = new GenericApplicationContext();

  @Mock
  private ConfigurableEnvironment environment;

  private final StandardBeanFactory beanFactory = context.getBeanFactory();

  @Mock
  private ClassLoader beanClassLoader;

  @Mock
  private PatternResourceLoader resourceLoader;

  @Mock
  BootstrapContext loadingContext;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    given(resourceLoader.getClassLoader()).willReturn(this.beanClassLoader);

    beanFactory.setBeanClassLoader(beanClassLoader);

    loadingContext = new BootstrapContext(beanFactory, context);
    loadingContext.setResourceLoader(resourceLoader);
    loadingContext.setEnvironment(environment);
  }

  @Test
  public void instantiateClassWhenHasNoArgsConstructorCallsAware() {
    NoArgsConstructor instance = instantiateClass(NoArgsConstructor.class);
    assertThat(instance.setEnvironment).isSameAs(this.environment);
    assertThat(instance.setBeanFactory).isSameAs(this.beanFactory);
    assertThat(instance.setBeanClassLoader).isSameAs(this.beanClassLoader);
    assertThat(instance.setResourceLoader).isSameAs(this.resourceLoader);
  }

  @Test
  public void instantiateClassWhenHasSingleContructorInjectsParams() {
    ArgsConstructor instance = instantiateClass(ArgsConstructor.class);
    assertThat(instance.environment).isSameAs(this.environment);
    assertThat(instance.beanFactory).isSameAs(this.beanFactory);
    assertThat(instance.beanClassLoader).isSameAs(this.beanClassLoader);
    assertThat(instance.resourceLoader).isSameAs(this.resourceLoader);
  }

  @Test
  public void instantiateClassWhenHasSingleContructorAndAwareInjectsParamsAndCallsAware() {
    ArgsConstructorAndAware instance = instantiateClass(ArgsConstructorAndAware.class);

    assertThat(instance.environment).isSameAs(this.environment);
    assertThat(instance.setEnvironment).isSameAs(this.environment);
    assertThat(instance.beanFactory).isSameAs(this.beanFactory);
    assertThat(instance.setBeanFactory).isSameAs(this.beanFactory);
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
  public void instantiateClassHasSubclassParameter() {
    var instantiated = instantiateClass(InvalidConstructorParameterSubType.class);
    assertThat(instantiated.environment).isSameAs(environment);
  }

  private <T> T instantiateClass(Class<T> clazz) {
    return loadingContext.instantiate(clazz, clazz);
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

    private final ConfigurableEnvironment environment;

    InvalidConstructorParameterSubType(ConfigurableEnvironment environment) {
      this.environment = environment;
    }

  }

}
