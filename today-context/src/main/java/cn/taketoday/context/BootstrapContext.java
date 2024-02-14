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

package cn.taketoday.context;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Set;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.Aware;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.config.BeanDefinitionCustomizers;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.ExpressionEvaluator;
import cn.taketoday.beans.factory.parsing.FailFastProblemReporter;
import cn.taketoday.beans.factory.parsing.Problem;
import cn.taketoday.beans.factory.parsing.ProblemReporter;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.context.annotation.AnnotationBeanNameGenerator;
import cn.taketoday.context.annotation.AnnotationScopeMetadataResolver;
import cn.taketoday.context.annotation.ConditionEvaluator;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ConfigurationCondition.ConfigurationPhase;
import cn.taketoday.context.annotation.ScopeMetadata;
import cn.taketoday.context.annotation.ScopeMetadataResolver;
import cn.taketoday.core.ConstructorNotFoundException;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.EnvironmentCapable;
import cn.taketoday.core.env.StandardEnvironment;
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
import cn.taketoday.lang.ClassInstantiator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.CollectionUtils;

/**
 * Startup(Refresh) Context
 *
 * <p>
 * BootstrapContext is can instantiate the class may implement any of the following
 * {@link Aware Aware} interfaces
 * <ul>
 * <li>{@link EnvironmentAware}</li>
 * <li>{@link BeanFactoryAware}</li>
 * <li>{@link BeanClassLoaderAware}</li>
 * <li>{@link ResourceLoaderAware}</li>
 * <li>{@link BootstrapContextAware}</li>
 * <li>{@link ApplicationContextAware}</li>
 * </ul>
 * and it's {@link Constructor#getParameters()} can be following types
 * <ul>
 * <li>{@link Environment}</li>
 * <li>{@link BeanFactory}</li>
 * <li>{@link ClassLoader}</li>
 * <li>{@link ResourceLoader}</li>
 * <li>{@link BootstrapContext}</li>
 * <li>{@link ApplicationContext}</li>
 * <li>{@link BeanDefinitionRegistry}</li>
 * <li>{@link ExpressionEvaluator}</li>
 * </ul>
 * <p>
 *  This bootstrap context is containing many components that for context(IoC) startup
 *
 * @author TODAY 2021/10/19 22:22
 * @since 4.0
 */
public class BootstrapContext extends BeanDefinitionCustomizers implements ClassInstantiator, EnvironmentCapable {

  public static final String BEAN_NAME = "cn.taketoday.context.internalBootstrapContext";

  private final BeanDefinitionRegistry registry;

  private final ConfigurableBeanFactory beanFactory;

  @Nullable
  private final ApplicationContext applicationContext;

  @Nullable
  private ConditionEvaluator conditionEvaluator;

  @Nullable
  private MetadataReaderFactory metadataReaderFactory;

  @Nullable
  private PatternResourceLoader resourceLoader;

  @Nullable
  private ScopeMetadataResolver scopeMetadataResolver;

  @Nullable
  private PropertySourceFactory propertySourceFactory;

  /* Using short class names as default bean names by default. */
  private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

  private ProblemReporter problemReporter = new FailFastProblemReporter();

  @Nullable
  private Environment environment;

  @Nullable
  private ExpressionEvaluator expressionEvaluator;

  public BootstrapContext(ApplicationContext context) {
    this(context.unwrapFactory(ConfigurableBeanFactory.class), context);
  }

  public BootstrapContext(ConfigurableBeanFactory beanFactory, @Nullable ApplicationContext context) {
    Assert.notNull(beanFactory, "beanFactory is required");
    this.registry = beanFactory.unwrap(BeanDefinitionRegistry.class);
    this.beanFactory = beanFactory;
    this.resourceLoader = context;
    this.applicationContext = context;
  }

  public BootstrapContext(@Nullable Environment environment, ConfigurableBeanFactory beanFactory) {
    this(beanFactory, null);
    setEnvironment(environment);
  }

  public BeanDefinitionRegistry getRegistry() {
    return registry;
  }

  @Nullable
  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  @Override
  public Environment getEnvironment() {
    Environment environment = this.environment;
    if (environment == null) {
      if (applicationContext != null) {
        environment = applicationContext.getEnvironment();
      }
      if (environment == null) {
        environment = new StandardEnvironment();
      }
      this.environment = environment;
    }
    return environment;
  }

