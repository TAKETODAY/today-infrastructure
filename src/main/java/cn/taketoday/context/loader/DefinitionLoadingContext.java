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

package cn.taketoday.context.loader;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Set;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanNamePopulator;
import cn.taketoday.beans.factory.SingletonBeanRegistry;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.support.BeanDefinitionCustomizers;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.DependencyInjectorAwareInstantiator;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotationBeanNamePopulator;
import cn.taketoday.context.annotation.AnnotationScopeMetadataResolver;
import cn.taketoday.context.annotation.ConditionEvaluator;
import cn.taketoday.context.annotation.ConfigurationCondition.ConfigurationPhase;
import cn.taketoday.context.annotation.FailFastProblemReporter;
import cn.taketoday.context.annotation.Problem;
import cn.taketoday.context.annotation.ProblemReporter;
import cn.taketoday.context.expression.ExpressionEvaluator;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.DefaultPropertySourceFactory;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.PropertySourceFactory;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.classreading.CachingMetadataReaderFactory;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ExceptionUtils;

/**
 * Startup Context
 *
 * @author TODAY 2021/10/19 22:22
 * @since 4.0
 */
@Experimental
public class DefinitionLoadingContext extends BeanDefinitionCustomizers {
  public static final String BEAN_NAME = "definitionLoadingContext";

  private final BeanDefinitionRegistry registry;
  private final ApplicationContext applicationContext;

  private ConditionEvaluator conditionEvaluator;
  private DependencyInjectorAwareInstantiator instantiator;
  private MetadataReaderFactory metadataReaderFactory;

  @Nullable
  private PatternResourceLoader resourceLoader;

  private ScopeMetadataResolver scopeMetadataResolver;
  private PropertySourceFactory propertySourceFactory;

  /* Using short class names as default bean names by default. */
  private BeanNamePopulator beanNamePopulator = AnnotationBeanNamePopulator.INSTANCE;

  private ProblemReporter problemReporter = new FailFastProblemReporter();

  public DefinitionLoadingContext(BeanDefinitionRegistry registry, @NonNull ApplicationContext context) {
    this.registry = registry;
    this.resourceLoader = context;
    this.applicationContext = context;
  }

  public DefinitionLoadingContext(
          BeanDefinitionRegistry registry,
          @Nullable ConditionEvaluator conditionEvaluator, @NonNull ApplicationContext context) {
    this.registry = registry;
    this.resourceLoader = context;
    this.applicationContext = context;
    this.conditionEvaluator = conditionEvaluator;
  }

  public BeanDefinitionRegistry getRegistry() {
    return registry;
  }

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  public Environment getEnvironment() {
    return applicationContext.getEnvironment();
  }

  public BeanFactory getBeanFactory() {
    return applicationContext.getBeanFactory();
  }

  /**
   * unwrap bean-factory to {@code requiredType}
   *
   * @throws IllegalArgumentException not a requiredType
   */
  @NonNull
  public <T> T unwrapFactory(Class<T> requiredType) {
    return applicationContext.unwrapFactory(requiredType);
  }

  /**
   * unwrap this ApplicationContext to {@code requiredType}
   *
   * @throws IllegalArgumentException not a requiredType
   */
  @NonNull
  public <T> T unwrapContext(Class<T> requiredType) {
    return applicationContext.unwrap(requiredType);
  }

  /**
   * Generate a bean name for the given bean definition.
   * <p>
   * use internal registry
   * </p>
   *
   * @param definition the bean definition to generate a name for
   * @return the generated bean name
   */
  public String populateName(BeanDefinition definition) {
    return beanNamePopulator.populateName(definition, registry);
  }

  @NonNull
  public ConditionEvaluator getConditionEvaluator() {
    if (conditionEvaluator == null) {
      this.conditionEvaluator = new ConditionEvaluator(applicationContext, registry);
    }
    return conditionEvaluator;
  }

  public void registerBeanDefinition(String beanName, BeanDefinition definition) {
    if (definition.getScope() == null) {
      definition.setScope(resolveScopeName(definition));
    }

    if (CollectionUtils.isNotEmpty(customizers)) {
      for (BeanDefinitionCustomizer definitionCustomizer : customizers) {
        definitionCustomizer.customize(definition);
      }
    }
    registry.registerBeanDefinition(beanName, definition);
  }

  public void registerAlias(String beanName, String alias) {
    registry.registerAlias(beanName, alias);
  }

  public void registerBeanDefinition(BeanDefinition definition) {
    registerBeanDefinition(definition.getBeanName(), definition);
  }

  public boolean containsBeanDefinition(Class<?> beanClass) {
    return registry.containsBeanDefinition(beanClass);
  }

