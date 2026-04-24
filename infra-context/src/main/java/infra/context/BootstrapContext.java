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
import infra.beans.factory.NoSuchBeanDefinitionException;
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

/**
 * Bootstrap Context for application startup and refresh.
 *
 * <p>This context is responsible for instantiating classes that may implement any of the following
 * {@link Aware} interfaces:
 * <ul>
 * <li>{@link EnvironmentAware}</li>
 * <li>{@link BeanFactoryAware}</li>
 * <li>{@link BeanClassLoaderAware}</li>
 * <li>{@link ResourceLoaderAware}</li>
 * <li>{@link BootstrapContextAware}</li>
 * <li>{@link ApplicationContextAware}</li>
 * </ul>
 *
 * <p>Additionally, the constructor parameters of such classes can be resolved from the following types:
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
 *
 * <p>This bootstrap context contains various components required for the IoC container startup process.
 *
 * @author TODAY 2021/10/19 22:22
 * @since 4.0
 */
public class BootstrapContext extends BeanDefinitionCustomizers implements ClassInstantiator, EnvironmentCapable {

  public static final String BEAN_NAME = "infra.context.internalBootstrapContext";

  private final BeanDefinitionRegistry registry;

  private final ConfigurableBeanFactory beanFactory;

  private final @Nullable ApplicationContext applicationContext;

  /* Using short class names as default bean names by default. */
  private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

  private ProblemReporter problemReporter = new FailFastProblemReporter();

  private @Nullable MetadataReaderFactory metadataReaderFactory;

  private @Nullable PatternResourceLoader resourceLoader;

  private @Nullable ScopeMetadataResolver scopeMetadataResolver;

  private @Nullable PropertySourceFactory propertySourceFactory;

  private @Nullable Environment environment;

  @Nullable ConditionEvaluator conditionEvaluator;

  @Nullable ExpressionEvaluator expressionEvaluator;

  /**
   * Construct a new BootstrapContext with the given ApplicationContext.
   * <p>This constructor delegates to {@link #BootstrapContext(ConfigurableBeanFactory, ApplicationContext)},
   * unwrapping the underlying {@link ConfigurableBeanFactory} from the provided context.
   *
   * @param context the application context to wrap; must not be null
   */
  public BootstrapContext(ApplicationContext context) {
    this(context.unwrapFactory(ConfigurableBeanFactory.class), context);
  }

  /**
   * Construct a new BootstrapContext with the given ConfigurableBeanFactory and ApplicationContext.
   * <p>The internal {@link BeanDefinitionRegistry} is obtained by unwrapping the bean factory.
   * If an ApplicationContext is provided, it is also used as the {@link ResourceLoader}.
   *
   * @param beanFactory the configurable bean factory to use; must not be null
   * @param context the application context, or {@code null} if not available
   */
  public BootstrapContext(ConfigurableBeanFactory beanFactory, @Nullable ApplicationContext context) {
    Assert.notNull(beanFactory, "beanFactory is required");
    this.registry = beanFactory.unwrap(BeanDefinitionRegistry.class);
    this.beanFactory = beanFactory;
    this.resourceLoader = context;
    this.applicationContext = context;
  }

  /**
   * Construct a new BootstrapContext with the given Environment and ConfigurableBeanFactory.
   * <p>This constructor creates a context without an associated ApplicationContext.
   * The provided environment is set explicitly via {@link #setEnvironment(Environment)}.
   *
   * @param environment the environment to use, or {@code null} to defer initialization
   * @param beanFactory the configurable bean factory to use; must not be null
   */
  public BootstrapContext(@Nullable Environment environment, ConfigurableBeanFactory beanFactory) {
    this(beanFactory, null);
    setEnvironment(environment);
  }

  /**
   * Return the underlying {@link BeanDefinitionRegistry} used by this context.
   *
   * @return the bean definition registry
   */
  public BeanDefinitionRegistry getRegistry() {
    return registry;
  }