  /**
   * Set environment when applicationContext is {@code null}
   *
   * @param environment Environment
   */
  public void setEnvironment(@Nullable Environment environment) {
    this.environment = environment;
  }

  public ConfigurableBeanFactory getBeanFactory() {
    return beanFactory;
  }

  /**
   * unwrap bean-factory to {@code requiredType}
   *
   * @throws IllegalArgumentException not a requiredType
   */
  public <T> T unwrapFactory(Class<T> requiredType) {
    if (applicationContext != null) {
      return applicationContext.unwrapFactory(requiredType);
    }
    return beanFactory.unwrap(requiredType);
  }

  /**
   * unwrap this ApplicationContext to {@code requiredType}
   *
   * @throws IllegalArgumentException not a requiredType
   */
  public <T> T unwrapContext(Class<T> requiredType) {
    Assert.state(applicationContext != null, "No ApplicationContext available");
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
  public String generateBeanName(BeanDefinition definition) {
    return beanNameGenerator.generateBeanName(definition, registry);
  }

  public ConditionEvaluator getConditionEvaluator() {
    if (conditionEvaluator == null) {
      if (applicationContext != null) {
        this.conditionEvaluator = new ConditionEvaluator(applicationContext, registry);
      }
      else {
        this.conditionEvaluator = new ConditionEvaluator(getEnvironment(), null, registry);
      }
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

  public boolean passCondition(AnnotatedTypeMetadata metadata, @Nullable ConfigurationPhase phase) {
    return getConditionEvaluator().passCondition(metadata, phase);
  }

  public boolean shouldSkip(@Nullable AnnotatedTypeMetadata metadata, @Nullable ConfigurationPhase phase) {
    return getConditionEvaluator().shouldSkip(metadata, phase);
  }

  //---------------------------------------------------------------------
  // Instantiator
  //---------------------------------------------------------------------

  /**
   * Convenience method to instantiate a class using the given class.
   * <p>
   * Note that this method tries to set the constructor accessible if given a
   * non-accessible (that is, non-public) constructor,
   * with optional parameters and default values.
   *
   * <p>
   * This method instantiate the class may implement any of the following
   * {@link Aware Aware} interfaces
   * <ul>
   * <li>{@link EnvironmentAware}</li>
   * <li>{@link BeanFactoryAware}</li>
   * <li>{@link BeanClassLoaderAware}</li>
   * <li>{@link ResourceLoaderAware}</li>
   * <li>{@link BootstrapContextAware}</li>
   * <li>{@link ApplicationContextAware}</li>
   * </ul>
   * And it's {@link Constructor#getParameters()} can be following types
   * <ul>
   * <li>{@link Environment}</li>
   * <li>{@link BeanFactory}</li>
   * <li>{@link ClassLoader}</li>
   * <li>{@link ResourceLoader}</li>
   * <li>{@link BootstrapContext}</li>
   * <li>{@link ApplicationContext}</li>
   * <li>{@link BeanDefinitionRegistry}</li>
   * <li>{@link ExpressionEvaluator}</li>
   * <li>{@link MetadataReaderFactory}</li>
   * </ul>
   *
   * @see BeanInstantiationException If the bean cannot be instantiated
   * @see ConstructorNotFoundException If there is no suitable constructor
   */
  @Override
  public <T> T instantiate(Class<T> clazz) {
    Assert.notNull(clazz, "Class is required");
    if (clazz.isInterface()) {
      throw new BeanInstantiationException(clazz, "Specified class is an interface");
    }
    Constructor<T> constructor = BeanUtils.obtainConstructor(clazz);

    try {
      int i = 0;
      Parameter[] parameters = constructor.getParameters();
      Object[] args = new Object[parameters.length];
      for (Parameter parameter : parameters) {
        Object arg = findProvided(parameter);
        args[i++] = arg;
      }

      T instance = BeanUtils.newInstance(constructor, args);
      invokeAwareMethods(instance);
      return instance;
    }
    catch (IllegalStateException ex) {
      throw new BeanInstantiationException(clazz, "No suitable constructor found", ex);
    }
  }

  /**
   * Instantiate a class using an appropriate constructor and return the new
   * instance as the specified assignable type. The returned instance will
   * have {@link BeanClassLoaderAware}, {@link BeanFactoryAware},
   * {@link EnvironmentAware}, and {@link ResourceLoaderAware} contracts
   * invoked if they are implemented by the given object.
   */
  @SuppressWarnings("unchecked")
  public <T> T instantiate(Class<?> clazz, Class<T> assignableTo) {
    Assert.notNull(clazz, "Class is required");
    Assert.isAssignable(assignableTo, clazz);
    if (clazz.isInterface()) {
      throw new BeanInstantiationException(clazz, "Specified class is an interface");
    }
    return (T) instantiate(clazz);
  }

  @Nullable
  private Object findProvided(Parameter parameter) {
    Class<?> parameterType = parameter.getType();
    if (Environment.class.isAssignableFrom(parameterType)
            && parameterType.isInstance(getEnvironment())) {
      return getEnvironment();
    }
    if (ResourceLoader.class.isAssignableFrom(parameterType)
            && parameterType.isInstance(getResourceLoader())) {
      return getResourceLoader();
    }
    if (parameterType.isInstance(beanFactory)) {
      return beanFactory;
    }
    if (parameterType == ClassLoader.class) {
      return getClassLoader();
    }
    if (parameterType == BeanDefinitionRegistry.class) {
      return registry;
    }
    if (parameterType == BootstrapContext.class) {
      return this;
    }
    if (parameterType.isInstance(applicationContext)) {
      return applicationContext;
    }
    if (parameterType == ExpressionEvaluator.class) {
      return getExpressionEvaluator();
    }
    if (parameterType == MetadataReaderFactory.class) {
      return getMetadataReaderFactory();
    }

    throw new IllegalStateException("Illegal method parameter type: " + parameterType.getName());
  }

  private void invokeAwareMethods(Object bean) {
    if (bean instanceof Aware) {
      if (bean instanceof BeanClassLoaderAware aware) {
        aware.setBeanClassLoader(getClassLoader());
      }
      if (bean instanceof BeanFactoryAware aware) {
        aware.setBeanFactory(beanFactory);
      }
      if (bean instanceof EnvironmentAware aware) {
        aware.setEnvironment(getEnvironment());
      }
      if (bean instanceof ResourceLoaderAware aware) {
        aware.setResourceLoader(getResourceLoader());
      }

      if (bean instanceof BootstrapContextAware aware) {
        aware.setBootstrapContext(this);
      }

      if (bean instanceof ApplicationContextAware aware && applicationContext != null) {
        aware.setApplicationContext(applicationContext);
      }
    }
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
      if (applicationContext != null) {
        this.resourceLoader = applicationContext;
      }
      else {
        this.resourceLoader = new PathMatchingPatternResourceLoader();
      }
      // try to bind ResourceLoader to MetadataReaderFactory
      if (this.metadataReaderFactory == null) {
        this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
      }
    }
    return this.resourceLoader;
  }

  public Resource getResource(String location) {
    return getResourceLoader().getResource(location);
  }

  public Set<Resource> getResources(String locationPattern) throws IOException {
    return getResourceLoader().getResources(locationPattern);
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
        getResourceLoader();
      }
      else {
        ResourceLoader resourceLoader;
        if (this.resourceLoader instanceof PathMatchingPatternResourceLoader loader) {
          resourceLoader = loader.getRootLoader();
        }
        else {
          resourceLoader = this.resourceLoader;
        }
        this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
      }
    }
    return this.metadataReaderFactory;
  }

