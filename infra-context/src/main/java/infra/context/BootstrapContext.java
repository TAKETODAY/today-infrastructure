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

package infra.context;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

import infra.beans.BeanInstantiationException;
import infra.beans.BeanUtils;
import infra.beans.factory.Aware;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionCustomizer;
import infra.beans.factory.config.BeanDefinitionCustomizers;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.ExpressionEvaluator;
import infra.beans.factory.parsing.FailFastProblemReporter;
import infra.beans.factory.parsing.Problem;
import infra.beans.factory.parsing.ProblemReporter;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.BeanNameGenerator;
import infra.context.annotation.AnnotationBeanNameGenerator;
import infra.context.annotation.AnnotationScopeMetadataResolver;
import infra.context.annotation.ConditionEvaluator;
import infra.context.annotation.Configuration;
import infra.context.annotation.ConfigurationCondition.ConfigurationPhase;
import infra.context.annotation.ScopeMetadata;
import infra.context.annotation.ScopeMetadataResolver;
import infra.core.ConstructorNotFoundException;
import infra.core.env.Environment;
import infra.core.env.EnvironmentCapable;
import infra.core.env.StandardEnvironment;
import infra.core.io.DefaultPropertySourceFactory;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.PatternResourceLoader;
import infra.core.io.PropertySourceFactory;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.core.type.AnnotatedTypeMetadata;
import infra.core.type.AnnotationMetadata;
import infra.core.type.classreading.CachingMetadataReaderFactory;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.lang.Assert;
import infra.lang.ClassInstantiator;
import infra.stereotype.Component;
import infra.util.CollectionUtils;

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

  public static final String BEAN_NAME = "infra.context.internalBootstrapContext";

  private final BeanDefinitionRegistry registry;

  private final ConfigurableBeanFactory beanFactory;

  @Nullable
  private final ApplicationContext applicationContext;

  @Nullable
  ConditionEvaluator conditionEvaluator;

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
  ExpressionEvaluator expressionEvaluator;

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
  @SuppressWarnings("NullAway")
  public <T> T instantiate(Class<T> clazz) {
    Assert.notNull(clazz, "Class is required");
    if (clazz.isInterface()) {
      throw new BeanInstantiationException(clazz, "Specified class is an interface");
    }
    Constructor<T> constructor = BeanUtils.obtainConstructor(clazz);

    try {
      int i = 0;
      Parameter[] parameters = constructor.getParameters();
      @Nullable Object[] args = new Object[parameters.length];
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

  @SuppressWarnings("NullAway")
  public void invokeAwareMethods(Object bean) {
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
   * <p>Default is a {@code PathMatchingPatternResourceLoader}, also capable of
   * resource pattern resolving through the {@code PatternResourceLoader} interface.
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
  @SuppressWarnings("NullAway")
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
  public static BootstrapContext obtain(BeanFactory beanFactory) {
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