  public boolean containsBeanDefinition(Class<?> type, boolean equals) {
    return registry.containsBeanDefinition(type, equals);
  }

  public boolean containsBeanDefinition(String beanName) {
    return registry.containsBeanDefinition(beanName);
  }

  public void removeBeanDefinition(String beanName) {
    registry.removeBeanDefinition(beanName);
  }

  public BeanDefinition getBeanDefinition(String beanName) {
    return registry.getBeanDefinition(beanName);
  }

  public boolean passCondition(AnnotatedElement annotated) {
    if (annotated instanceof Class) {
      return getConditionEvaluator().passCondition((Class<?>) annotated);
    }
    else if (annotated instanceof Method) {
      return getConditionEvaluator().passCondition((Method) annotated);
    }
    else {
      throw new IllegalArgumentException("AnnotatedElement must be Method or Class");
    }
  }

  public boolean passCondition(AnnotatedTypeMetadata metadata, @Nullable ConfigurationPhase phase) {
    return getConditionEvaluator().passCondition(metadata, phase);
  }

  public boolean shouldSkip(@Nullable AnnotatedTypeMetadata metadata, @Nullable ConfigurationPhase phase) {
    return getConditionEvaluator().shouldSkip(metadata, phase);
  }

  public boolean passCondition(AnnotatedTypeMetadata metadata) {
    return getConditionEvaluator().passCondition(metadata);
  }

  //---------------------------------------------------------------------
  // BeanFactoryAwareBeanInstantiator
  //---------------------------------------------------------------------

  private DependencyInjectorAwareInstantiator instantiator() {
    if (instantiator == null) {
      this.instantiator = DependencyInjectorAwareInstantiator.from(applicationContext);
    }
    return instantiator;
  }

  public <T> T instantiate(Class<T> beanClass) {
    return instantiator().instantiate(beanClass);
  }

  //---------------------------------------------------------------------
  // MetadataReaderFactory
  //---------------------------------------------------------------------