  public AnnotationMetadata getAnnotationMetadata(String className) throws IOException {
    return getMetadataReader(className).getAnnotationMetadata();
  }

  public MetadataReader getMetadataReader(String className) throws IOException {
    MetadataReaderFactory metadataFactory = getMetadataReaderFactory();
    return metadataFactory.getMetadataReader(className);
  }

  /**
   * Clear the local metadata cache, if any, removing all cached class metadata.
   */
  public void clearCache() {
    if (metadataReaderFactory instanceof CachingMetadataReaderFactory cmef) {
      // Clear cache in externally provided MetadataReaderFactory; this is a no-op
      // for a shared cache since it'll be cleared by the ApplicationContext.
      cmef.clearCache();
    }
    if (conditionEvaluator != null) {
      conditionEvaluator.clearCache();
    }
  }

  //---------------------------------------------------------------------
  // ExpressionEvaluator
  //---------------------------------------------------------------------

  /**
   * evaluate expression
   */
  @Nullable
  public String evaluateExpression(String expression) {
    return evaluateExpression(expression, String.class);
  }

  /**
   * evaluate expression
   */
  @Nullable
  public <T> T evaluateExpression(String expression, Class<T> requiredType) {
    return getExpressionEvaluator().evaluate(expression, requiredType);
  }