  /**
   * Return the underlying {@link ApplicationContext} if available.
   *
   * @return the application context, or {@code null} if not available
   */
  public @Nullable ApplicationContext getApplicationContext() {
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

  /**
   * Return the underlying {@link ConfigurableBeanFactory} used by this context.
   *
   * @return the configurable bean factory
   */
  public ConfigurableBeanFactory getBeanFactory() {
    return beanFactory;
  }

  /**
   * Unwrap the underlying bean factory to the specified required type.
   * <p>
   * If an {@link ApplicationContext} is available, this method delegates to
   * {@link ApplicationContext#unwrapFactory(Class)}. Otherwise, it delegates to
   * the underlying {@link ConfigurableBeanFactory#unwrap(Class)}.
   *
   * @param <T> the target type to unwrap to
   * @param requiredType the class of the target type
   * @return an instance of the specified type from the underlying bean factory
   * @throws IllegalArgumentException if the bean factory cannot be unwrapped to the required type
   */
  public <T> T unwrapFactory(Class<T> requiredType) {
    if (applicationContext != null) {
      return applicationContext.unwrapFactory(requiredType);
    }
    return beanFactory.unwrap(requiredType);
  }

  /**
   * Unwrap the underlying {@link ApplicationContext} to the specified required type.
   * <p>
   * This method delegates to {@link ApplicationContext#unwrap(Class)} to obtain an instance
   * of the given type from the application context.
   *
   * @param <T> the target type to unwrap to
   * @param requiredType the class of the target type
   * @return an instance of the specified type from the application context
   * @throws IllegalArgumentException if the application context cannot be unwrapped to the required type
   * @throws IllegalStateException if no {@link ApplicationContext} is available
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

  /**
   * Get the {@link ConditionEvaluator} for this context.
   * <p>
   * If the evaluator has not been initialized, it will be created based on the
   * available {@link ApplicationContext} or {@link Environment}.
   *
   * @return the condition evaluator instance
   */
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

  /**
   * Register a bean definition with the given name in the underlying registry.
   * <p>
   * Before registration, this method ensures that the bean definition has a scope set.
   * If no scope is present, it resolves the scope name using the configured
   * {@link ScopeMetadataResolver}. Additionally, any registered
   * {@link BeanDefinitionCustomizer}s are applied to the definition.
   *
   * @param beanName the name of the bean to register
   * @param definition the bean definition to register
   */
  public void registerBeanDefinition(String beanName, BeanDefinition definition) {
    if (definition.getScope() == null) {
      definition.setScope(resolveScopeName(definition));
    }

    customize(definition);
    registry.registerBeanDefinition(beanName, definition);
  }

  /**
   * Register an alias for the specified bean name.
   *
   * @param beanName the canonical bean name
   * @param alias the alias to register
   */
  public void registerAlias(String beanName, String alias) {
    registry.registerAlias(beanName, alias);
  }

  /**
   * Check whether a bean definition with the given name exists in the registry.
   *
   * @param beanName the name of the bean to check
   * @return {@code true} if a bean definition with the given name exists
   */
  public boolean containsBeanDefinition(String beanName) {
    return registry.containsBeanDefinition(beanName);
  }

  /**
   * Remove the bean definition with the given name from the registry.
   *
   * @param beanName the name of the bean definition to remove
   */
  public void removeBeanDefinition(String beanName) {
    registry.removeBeanDefinition(beanName);
  }

  /**
   * Return the bean definition for the given bean name.
   *
   * @param beanName the name of the bean to look up
   * @return the bean definition for the given name
   * @throws NoSuchBeanDefinitionException if no bean definition with the given name exists
   */
  public BeanDefinition getBeanDefinition(String beanName) {
    return registry.getBeanDefinition(beanName);
  }

  /**
   * Determine whether the annotated type matches the conditions for the given configuration phase.
   *
   * @param metadata the annotated type metadata to check
   * @param phase the configuration phase to evaluate against (may be {@code null})
   * @return {@code true} if the conditions pass
   */
  public boolean passCondition(AnnotatedTypeMetadata metadata, @Nullable ConfigurationPhase phase) {
    return getConditionEvaluator().passCondition(metadata, phase);
  }

  /**
   * Determine whether the annotated type should be skipped during processing for the given configuration phase.
   *
   * @param metadata the annotated type metadata to check (may be {@code null})
   * @param phase the configuration phase to evaluate against (may be {@code null})
   * @return {@code true} if the type should be skipped
   */
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
   */
  @Override
  public <T> T instantiate(Class<T> clazz) {
    Assert.notNull(clazz, "Class is required");
    if (clazz.isInterface()) {
      throw new BeanInstantiationException(clazz, "Specified class is an interface");
    }

    try {
      Constructor<T> constructor = BeanUtils.getResolvableConstructor(clazz);
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
    catch (Exception e) {
      throw new BeanInstantiationException(clazz, "No suitable constructor found");
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

  private @Nullable Object findProvided(Parameter parameter) {
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

  /**
   * Return the {@link PatternResourceLoader} used by this context.
   * <p>If the loader has not been initialized, it will be created based on the
   * available {@link ApplicationContext} or a default {@link PathMatchingPatternResourceLoader}.
   * Additionally, if the {@link MetadataReaderFactory} is not yet initialized, it will be
   * created using the resolved resource loader.
   *
   * @return the pattern resource loader instance; never {@code null}
   */
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

  /**
   * Resolve the given location string into a {@link Resource} handle.
   * <p>This method delegates to the configured {@link PatternResourceLoader}
   * to perform the actual resource resolution.
   *
   * @param location the resource location (e.g., a file path, classpath location, or URL)
   * @return the corresponding {@link Resource} handle; never {@code null}
   * @see #getResourceLoader()
   * @see PatternResourceLoader#getResource(String)
   */
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

  /**
   * Obtain the {@link AnnotationMetadata} for the specified class name.
   * <p>This method retrieves a {@link MetadataReader} for the given class and extracts
   * its annotation metadata, including information about annotations present on the class,
   * its methods, fields, and constructors.
   *
   * @param className the fully qualified name of the class to read
   * @return the annotation metadata for the specified class
   * @throws IOException if an I/O error occurs while reading the class resource
   * @see #getMetadataReader(String)
   */
  public AnnotationMetadata getAnnotationMetadata(String className) throws IOException {
    return getMetadataReader(className).getAnnotationMetadata();
  }

  /**
   * Obtain a {@link MetadataReader} for the specified class name.
   * <p>This method delegates to the configured {@link MetadataReaderFactory}
   * to read and parse the class metadata, including annotations and class hierarchy information.
   *
   * @param className the fully qualified name of the class to read
   * @return a {@code MetadataReader} instance for the specified class
   * @throws IOException if an I/O error occurs while reading the class resource
   * @see #getMetadataReaderFactory()
   */
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
   * Evaluate the given expression and return the result as a {@link String}.
   * <p>This is a convenience method that delegates to
   * {@link #evaluateExpression(String, Class)} with {@code String.class} as the required type.
   *
   * @param expression the expression to evaluate
   * @return the evaluation result as a {@code String}, or {@code null} if the result is absent
   * @see #evaluateExpression(String, Class)
   */
  public @Nullable String evaluateExpression(String expression) {
    return evaluateExpression(expression, String.class);
  }

  /**
   * Evaluate the given expression and return the result as the specified required type.
   * <p>This method delegates to the configured {@link ExpressionEvaluator} to perform
   * the actual expression evaluation against the current context.
   *
   * @param <T> the required result type
   * @param expression the expression to evaluate (e.g., SpEL, property placeholder)
   * @param requiredType the class of the required result type
   * @return the evaluation result as an instance of {@code T}, or {@code null} if the result is absent
   * @see #getExpressionEvaluator()
   * @see ExpressionEvaluator#evaluate(String, Class)
   */
  public <T extends @Nullable Object> T evaluateExpression(String expression, Class<T> requiredType) {
    return getExpressionEvaluator().evaluate(expression, requiredType);
  }

  /**
   * Return the {@link ExpressionEvaluator} used by this context.
   * <p>If the evaluator has not been initialized, it will be created based on the
   * available {@link ApplicationContext} or the underlying {@link BeanFactory}.
   *
   * @return the expression evaluator instance; never {@code null}
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

  /**
   * Set the {@link ScopeMetadataResolver} to use for resolving scope metadata.
   * <p>The default is an {@link AnnotationScopeMetadataResolver}.
   *
   * @param scopeMetadataResolver the scope metadata resolver; must not be null
   */
  public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
    Assert.notNull(scopeMetadataResolver, "ScopeMetadataResolver is required");
    this.scopeMetadataResolver = scopeMetadataResolver;
  }

  /**
   * Return the {@link ScopeMetadataResolver} used by this context.
   * <p>If no resolver has been explicitly set, a default {@link AnnotationScopeMetadataResolver}
   * will be created and returned.
   *
   * @return the scope metadata resolver; never {@code null}
   */
  public ScopeMetadataResolver getScopeMetadataResolver() {
    if (scopeMetadataResolver == null) {
      scopeMetadataResolver = new AnnotationScopeMetadataResolver();
    }
    return scopeMetadataResolver;
  }

  /**
   * Resolve the scope name for the given bean definition.
   * <p>This method delegates to {@link #resolveScopeMetadata(BeanDefinition)} to obtain
   * the {@link ScopeMetadata} and then extracts the scope name from it.
   *
   * @param definition the bean definition to resolve the scope name for
   * @return the resolved scope name; never {@code null}
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
   * Set the {@link PropertySourceFactory} to use for creating property sources from resources.
   * <p>The default is a {@link DefaultPropertySourceFactory}.
   *
   * @param propertySourceFactory the property source factory, or {@code null} to use the default
   */
  public void setPropertySourceFactory(@Nullable PropertySourceFactory propertySourceFactory) {
    this.propertySourceFactory = propertySourceFactory;
  }

  /**
   * Return the {@link PropertySourceFactory} used by this context.
   * <p>If no factory has been explicitly set, a default {@link DefaultPropertySourceFactory}
   * will be created and returned.
   *
   * @return the property source factory; never {@code null}
   */
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

  /**
   * Return the {@link BeanNameGenerator} used by this context.
   * <p>If no generator has been explicitly set, a default {@link AnnotationBeanNameGenerator}
   * will be used.
   *
   * @return the bean name generator; never {@code null}
   */
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

  /**
   * Return the {@link ProblemReporter} used by this context.
   * <p>This reporter is responsible for handling problems detected during
   * {@link Configuration} or {@link Component} processing, such as invalid method signatures.
   * The default implementation is a {@link FailFastProblemReporter}.
   *
   * @return the problem reporter; never {@code null}
   * @see #setProblemReporter(ProblemReporter)
   */
  public ProblemReporter getProblemReporter() {
    return problemReporter;
  }

  /**
   * Report a problem using the configured {@link ProblemReporter}.
   * <p>This method delegates to {@link ProblemReporter#error(Problem)} to handle
   * the given problem, which may result in an exception being thrown depending
   * on the reporter's configuration (e.g., fail-fast behavior).
   *
   * @param problem the problem to report; must not be null
   * @see ProblemReporter#error(Problem)
   */
  public void reportError(Problem problem) {
    problemReporter.error(problem);
  }

  /**
   * Return the {@link ClassLoader} to use for loading classes and resources.
   * <p>This method first attempts to obtain the class loader from the underlying
   * {@link ConfigurableBeanFactory}. If that returns {@code null}, it falls back to the
   * class loader of the configured {@link ResourceLoader}.
   *
   * @return the class loader, or {@code null} if neither the bean factory nor
   * the resource loader provides one
   * @see ConfigurableBeanFactory#getBeanClassLoader()
   * @see ResourceLoader#getClassLoader()
   */
  public @Nullable ClassLoader getClassLoader() {
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

  static @Nullable ApplicationContext deduceContext(BeanFactory beanFactory) {
    if (beanFactory instanceof ApplicationContext context) {
      return context;
    }
    return null;
  }

  private static @Nullable BootstrapContext findContext(BeanFactory beanFactory) {
    return BeanFactoryUtils.findLocal(beanFactory, BEAN_NAME, BootstrapContext.class);
  }

}