  /**
   * Set the {@link ResourceLoader} to use for resource locations.
   * This will typically be a {@link PatternResourceLoader} implementation.
   * <p>Default is a {@code PathMatchingResourcePatternResolver}, also capable of
   * resource pattern resolving through the {@code ResourcePatternResolver} interface.
   *
   * @see PatternResourceLoader
   * @see PathMatchingPatternResourceLoader
   */
  public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = PatternResourceLoader.fromResourceLoader(resourceLoader);
    this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
  }

  public PatternResourceLoader getResourceLoader() {
    if (this.resourceLoader == null) {
      this.resourceLoader = applicationContext;
      // try to bind ResourceLoader to MetadataReaderFactory
      if (this.metadataReaderFactory == null) {
        this.metadataReaderFactory = new CachingMetadataReaderFactory(applicationContext);
      }
    }
    return this.resourceLoader;
  }

  /**
   * Set the {@link MetadataReaderFactory} to use.
   * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
   * {@linkplain #setResourceLoader resource loader}.
   * <p>Call this setter method <i>after</i> {@link #setResourceLoader} in order
   * for the given MetadataReaderFactory to override the default factory.
   */
  public void setMetadataReaderFactory(@Nullable MetadataReaderFactory metadataReaderFactory) {
    this.metadataReaderFactory = metadataReaderFactory;
  }

  /**
   * Return the MetadataReaderFactory used by this component provider.
   */
  public final MetadataReaderFactory getMetadataReaderFactory() {
    if (this.metadataReaderFactory == null) {
      // try to bind ResourceLoader to MetadataReaderFactory
      if (this.resourceLoader == null) {
        this.metadataReaderFactory = new CachingMetadataReaderFactory(applicationContext);
        this.resourceLoader = applicationContext;
      }
      else {
        ResourceLoader resourceLoader;
        if (this.resourceLoader instanceof PathMatchingPatternResourceLoader) {
          resourceLoader = ((PathMatchingPatternResourceLoader) this.resourceLoader).getRootLoader();
        }
        else {
          resourceLoader = this.resourceLoader;
        }
        this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
      }
    }
    return this.metadataReaderFactory;
  }

  @NonNull
  public AnnotationMetadata getAnnotationMetadata(String className) {
    return getMetadataReader(className).getAnnotationMetadata();
  }

  @NonNull
  public MetadataReader getMetadataReader(String className) {
    try {
      MetadataReaderFactory metadataFactory = getMetadataReaderFactory();
      return metadataFactory.getMetadataReader(className);
    }
    catch (IOException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  /**
   * Clear the local metadata cache, if any, removing all cached class metadata.
   */
  public void clearCache() {
    if (metadataReaderFactory instanceof CachingMetadataReaderFactory) {
      // Clear cache in externally provided MetadataReaderFactory; this is a no-op
      // for a shared cache since it'll be cleared by the ApplicationContext.
      ((CachingMetadataReaderFactory) metadataReaderFactory).clearCache();
    }
  }

  // ExpressionEvaluator

  public String evaluateExpression(String expression) {
    return evaluateExpression(expression, String.class);
  }

  public <T> T evaluateExpression(String expression, Class<T> requiredType) {
    ExpressionEvaluator expressionEvaluator = applicationContext.getExpressionEvaluator();
    return expressionEvaluator.evaluate(expression, requiredType);
  }

  // PatternResourceLoader

  public Resource getResource(String location) {
    return getResourceLoader().getResource(location);
  }

  public Set<Resource> getResources(String locationPattern) throws IOException {
    return getResourceLoader().getResources(locationPattern);
  }

  // ScopeMetadataResolver

  public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
    Assert.notNull(scopeMetadataResolver, "ScopeMetadataResolver is required");
    this.scopeMetadataResolver = scopeMetadataResolver;
  }

  public ScopeMetadataResolver getScopeMetadataResolver() {
    if (scopeMetadataResolver == null) {
      scopeMetadataResolver = new AnnotationScopeMetadataResolver();
    }
    return scopeMetadataResolver;
  }

  public String resolveScopeName(BeanDefinition definition) {
    return getScopeMetadataResolver().resolveScopeMetadata(definition).getScopeName();
  }

  /**
   * setting default PropertySourceFactory
   *
   * @param propertySourceFactory PropertySourceFactory
   */
  public void setPropertySourceFactory(@Nullable PropertySourceFactory propertySourceFactory) {
    this.propertySourceFactory = propertySourceFactory;
  }

  public PropertySourceFactory getPropertySourceFactory() {
    if (propertySourceFactory == null) {
      propertySourceFactory = new DefaultPropertySourceFactory();
    }
    return propertySourceFactory;
  }

  /**
   * Set the {@code BeanNamePopulator} to use for detected bean classes.
   * <p>The default is a {@link AnnotationBeanNamePopulator}.
   */
  public void setBeanNamePopulator(@Nullable BeanNamePopulator beanNamePopulator) {
    this.beanNamePopulator = beanNamePopulator != null ? beanNamePopulator : AnnotationBeanNamePopulator.INSTANCE;
  }

  public BeanNamePopulator getBeanNamePopulator() {
    return beanNamePopulator;
  }

  public void setProblemReporter(ProblemReporter problemReporter) {
    this.problemReporter = problemReporter;
  }

  public ProblemReporter getProblemReporter() {
    return problemReporter;
  }

  public void reportError(Problem problem) {
    problemReporter.error(problem);
  }

  public void reportWarning(Problem problem) {
    problemReporter.warning(problem);
  }

  public void reportFatal(Problem problem) {
    problemReporter.fatal(problem);
  }

  public ClassLoader getClassLoader() {
    return getResourceLoader().getClassLoader();
  }

  // static

  // this method mainly for internal use
  public static DefinitionLoadingContext from(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "beanFactory is required");
    DefinitionLoadingContext context = getContext(beanFactory);
    if (context == null) {
      synchronized(beanFactory) {
        context = getContext(beanFactory);
        if (context == null) {
          context = new DefinitionLoadingContext(deduceRegistry(beanFactory), deduceContext(beanFactory));
          context.unwrapFactory(SingletonBeanRegistry.class)
                  .registerSingleton(BEAN_NAME, context);
        }
      }
    }
    return context;
  }

  static BeanDefinitionRegistry deduceRegistry(BeanFactory beanFactory) {
    if (beanFactory instanceof BeanDefinitionRegistry registry) {
      return registry;
    }
    throw new IllegalArgumentException("Expect a BeanDefinitionRegistry");
  }

  static ApplicationContext deduceContext(BeanFactory beanFactory) {
    if (beanFactory instanceof ApplicationContext context) {
      return context;
    }
    throw new IllegalArgumentException("Expect a ApplicationContext");
  }

  @Nullable
  private static DefinitionLoadingContext getContext(BeanFactory beanFactory) {
    if (beanFactory instanceof ConfigurableBeanFactory configurable) {
      if (configurable.containsLocalBean(BEAN_NAME)) {
        return configurable.getBean(BEAN_NAME, DefinitionLoadingContext.class);
      }
    }
    else {
      return beanFactory.getBean(BEAN_NAME, DefinitionLoadingContext.class);
    }
    return null;
  }
}
