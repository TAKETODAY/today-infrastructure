/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.test.context.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.app.test.context.assertj.ApplicationContextAssert;
import infra.app.test.context.assertj.ApplicationContextAssertProvider;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionCustomizer;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.AbstractAutowireCapableBeanFactory;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.BeanNameGenerator;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.AnnotationConfigRegistry;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.config.Configurations;
import infra.context.annotation.config.UserConfigurations;
import infra.context.support.GenericApplicationContext;
import infra.core.ResolvableType;
import infra.core.env.Environment;
import infra.core.io.DefaultResourceLoader;
import infra.lang.Assert;
import infra.test.context.FilteredClassLoader;
import infra.test.util.TestPropertyValues;
import infra.util.CollectionUtils;

/**
 * Utility design to run an {@link ApplicationContext} and provide AssertJ style
 * assertions. The test is best used as a field of a test class, describing the shared
 * configuration required for the test:
 *
 * <pre class="code">
 * public class MyContextTests {
 *     private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
 *             .withPropertyValues("infra.foo=bar")
 *             .withUserConfiguration(MyConfiguration.class);
 * }</pre>
 *
 * <p>
 * The initialization above makes sure to register {@code MyConfiguration} for all tests
 * and set the {@code today.foo} property to {@code bar} unless specified otherwise.
 * <p>
 * Based on the configuration above, a specific test can simulate what will happen when
 * the context runs, perhaps with overridden property values:
 *
 * <pre class="code">
 * &#064;Test
 * public someTest() {
 *     this.contextRunner.withPropertyValues("infra.foo=biz").run(context -&gt; {
 *         assertThat(context).containsSingleBean(MyBean.class);
 *         // other assertions
 *     });
 * }</pre>
 * <p>
 * The test above has changed the {@code infra.foo} property to {@code biz} and is
 * asserting that the context contains a single {@code MyBean} bean. The
 * {@link #run(ContextConsumer) run} method takes a {@link ContextConsumer} that can apply
 * assertions to the context. Upon completion, the context is automatically closed.
 * <p>
 * If the application context fails to start the {@code #run(ContextConsumer)} method is
 * called with a "failed" application context. Calls to the context will throw an
 * {@link IllegalStateException} and assertions that expect a running context will fail.
 * The {@link ApplicationContextAssert#getFailure() getFailure()} assertion can be used if
 * further checks are required on the cause of the failure: <pre class="code">
 * &#064;Test
 * public someTest() {
 *     this.context.withPropertyValues("infra.foo=fails").run(loaded -&gt; {
 *         assertThat(loaded).getFailure().hasCauseInstanceOf(BadPropertyException.class);
 *         // other assertions
 *     });
 * }</pre>
 * <p>
 *
 * @param <SELF> the "self" type for this runner
 * @param <C> the context type
 * @param <A> the application context assertion provider
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @see ApplicationContextRunner
 * @see WebApplicationContextRunner
 * @see ReactiveWebApplicationContextRunner
 * @see ApplicationContextAssert
 * @since 4.0
 */