  /**
   * Get ExpressionEvaluator
   */
  public ExpressionEvaluator getExpressionEvaluator() {
    ExpressionEvaluator expressionEvaluator = this.expressionEvaluator;
    if (expressionEvaluator == null) {
      if (applicationContext != null) {
        expressionEvaluator = applicationContext.getExpressionEvaluator();
      }
      else {
        expressionEvaluator = ExpressionEvaluator.from(getBeanFactory());
      }
      this.expressionEvaluator = expressionEvaluator;
    }
    return expressionEvaluator;
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

  /**
   * Get the name of the scope.
   *
   * @see #resolveScopeMetadata(BeanDefinition)
   */
  public String resolveScopeName(BeanDefinition definition) {
    return resolveScopeMetadata(definition).getScopeName();
  }

  /**
   * Resolve the {@link ScopeMetadata} appropriate to the supplied
   * bean {@code definition}.
   * <p>Implementations can of course use any strategy they like to
   * determine the scope metadata, but some implementations that
   * immediately to mind might be to use source level annotations
   * present on {@link BeanDefinition#getBeanClassName() the class} of the
   * supplied {@code definition}, or to use metadata present in the
   * {@link BeanDefinition#getAttributeNames()} of the supplied {@code definition}.
   *
   * @param definition the target bean definition
   * @return the relevant scope metadata; never {@code null}
   */
  public ScopeMetadata resolveScopeMetadata(BeanDefinition definition) {
    return getScopeMetadataResolver().resolveScopeMetadata(definition);
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
   * Set the {@code BeanNameGenerator} to use for detected bean classes.
   * <p>The default is a {@link AnnotationBeanNameGenerator}.
   */
  public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
    this.beanNameGenerator = beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE;
  }

  public BeanNameGenerator getBeanNameGenerator() {
    return beanNameGenerator;
  }

  /**
   * Set the {@link ProblemReporter} to use.
   * <p>Used to register any problems detected with {@link Configuration} or {@link Component}
   * declarations. For instance, a @Component method marked as {@code final} is illegal
   * and would be reported as a problem. Defaults to {@link FailFastProblemReporter}.
   */
  public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
    this.problemReporter = problemReporter != null ? problemReporter : new FailFastProblemReporter();
  }

  public ProblemReporter getProblemReporter() {
    return problemReporter;
  }

  public void reportError(Problem problem) {
    problemReporter.error(problem);
  }

  @Nullable
  public ClassLoader getClassLoader() {
    ClassLoader classLoader = beanFactory.getBeanClassLoader();
    if (classLoader == null) {
      classLoader = getResourceLoader().getClassLoader();
    }
    return classLoader;
  }

  // static

  // this method mainly for internal use
  public static BootstrapContext from(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "beanFactory is required");
    BootstrapContext context = findContext(beanFactory);
    if (context == null) {
      synchronized(beanFactory) {
        context = findContext(beanFactory);
        if (context == null) {
          ConfigurableBeanFactory factory = beanFactory.unwrap(ConfigurableBeanFactory.class);
          context = new BootstrapContext(factory, deduceContext(beanFactory));
          factory.registerSingleton(BEAN_NAME, context);
        }
      }
    }
    return context;
  }

  @Nullable
  static ApplicationContext deduceContext(BeanFactory beanFactory) {
    if (beanFactory instanceof ApplicationContext context) {
      return context;
    }
    return null;
  }

  @Nullable
  private static BootstrapContext findContext(BeanFactory beanFactory) {
    return BeanFactoryUtils.findLocal(beanFactory, BEAN_NAME, BootstrapContext.class);
  }

}
