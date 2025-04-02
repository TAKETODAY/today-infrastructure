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

package infra.context;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import infra.beans.BeanInstantiationException;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionCustomizer;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.ExpressionEvaluator;
import infra.beans.factory.parsing.Problem;
import infra.beans.factory.parsing.ProblemReporter;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.BeanNameGenerator;
import infra.context.annotation.AnnotationBeanNameGenerator;
import infra.context.annotation.ConditionEvaluator;
import infra.context.annotation.ScopeMetadata;
import infra.context.annotation.ScopeMetadataResolver;
import infra.core.env.Environment;
import infra.core.env.StandardEnvironment;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.PatternResourceLoader;
import infra.core.io.ResourceLoader;
import infra.core.type.AnnotationMetadata;
import infra.core.type.classreading.CachingMetadataReaderFactory;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 20:01
 */
class BootstrapContextTests {

  @Test
  void customEnvironmentIsReturnedWhenSet() {
    Environment mockEnv = mock(Environment.class);
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(null, factory);
    context.setEnvironment(mockEnv);

    assertThat(context.getEnvironment()).isSameAs(mockEnv);
  }

  @Test
  void nullEnvironmentCreatesStandardEnvironment() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(null, factory);

    assertThat(context.getEnvironment()).isInstanceOf(StandardEnvironment.class);
  }

  @Test
  void applicationContextEnvironmentIsUsedIfAvailable() {
    Environment mockEnv = mock(Environment.class);
    ApplicationContext appContext = mock(ApplicationContext.class);
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);

    when(appContext.getEnvironment()).thenReturn(mockEnv);
    when(appContext.unwrapFactory(ConfigurableBeanFactory.class)).thenReturn(factory);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(appContext);

    assertThat(context.getEnvironment()).isSameAs(mockEnv);
  }

  @Test
  void properInstantiationWithApplicationContext() {
    ApplicationContext appContext = mock(ApplicationContext.class);
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);

    when(appContext.unwrapFactory(ConfigurableBeanFactory.class)).thenReturn(factory);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(appContext);

    assertThat(context.getRegistry()).isSameAs(registry);
    assertThat(context.getBeanFactory()).isSameAs(factory);
    assertThat(context.getApplicationContext()).isSameAs(appContext);
  }

  @Test
  void properInstantiationWithBeanFactory() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);

    assertThat(context.getRegistry()).isSameAs(registry);
    assertThat(context.getBeanFactory()).isSameAs(factory);
    assertThat(context.getApplicationContext()).isNull();
  }

  @Test
  void beanRegistrationWithCustomization() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var definition = mock(BeanDefinition.class);
    var customizer = mock(BeanDefinitionCustomizer.class);

    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setCustomizers(customizer);
    context.registerBeanDefinition("testBean", definition);

    verify(customizer).customize(definition);
    verify(registry).registerBeanDefinition("testBean", definition);
  }

  @Test
  void beanRegistrationHandlesScopeProperly() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var definition = mock(BeanDefinition.class);
    var scopeResolver = mock(ScopeMetadataResolver.class);
    var scopeMetadata = new ScopeMetadata();
    scopeMetadata.setScopeName("singleton");

    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(scopeResolver.resolveScopeMetadata(definition)).thenReturn(scopeMetadata);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setScopeMetadataResolver(scopeResolver);
    context.registerBeanDefinition("testBean", definition);

    verify(definition).setScope("singleton");
    verify(registry).registerBeanDefinition("testBean", definition);
  }

  @Test
  void defaultBeanNameGeneratorIsAnnotationBased() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);

    assertThat(context.getBeanNameGenerator()).isSameAs(AnnotationBeanNameGenerator.INSTANCE);
  }

  @Test
  void instantiateClassWithConstructorParameters() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    TestComponent component = context.instantiate(TestComponent.class);

    assertThat(component.getEnvironment()).isSameAs(context.getEnvironment());
    assertThat(component.getBeanFactory()).isSameAs(factory);
    assertThat(component.getBootstrapContext()).isSameAs(context);
  }

  @Test
  void resourceLoaderIsProperlyInitialized() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    assertThat(context.getResourceLoader()).isNotNull();
  }

  @Test
  void metadataReaderFactoryIsProperlyInitialized() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    assertThat(context.getMetadataReaderFactory()).isNotNull();
  }

  @Test
  void propertySourceFactoryIsProperlyInitialized() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    assertThat(context.getPropertySourceFactory()).isNotNull();
  }

  @Test
  void clearCacheRemovesAllCachedMetadata() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var cachingFactory = mock(CachingMetadataReaderFactory.class);
    var evaluator = mock(ConditionEvaluator.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setMetadataReaderFactory(cachingFactory);
    context.conditionEvaluator = evaluator;

    context.clearCache();

    verify(cachingFactory).clearCache();
    verify(evaluator).clearCache();
  }

  @Test
  void expressionEvaluatorFallsBackToApplicationContext() {
    var appContext = mock(ApplicationContext.class);
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var evaluator = mock(ExpressionEvaluator.class);

    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(appContext.unwrapFactory(ConfigurableBeanFactory.class)).thenReturn(factory);
    when(appContext.getExpressionEvaluator()).thenReturn(evaluator);

    BootstrapContext context = new BootstrapContext(appContext);

    assertThat(context.getExpressionEvaluator()).isSameAs(evaluator);
  }

  @Test
  void customProblemReporterIsUsedForErrors() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var reporter = mock(ProblemReporter.class);
    var problem = mock(Problem.class);

    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setProblemReporter(reporter);
    context.reportError(problem);

    verify(reporter).error(problem);
  }

  @Test
  void classLoaderFallsBackToResourceLoaderWhenBeanFactoryReturnsNull() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var resourceLoader = mock(ResourceLoader.class);
    var classLoader = mock(ClassLoader.class);

    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(factory.getBeanClassLoader()).thenReturn(null);
    when(resourceLoader.getClassLoader()).thenReturn(classLoader);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setResourceLoader(resourceLoader);

    assertThat(context.getClassLoader()).isSameAs(classLoader);
  }

  @Test
  void cachingMetadataReaderFactoryIsCreatedWithResourceLoader() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var resourceLoader = mock(ResourceLoader.class);

    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setResourceLoader(resourceLoader);

    assertThat(context.getMetadataReaderFactory()).isInstanceOf(CachingMetadataReaderFactory.class);
  }

  @Test
  void instantiateWithAssignableTypeChecksSuperType() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    TestComponent component = context.instantiate(TestSubComponent.class, TestComponent.class);

    assertThat(component).isInstanceOf(TestSubComponent.class);
  }

  @Test
  void evaluateExpressionThrowsExceptionWhenInvalid() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var evaluator = mock(ExpressionEvaluator.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(evaluator.evaluate("invalid", String.class)).thenThrow(IllegalStateException.class);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.expressionEvaluator = evaluator;

    assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> context.evaluateExpression("invalid", String.class));
  }

  @Test
  void instantiateWithInvalidConstructorParameterThrowsException() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);

    assertThatExceptionOfType(BeanInstantiationException.class)
            .isThrownBy(() -> context.instantiate(InvalidComponent.class));
  }

  @Test
  void resolveScopeMetadataHandlesNullBeanDefinition() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var resolver = mock(ScopeMetadataResolver.class);
    var metadata = new ScopeMetadata();

    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(resolver.resolveScopeMetadata(null)).thenReturn(metadata);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setScopeMetadataResolver(resolver);

    ScopeMetadata result = context.resolveScopeMetadata(null);
    assertThat(result).isSameAs(metadata);
  }

  @Test
  void classLoaderFallbackChain() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var resourceLoader = mock(ResourceLoader.class);
    var factoryClassLoader = mock(ClassLoader.class);
    var resourceClassLoader = mock(ClassLoader.class);

    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(factory.getBeanClassLoader()).thenReturn(factoryClassLoader);
    when(resourceLoader.getClassLoader()).thenReturn(resourceClassLoader);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setResourceLoader(resourceLoader);

    assertThat(context.getClassLoader()).isSameAs(factoryClassLoader);

    when(factory.getBeanClassLoader()).thenReturn(null);
    assertThat(context.getClassLoader()).isSameAs(resourceClassLoader);
  }

  @Test
  void evaluateExpressionWithBindingVariables() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var evaluator = mock(ExpressionEvaluator.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(evaluator.evaluate("${foo}", String.class)).thenReturn("bar");

    BootstrapContext context = new BootstrapContext(factory, null);
    context.expressionEvaluator = evaluator;

    String result = context.evaluateExpression("${foo}", String.class);
    assertThat(result).isEqualTo("bar");
  }

  @Test
  void unwrapFactoryDelegatesToApplicationContext() {
    var appContext = mock(ApplicationContext.class);
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(appContext.unwrapFactory(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(appContext.unwrapFactory(ConfigurableBeanFactory.class)).thenReturn(factory);

    BootstrapContext context = new BootstrapContext(appContext);
    var result = context.unwrapFactory(BeanDefinitionRegistry.class);

    assertThat(result).isSameAs(registry);
    verify(appContext).unwrapFactory(BeanDefinitionRegistry.class);
  }

  @Test
  void registerAliasWithValidNames() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.registerAlias("originalBean", "aliasName");

    verify(registry).registerAlias("originalBean", "aliasName");
  }

  @Test
  void removeBeanDefinitionDelegates() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.removeBeanDefinition("testBean");

    verify(registry).removeBeanDefinition("testBean");
  }

  @Test
  void getBeanDefinitionDelegates() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var definition = mock(BeanDefinition.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(registry.getBeanDefinition("testBean")).thenReturn(definition);

    BootstrapContext context = new BootstrapContext(factory, null);
    BeanDefinition result = context.getBeanDefinition("testBean");

    assertThat(result).isSameAs(definition);
  }

  @Test
  void containsBeanDefinitionDelegates() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(registry.containsBeanDefinition("testBean")).thenReturn(true);

    BootstrapContext context = new BootstrapContext(factory, null);
    boolean result = context.containsBeanDefinition("testBean");

    assertThat(result).isTrue();
  }

  @Test
  void resourceLoaderFromPatternResourceLoader() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var customLoader = mock(PathMatchingPatternResourceLoader.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setResourceLoader(customLoader);

    assertThat(context.getResourceLoader()).isSameAs(customLoader);
  }

  @Test
  void instantiateWithMetadataReaderFactoryParameter() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var metadataFactory = mock(MetadataReaderFactory.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setMetadataReaderFactory(metadataFactory);

    TestMetadataComponent component = context.instantiate(TestMetadataComponent.class);
    assertThat(component.getMetadataFactory()).isSameAs(metadataFactory);
  }

  @Test
  void deduceContextFromBeanFactory() {
    var appContext = mock(ApplicationContext.class);
    var factory = mock(ConfigurableBeanFactory.class);

    when(factory.unwrap(ConfigurableBeanFactory.class)).thenReturn(factory);
    when(appContext.unwrapFactory(ConfigurableBeanFactory.class)).thenReturn(factory);

    assertThat(BootstrapContext.deduceContext(appContext)).isSameAs(appContext);
    assertThat(BootstrapContext.deduceContext(factory)).isNull();
  }

  @Test
  void getAnnotationMetadataReadsFromFactory() throws IOException {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var metadataFactory = mock(MetadataReaderFactory.class);
    var metadataReader = mock(MetadataReader.class);
    var metadata = mock(AnnotationMetadata.class);

    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(metadataFactory.getMetadataReader("test.Class")).thenReturn(metadataReader);
    when(metadataReader.getAnnotationMetadata()).thenReturn(metadata);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setMetadataReaderFactory(metadataFactory);

    assertThat(context.getAnnotationMetadata("test.Class")).isSameAs(metadata);
  }

  @Test
  @SuppressWarnings("cast")
  void evaluateExpressionHandlesNullExpressionAndType() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var evaluator = mock(ExpressionEvaluator.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(evaluator.evaluate(null, null)).thenReturn(null);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.expressionEvaluator = evaluator;

    assertThat((Object) context.evaluateExpression(null, null)).isNull();
  }

  @Test
  void instantiateClassWithMetadataReaderFactoryAware() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var metadataFactory = mock(MetadataReaderFactory.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setMetadataReaderFactory(metadataFactory);

    var instance = context.instantiate(MetadataReaderFactoryAwareBean.class);
    assertThat(instance.getMetadataReaderFactory()).isSameAs(metadataFactory);
  }

  @Test
  void instantiateClassWithResourceLoaderAware() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var resourceLoader = mock(PatternResourceLoader.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setResourceLoader(resourceLoader);

    var instance = context.instantiate(ResourceLoaderAwareBean.class);
    assertThat(instance.getResourceLoader()).isSameAs(resourceLoader);
  }

  @Test
  void environmentAwareComponentReceivesEnvironment() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var environment = new StandardEnvironment();
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setEnvironment(environment);

    var instance = context.instantiate(EnvironmentAwareBean.class);
    assertThat(instance.getEnvironment()).isSameAs(environment);
  }

  @Test
  void multipleAwareInterfacesAreInvokedInOrder() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var environment = mock(Environment.class);
    var resourceLoader = mock(ResourceLoader.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setEnvironment(environment);
    context.setResourceLoader(resourceLoader);

    MultiAwareBean bean = context.instantiate(MultiAwareBean.class);

    assertThat(bean.getInvocationOrder())
            .containsExactly("BeanClassLoaderAware", "BeanFactoryAware", "EnvironmentAware", "ResourceLoaderAware");
  }

  @Test
  void evaluateExpressionWithNullEvaluatorFallsBackToFactoryUsingUnwrap() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(factory.unwrap(ConfigurableBeanFactory.class)).thenReturn(factory);
    when(factory.resolveEmbeddedValue("${test}")).thenReturn("resolved");

    BootstrapContext context = new BootstrapContext(factory, null);
    String result = context.evaluateExpression("${test}");

    assertThat(result).isEqualTo("resolved");
  }

  @Test
  void instantiateWithMultipleConstructorParametersInDifferentOrder() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var environment = mock(Environment.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setEnvironment(environment);

    MultiParamComponent component = context.instantiate(MultiParamComponent.class);

    assertThat(component.getRegistry()).isSameAs(registry);
    assertThat(component.getEnvironment()).isSameAs(environment);
    assertThat(component.getBootstrapContext()).isSameAs(context);
  }

  @Test
  void setResourceLoaderUpdatesMetadataReaderFactory() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var resourceLoader = mock(ResourceLoader.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    MetadataReaderFactory originalFactory = context.getMetadataReaderFactory();
    context.setResourceLoader(resourceLoader);

    assertThat(context.getMetadataReaderFactory()).isNotSameAs(originalFactory);
  }

  @Test
  void unwrapContextThrowsExceptionWhenNoApplicationContext() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);

    assertThatIllegalStateException()
            .isThrownBy(() -> context.unwrapContext(ApplicationContext.class))
            .withMessage("No ApplicationContext available");
  }

  @Test
  void generateBeanNameDelegatesToConfiguredGenerator() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var generator = mock(BeanNameGenerator.class);
    var definition = mock(BeanDefinition.class);

    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);
    when(generator.generateBeanName(definition, registry)).thenReturn("generatedName");

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setBeanNameGenerator(generator);

    assertThat(context.generateBeanName(definition)).isEqualTo("generatedName");
  }

  @Test
  void instantiateHandlesInterfaceClass() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);

    assertThatExceptionOfType(BeanInstantiationException.class)
            .isThrownBy(() -> context.instantiate(EnvironmentAware.class))
            .withMessageContaining("Specified class is an interface");
  }

  @Test
  void obtainContextWithNullBeanFactoryThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> BootstrapContext.obtain(null))
            .withMessage("beanFactory is required");
  }

  @Test
  void multipleRegisteredCustomizersAreExecutedInOrder() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var definition = mock(BeanDefinition.class);
    var customizer1 = mock(BeanDefinitionCustomizer.class);
    var customizer2 = mock(BeanDefinitionCustomizer.class);
    var customizer3 = mock(BeanDefinitionCustomizer.class);

    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setCustomizers(customizer1, customizer2, customizer3);
    context.registerBeanDefinition("testBean", definition);

    var inOrder = inOrder(customizer1, customizer2, customizer3);
    inOrder.verify(customizer1).customize(definition);
    inOrder.verify(customizer2).customize(definition);
    inOrder.verify(customizer3).customize(definition);
  }

  @Test
  void nullCustomizerArrayDoesNotThrowException() {
    var factory = mock(ConfigurableBeanFactory.class);
    var registry = mock(BeanDefinitionRegistry.class);
    var definition = mock(BeanDefinition.class);
    when(factory.unwrap(BeanDefinitionRegistry.class)).thenReturn(registry);

    BootstrapContext context = new BootstrapContext(factory, null);
    context.setCustomizers((BeanDefinitionCustomizer[]) null);
    context.registerBeanDefinition("testBean", definition);

    verify(registry).registerBeanDefinition("testBean", definition);
  }

  private static class MultiParamComponent {
    private final Environment environment;
    private final BeanDefinitionRegistry registry;
    private final BootstrapContext bootstrapContext;

    MultiParamComponent(BeanDefinitionRegistry registry, Environment environment, BootstrapContext context) {
      this.registry = registry;
      this.environment = environment;
      this.bootstrapContext = context;
    }

    public Environment getEnvironment() {
      return environment;
    }

    public BeanDefinitionRegistry getRegistry() {
      return registry;
    }

    public BootstrapContext getBootstrapContext() {
      return bootstrapContext;
    }
  }

  private static class MultiAwareBean implements BeanClassLoaderAware, BeanFactoryAware,
          EnvironmentAware, ResourceLoaderAware {

    private final List<String> invocationOrder = new ArrayList<>();

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
      invocationOrder.add("BeanClassLoaderAware");
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
      invocationOrder.add("BeanFactoryAware");
    }

    @Override
    public void setEnvironment(Environment environment) {
      invocationOrder.add("EnvironmentAware");
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
      invocationOrder.add("ResourceLoaderAware");
    }

    public List<String> getInvocationOrder() {
      return invocationOrder;
    }
  }

  private static class MetadataReaderFactoryAwareBean {
    private final MetadataReaderFactory metadataReaderFactory;

    MetadataReaderFactoryAwareBean(MetadataReaderFactory factory) {
      this.metadataReaderFactory = factory;
    }

    public MetadataReaderFactory getMetadataReaderFactory() {
      return metadataReaderFactory;
    }
  }

  private static class ResourceLoaderAwareBean implements ResourceLoaderAware {
    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
      this.resourceLoader = resourceLoader;
    }

    public ResourceLoader getResourceLoader() {
      return resourceLoader;
    }
  }

  private static class EnvironmentAwareBean implements EnvironmentAware {
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
      this.environment = environment;
    }

    public Environment getEnvironment() {
      return environment;
    }
  }

  private static class TestMetadataComponent {

    private final MetadataReaderFactory metadataFactory;

    TestMetadataComponent(MetadataReaderFactory metadataFactory) {
      this.metadataFactory = metadataFactory;
    }

    public MetadataReaderFactory getMetadataFactory() {
      return metadataFactory;
    }
  }

  private static class InvalidComponent {
    InvalidComponent(String invalidParameter) { }
  }

  private static class TestSubComponent extends TestComponent {
    TestSubComponent(Environment environment, BeanFactory beanFactory, BootstrapContext bootstrapContext) {
      super(environment, beanFactory, bootstrapContext);
    }
  }

  private static class TestComponent {

    private final Environment environment;

    private final BeanFactory beanFactory;

    private final BootstrapContext bootstrapContext;

    TestComponent(Environment environment, BeanFactory beanFactory, BootstrapContext bootstrapContext) {
      this.environment = environment;
      this.beanFactory = beanFactory;
      this.bootstrapContext = bootstrapContext;
    }

    public Environment getEnvironment() {
      return environment;
    }

    public BeanFactory getBeanFactory() {
      return beanFactory;
    }

    public BootstrapContext getBootstrapContext() {
      return bootstrapContext;
    }
  }

}