@SuppressWarnings("NullAway")
public abstract class AbstractApplicationContextRunner<
        SELF extends AbstractApplicationContextRunner<SELF, C, A>,
        C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> {

  private static final Class<?>[] NO_ADDITIONAL_CONTEXT_INTERFACES = {};

  private final RunnerConfiguration<C> runnerConfiguration;

  private final Function<RunnerConfiguration<C>, SELF> instanceFactory;

  /**
   * Create a new {@link AbstractApplicationContextRunner} instance.
   *
   * @param contextFactory the factory used to create the actual context
   * @param instanceFactory the factory used to create new instance of the runner
   */
  protected AbstractApplicationContextRunner(Supplier<C> contextFactory,
          Function<RunnerConfiguration<C>, SELF> instanceFactory) {
    this(instanceFactory, contextFactory, NO_ADDITIONAL_CONTEXT_INTERFACES);
  }

  /**
   * Create a new {@link AbstractApplicationContextRunner} instance.
   *
   * @param instanceFactory the factory used to create new instance of the runner
   * @param contextFactory the factory used to create the actual context
   * @param additionalContextInterfaces any additional application context interfaces to
   * be added to the application context proxy
   */
  protected AbstractApplicationContextRunner(Function<RunnerConfiguration<C>, SELF> instanceFactory,
          Supplier<C> contextFactory, Class<?>... additionalContextInterfaces) {
    Assert.notNull(instanceFactory, "'instanceFactory' is required");
    Assert.notNull(contextFactory, "'contextFactory' is required");
    this.instanceFactory = instanceFactory;
    this.runnerConfiguration = new RunnerConfiguration<>(contextFactory, additionalContextInterfaces);
  }

  /**
   * Create a new {@link AbstractApplicationContextRunner} instance.
   *
   * @param configuration the configuration for the runner to use
   * @param instanceFactory the factory used to create new instance of the runner
   */
  protected AbstractApplicationContextRunner(RunnerConfiguration<C> configuration,
          Function<RunnerConfiguration<C>, SELF> instanceFactory) {
    Assert.notNull(configuration, "RunnerConfiguration is required");
    Assert.notNull(instanceFactory, "instanceFactory is required");
    this.runnerConfiguration = configuration;
    this.instanceFactory = instanceFactory;
  }

  /**
   * Specify if bean definition overriding, by registering a definition with the same
   * name as an existing definition, should be allowed.
   *
   * @param allowBeanDefinitionOverriding if bean overriding is allowed
   * @return a new instance with the updated bean definition overriding policy
   * @see StandardBeanFactory#setAllowBeanDefinitionOverriding(boolean)
   */
  public SELF withAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
    return newInstance(this.runnerConfiguration.withAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding));
  }

  /**
   * Specify if circular references between beans should be allowed.
   *
   * @param allowCircularReferences if circular references between beans are allowed
   * @return a new instance with the updated circular references policy
   * @see AbstractAutowireCapableBeanFactory#setAllowCircularReferences(boolean)
   */
  public SELF withAllowCircularReferences(boolean allowCircularReferences) {
    return newInstance(this.runnerConfiguration.withAllowCircularReferences(allowCircularReferences));
  }

  /**
   * Add an {@link ApplicationContextInitializer} to be called when the context is
   * created.
   *
   * @param initializer the initializer to add
   * @return a new instance with the updated initializers
   */
  public SELF withInitializer(ApplicationContextInitializer initializer) {
    Assert.notNull(initializer, "Initializer is required");
    return newInstance(this.runnerConfiguration.withInitializer(initializer));
  }

  /**
   * Add the specified {@link Environment} property pairs. Key-value pairs can be
   * specified with colon (":") or equals ("=") separators. Override matching keys that
   * might have been specified previously.
   *
   * @param pairs the key-value pairs for properties that need to be added to the
   * environment
   * @return a new instance with the updated property values
   * @see TestPropertyValues
   * @see #withSystemProperties(String...)
   */
  public SELF withPropertyValues(String... pairs) {
    return newInstance(this.runnerConfiguration.withPropertyValues(pairs));
  }

  /**
   * Add the specified {@link System} property pairs. Key-value pairs can be specified
   * with colon (":") or equals ("=") separators. System properties are added before the
   * context is {@link #run(ContextConsumer) run} and restored when the context is
   * closed.
   *
   * @param pairs the key-value pairs for properties that need to be added to the system
   * @return a new instance with the updated system properties
   * @see TestPropertyValues
   * @see #withSystemProperties(String...)
   */
  public SELF withSystemProperties(String... pairs) {
    return newInstance(this.runnerConfiguration.withSystemProperties(pairs));
  }

  /**
   * Customize the {@link ClassLoader} that the {@link ApplicationContext} should use
   * for resource loading and bean class loading.
   *
   * @param classLoader the classloader to use (or {@code null} to use the default)
   * @return a new instance with the updated class loader
   * @see FilteredClassLoader
   */
  public SELF withClassLoader(ClassLoader classLoader) {
    return newInstance(this.runnerConfiguration.withClassLoader(classLoader));
  }

  /**
   * Configure the {@link ConfigurableApplicationContext#setParent(ApplicationContext)
   * parent} of the {@link ApplicationContext}.
   *
   * @param parent the parent
   * @return a new instance with the updated parent
   */
  public SELF withParent(ApplicationContext parent) {
    return newInstance(this.runnerConfiguration.withParent(parent));
  }

  /**
   * Register the specified user bean with the {@link ApplicationContext}. The bean name
   * is generated from the configured {@link BeanNameGenerator} on the underlying
   * context.
   * <p>
   * Such beans are registered after regular {@linkplain #withUserConfiguration(Class[])
   * user configurations} in the order of registration.
   *
   * @param type the type of the bean
   * @param constructorArgs custom argument values to be fed into Infra constructor
   * resolution algorithm, resolving either all arguments or just specific ones, with
   * the rest to be resolved through regular autowiring (may be {@code null} or empty)
   * @param <T> the type of the bean
   * @return a new instance with the updated bean
   */
  public <T> SELF withBean(Class<T> type, Object... constructorArgs) {
    return withBean(null, type, constructorArgs);
  }

  /**
   * Register the specified user bean with the {@link ApplicationContext}.
   * <p>
   * Such beans are registered after regular {@linkplain #withUserConfiguration(Class[])
   * user configurations} in the order of registration.
   *
   * @param name the bean name or {@code null} to use a generated name
   * @param type the type of the bean
   * @param constructorArgs custom argument values to be fed into Infra constructor
   * resolution algorithm, resolving either all arguments or just specific ones, with
   * the rest to be resolved through regular autowiring (may be {@code null} or empty)
   * @param <T> the type of the bean
   * @return a new instance with the updated bean
   */
  public <T> SELF withBean(String name, Class<T> type, Object... constructorArgs) {
    return newInstance(this.runnerConfiguration.withBean(name, type, constructorArgs));
  }

  /**
   * Register the specified user bean with the {@link ApplicationContext}. The bean name
   * is generated from the configured {@link BeanNameGenerator} on the underlying
   * context.
   * <p>
   * Such beans are registered after regular {@linkplain #withUserConfiguration(Class[])
   * user configurations} in the order of registration.
   *
   * @param type the type of the bean
   * @param supplier a supplier for the bean
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   * @param <T> the type of the bean
   * @return a new instance with the updated bean
   */
  public <T> SELF withBean(Class<T> type, Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {
    return withBean(null, type, supplier, customizers);
  }

  /**
   * Register the specified user bean with the {@link ApplicationContext}. The bean name
   * is generated from the configured {@link BeanNameGenerator} on the underlying
   * context.
   * <p>
   * Such beans are registered after regular {@linkplain #withUserConfiguration(Class[])
   * user configurations} in the order of registration.
   *
   * @param name the bean name or {@code null} to use a generated name
   * @param type the type of the bean
   * @param supplier a supplier for the bean
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   * @param <T> the type of the bean
   * @return a new instance with the updated bean
   */
  public <T> SELF withBean(String name, Class<T> type, Supplier<T> supplier,
          BeanDefinitionCustomizer... customizers) {
    return newInstance(this.runnerConfiguration.withBean(name, type, supplier, customizers));
  }

  /**
   * Register the specified user configuration classes with the
   * {@link ApplicationContext}.
   *
   * @param configurationClasses the user configuration classes to add
   * @return a new instance with the updated configuration
   */
  public SELF withUserConfiguration(Class<?>... configurationClasses) {
    return withConfiguration(UserConfigurations.of(configurationClasses));
  }

  /**
   * Register the specified configuration classes with the {@link ApplicationContext}.
   *
   * @param configurations the configurations to add
   * @return a new instance with the updated configuration
   */
  public SELF withConfiguration(Configurations configurations) {
    Assert.notNull(configurations, "Configurations is required");
    return newInstance(this.runnerConfiguration.withConfiguration(configurations));
  }

  /**
   * Apply customization to this runner.
   *
   * @param customizer the customizer to call
   * @return a new instance with the customizations applied
   */
  @SuppressWarnings("unchecked")
  public SELF with(Function<SELF, SELF> customizer) {
    return customizer.apply((SELF) this);
  }

  private SELF newInstance(RunnerConfiguration<C> runnerConfiguration) {
    return this.instanceFactory.apply(runnerConfiguration);
  }

  /**
   * Create and refresh a new {@link ApplicationContext} based on the current state of
   * this loader. The context is consumed by the specified {@code consumer} and closed
   * upon completion.
   *
   * @param consumer the consumer of the created {@link ApplicationContext}
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public SELF run(ContextConsumer<? super A> consumer) {
    withContextClassLoader(this.runnerConfiguration.classLoader, () -> this.runnerConfiguration.systemProperties
            .applyToSystemProperties(() -> consumeAssertableContext(true, consumer)));
    return (SELF) this;
  }

  /**
   * Prepare a new {@link ApplicationContext} based on the current state of this loader.
   * The context is consumed by the specified {@code consumer} and closed upon
   * completion. Unlike {@link #run(ContextConsumer)}, this method does not refresh the
   * consumed context.
   *
   * @param consumer the consumer of the created {@link ApplicationContext}
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public SELF prepare(ContextConsumer<? super A> consumer) {
    withContextClassLoader(this.runnerConfiguration.classLoader, () -> this.runnerConfiguration.systemProperties
            .applyToSystemProperties(() -> consumeAssertableContext(false, consumer)));
    return (SELF) this;
  }

  private void consumeAssertableContext(boolean refresh, ContextConsumer<? super A> consumer) {
    try (A context = createAssertableContext(refresh)) {
      accept(consumer, context);
    }
  }

  private void withContextClassLoader(ClassLoader classLoader, Runnable action) {
    if (classLoader == null) {
      action.run();
    }
    else {
      Thread currentThread = Thread.currentThread();
      ClassLoader previous = currentThread.getContextClassLoader();
      currentThread.setContextClassLoader(classLoader);
      try {
        action.run();
      }
      finally {
        currentThread.setContextClassLoader(previous);
      }
    }
  }

  @SuppressWarnings({ "unchecked" })
  private A createAssertableContext(boolean refresh) {
    ResolvableType resolvableType = ResolvableType.forClass(AbstractApplicationContextRunner.class, getClass());
    Class<A> assertType = (Class<A>) resolvableType.resolveGeneric(1);
    Class<C> contextType = (Class<C>) resolvableType.resolveGeneric(2);
    return ApplicationContextAssertProvider.get(assertType, contextType, () -> createAndLoadContext(refresh),
            this.runnerConfiguration.additionalContextInterfaces);
  }

  private C createAndLoadContext(boolean refresh) {
    C context = this.runnerConfiguration.contextFactory.get();
    ConfigurableBeanFactory beanFactory = context.getBeanFactory();
    if (beanFactory instanceof AbstractAutowireCapableBeanFactory autowireCapableBeanFactory) {
      autowireCapableBeanFactory.setAllowCircularReferences(this.runnerConfiguration.allowCircularReferences);
      if (beanFactory instanceof StandardBeanFactory stdBeanFactory) {
        stdBeanFactory.setAllowBeanDefinitionOverriding(
                this.runnerConfiguration.allowBeanDefinitionOverriding);
      }
    }
    try {
      configureContext(context, refresh);
      return context;
    }
    catch (RuntimeException ex) {
      context.close();
      throw ex;
    }
  }

  private void configureContext(C context, boolean refresh) {
    if (this.runnerConfiguration.parent != null) {
      context.setParent(this.runnerConfiguration.parent);
    }
    if (this.runnerConfiguration.classLoader != null) {
      Assert.isInstanceOf(DefaultResourceLoader.class, context);
      ((DefaultResourceLoader) context).setClassLoader(this.runnerConfiguration.classLoader);
    }
    this.runnerConfiguration.environmentProperties.applyTo(context);
    this.runnerConfiguration.beanRegistrations.forEach((registration) -> registration.apply(context));
    this.runnerConfiguration.initializers.forEach((initializer) -> initializer.initialize(context));
    if (CollectionUtils.isNotEmpty(this.runnerConfiguration.configurations)) {
      BiConsumer<Class<?>, String> registrar = getRegistrar(context);
      for (Configurations configurations : Configurations.collate(this.runnerConfiguration.configurations)) {
        for (Class<?> beanClass : Configurations.getClasses(configurations)) {
          String beanName = configurations.getBeanName(beanClass);
          registrar.accept(beanClass, beanName);
        }
      }
    }
    if (refresh) {
      context.refresh();
    }
  }

  private BiConsumer<Class<?>, String> getRegistrar(C context) {
    if (context instanceof BeanDefinitionRegistry registry) {
      return new AnnotatedBeanDefinitionReader(registry, context.getEnvironment())::registerBean;
    }
    return (beanClass, beanName) -> ((AnnotationConfigRegistry) context).register(beanClass);
  }

  private void accept(ContextConsumer<? super A> consumer, A context) {
    try {
      consumer.accept(context);
    }
    catch (Throwable ex) {
      rethrow(ex);
    }
  }

  @SuppressWarnings("unchecked")
  private <E extends Throwable> void rethrow(Throwable e) throws E {
    throw (E) e;
  }

  /**
   * A Bean registration to be applied when the context loaded.
   *
   * @param <T> the bean type
   */
  protected static final class BeanRegistration<T> {

    Consumer<GenericApplicationContext> registrar;

    public BeanRegistration(String name, Class<T> type, Object... constructorArgs) {
      this.registrar = (context) -> context.registerBean(name, type, constructorArgs);
    }

    public BeanRegistration(String name, Class<T> type, Supplier<T> supplier,
            BeanDefinitionCustomizer... customizers) {
      this.registrar = (context) -> context.registerBean(name, type, supplier, customizers);
    }

    public void apply(ConfigurableApplicationContext context) {
      Assert.isInstanceOf(GenericApplicationContext.class, context);
      this.registrar.accept(((GenericApplicationContext) context));
    }

  }

  protected static final class RunnerConfiguration<C extends ConfigurableApplicationContext> {

    private final Supplier<C> contextFactory;

    private final Class<?>[] additionalContextInterfaces;

    private boolean allowBeanDefinitionOverriding = false;

    private boolean allowCircularReferences = false;

    private List<ApplicationContextInitializer> initializers = Collections.emptyList();

    private TestPropertyValues environmentProperties = TestPropertyValues.empty();

    private TestPropertyValues systemProperties = TestPropertyValues.empty();

    private ClassLoader classLoader;

    private ApplicationContext parent;

    private List<BeanRegistration<?>> beanRegistrations = Collections.emptyList();

    private List<Configurations> configurations = Collections.emptyList();

    private RunnerConfiguration(Supplier<C> contextFactory, Class<?>[] additionalContextInterfaces) {
      this.contextFactory = contextFactory;
      this.additionalContextInterfaces = additionalContextInterfaces;
    }

    private RunnerConfiguration(RunnerConfiguration<C> source) {
      this.contextFactory = source.contextFactory;
      this.additionalContextInterfaces = source.additionalContextInterfaces;
      this.allowBeanDefinitionOverriding = source.allowBeanDefinitionOverriding;
      this.allowCircularReferences = source.allowCircularReferences;
      this.initializers = source.initializers;
      this.environmentProperties = source.environmentProperties;
      this.systemProperties = source.systemProperties;
      this.classLoader = source.classLoader;
      this.parent = source.parent;
      this.beanRegistrations = source.beanRegistrations;
      this.configurations = source.configurations;
    }

    public RunnerConfiguration<C> withAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
      return config;
    }

    public RunnerConfiguration<C> withAllowCircularReferences(boolean allowCircularReferences) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.allowCircularReferences = allowCircularReferences;
      return config;
    }

    public RunnerConfiguration<C> withInitializer(ApplicationContextInitializer initializer) {
      Assert.notNull(initializer, "Initializer is required");
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.initializers = add(config.initializers, initializer);
      return config;
    }

    public RunnerConfiguration<C> withPropertyValues(String... pairs) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.environmentProperties = config.environmentProperties.and(pairs);
      return config;
    }

    public RunnerConfiguration<C> withSystemProperties(String... pairs) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.systemProperties = config.systemProperties.and(pairs);
      return config;
    }

    public RunnerConfiguration<C> withClassLoader(ClassLoader classLoader) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.classLoader = classLoader;
      return config;
    }

    public RunnerConfiguration<C> withParent(ApplicationContext parent) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.parent = parent;
      return config;
    }

    public <T> RunnerConfiguration<C> withBean(String name, Class<T> type, Object... constructorArgs) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.beanRegistrations = add(config.beanRegistrations,
              new BeanRegistration<>(name, type, constructorArgs));
      return config;
    }

    public <T> RunnerConfiguration<C> withBean(String name, Class<T> type, Supplier<T> supplier,
            BeanDefinitionCustomizer... customizers) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.beanRegistrations = add(config.beanRegistrations,
              new BeanRegistration<>(name, type, supplier, customizers));
      return config;
    }

    public RunnerConfiguration<C> withConfiguration(Configurations configurations) {
      Assert.notNull(configurations, "Configurations is required");
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.configurations = add(config.configurations, configurations);
      return config;
    }

    public static <T> List<T> add(List<T> list, T element) {
      List<T> result = new ArrayList<>(list);
      result.add(element);
      return result;
    }

  }

}
