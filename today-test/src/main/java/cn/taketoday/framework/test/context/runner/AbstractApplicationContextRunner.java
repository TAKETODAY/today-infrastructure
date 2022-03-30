/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.test.context.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.AbstractAutowireCapableBeanFactory;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.AnnotationConfigRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.config.Configurations;
import cn.taketoday.context.annotation.config.UserConfigurations;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.framework.test.context.FilteredClassLoader;
import cn.taketoday.framework.test.context.assertj.ApplicationContextAssert;
import cn.taketoday.framework.test.context.assertj.ApplicationContextAssertProvider;
import cn.taketoday.framework.test.util.TestPropertyValues;
import cn.taketoday.lang.Assert;

/**
 * Utility design to run an {@link ApplicationContext} and provide AssertJ style
 * assertions. The test is best used as a field of a test class, describing the shared
 * configuration required for the test:
 *
 * <pre class="code">
 * public class MyContextTests {
 *     private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
 *             .withPropertyValues("spring.foo=bar")
 *             .withUserConfiguration(MyConfiguration.class);
 * }</pre>
 *
 * <p>
 * The initialization above makes sure to register {@code MyConfiguration} for all tests
 * and set the {@code spring.foo} property to {@code bar} unless specified otherwise.
 * <p>
 * Based on the configuration above, a specific test can simulate what will happen when
 * the context runs, perhaps with overridden property values:
 *
 * <pre class="code">
 * &#064;Test
 * public someTest() {
 *     this.contextRunner.withPropertyValues("spring.foo=biz").run((context) -&gt; {
 *         assertThat(context).containsSingleBean(MyBean.class);
 *         // other assertions
 *     });
 * }</pre>
 * <p>
 * The test above has changed the {@code spring.foo} property to {@code biz} and is
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
 *     this.context.withPropertyValues("spring.foo=fails").run((loaded) -&gt; {
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
public abstract class AbstractApplicationContextRunner<SELF extends AbstractApplicationContextRunner<SELF, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> {

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
    Assert.notNull(contextFactory, "ContextFactory must not be null");
    Assert.notNull(contextFactory, "RunnerConfiguration must not be null");
    this.runnerConfiguration = new RunnerConfiguration<>(contextFactory);
    this.instanceFactory = instanceFactory;
  }

  /**
   * Create a new {@link AbstractApplicationContextRunner} instance.
   *
   * @param configuration the configuration for the runner to use
   * @param instanceFactory the factory used to create new instance of the runner
   */
  protected AbstractApplicationContextRunner(RunnerConfiguration<C> configuration,
          Function<RunnerConfiguration<C>, SELF> instanceFactory) {
    Assert.notNull(configuration, "RunnerConfiguration must not be null");
    Assert.notNull(instanceFactory, "instanceFactory must not be null");
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
    Assert.notNull(initializer, "Initializer must not be null");
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
   * @param classLoader the classloader to use (can be null to use the default)
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
   * @param constructorArgs custom argument values to be fed into Spring's constructor
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
   * @param constructorArgs custom argument values to be fed into Spring's constructor
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
    Assert.notNull(configurations, "Configurations must not be null");
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
    withContextClassLoader(this.runnerConfiguration.classLoader,
            () -> this.runnerConfiguration.systemProperties.applyToSystemProperties(() -> {
              try (A context = createAssertableContext()) {
                accept(consumer, context);
              }
              return null;
            }));
    return (SELF) this;
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

  @SuppressWarnings("unchecked")
  private A createAssertableContext() {
    ResolvableType resolvableType = ResolvableType.fromClass(AbstractApplicationContextRunner.class, getClass());
    Class<A> assertType = (Class<A>) resolvableType.resolveGeneric(1);
    Class<C> contextType = (Class<C>) resolvableType.resolveGeneric(2);
    return ApplicationContextAssertProvider.get(assertType, contextType, this::createAndLoadContext);
  }

  private C createAndLoadContext() {
    C context = this.runnerConfiguration.contextFactory.get();
    ConfigurableBeanFactory beanFactory = context.getBeanFactory();
    if (beanFactory instanceof AbstractAutowireCapableBeanFactory) {
      ((AbstractAutowireCapableBeanFactory) beanFactory)
              .setAllowCircularReferences(this.runnerConfiguration.allowCircularReferences);
      if (beanFactory instanceof StandardBeanFactory) {
        ((StandardBeanFactory) beanFactory)
                .setAllowBeanDefinitionOverriding(this.runnerConfiguration.allowBeanDefinitionOverriding);
      }
    }
    try {
      configureContext(context);
      return context;
    }
    catch (RuntimeException ex) {
      context.close();
      throw ex;
    }
  }

  private void configureContext(C context) {
    if (this.runnerConfiguration.parent != null) {
      context.setParent(this.runnerConfiguration.parent);
    }
    if (this.runnerConfiguration.classLoader != null) {
      Assert.isInstanceOf(DefaultResourceLoader.class, context);
      ((DefaultResourceLoader) context).setClassLoader(this.runnerConfiguration.classLoader);
    }
    this.runnerConfiguration.environmentProperties.applyTo(context);
    Class<?>[] classes = Configurations.getClasses(this.runnerConfiguration.configurations);
    if (classes.length > 0) {
      ((AnnotationConfigRegistry) context).register(classes);
    }
    this.runnerConfiguration.beanRegistrations.forEach((registration) -> registration.apply(context));
    this.runnerConfiguration.initializers.forEach((initializer) -> initializer.initialize(context));
    context.refresh();
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

    private boolean allowBeanDefinitionOverriding = false;

    private boolean allowCircularReferences = false;

    private List<ApplicationContextInitializer> initializers = Collections.emptyList();

    private TestPropertyValues environmentProperties = TestPropertyValues.empty();

    private TestPropertyValues systemProperties = TestPropertyValues.empty();

    private ClassLoader classLoader;

    private ApplicationContext parent;

    private List<BeanRegistration<?>> beanRegistrations = Collections.emptyList();

    private List<Configurations> configurations = Collections.emptyList();

    private RunnerConfiguration(Supplier<C> contextFactory) {
      this.contextFactory = contextFactory;
    }

    private RunnerConfiguration(RunnerConfiguration<C> source) {
      this.contextFactory = source.contextFactory;
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

    private RunnerConfiguration<C> withAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
      return config;
    }

    private RunnerConfiguration<C> withAllowCircularReferences(boolean allowCircularReferences) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.allowCircularReferences = allowCircularReferences;
      return config;
    }

    private RunnerConfiguration<C> withInitializer(ApplicationContextInitializer initializer) {
      Assert.notNull(initializer, "Initializer must not be null");
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.initializers = add(config.initializers, initializer);
      return config;
    }

    private RunnerConfiguration<C> withPropertyValues(String... pairs) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.environmentProperties = config.environmentProperties.and(pairs);
      return config;
    }

    private RunnerConfiguration<C> withSystemProperties(String... pairs) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.systemProperties = config.systemProperties.and(pairs);
      return config;
    }

    private RunnerConfiguration<C> withClassLoader(ClassLoader classLoader) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.classLoader = classLoader;
      return config;
    }

    private RunnerConfiguration<C> withParent(ApplicationContext parent) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.parent = parent;
      return config;
    }

    private <T> RunnerConfiguration<C> withBean(String name, Class<T> type, Object... constructorArgs) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.beanRegistrations = add(config.beanRegistrations,
              new BeanRegistration<>(name, type, constructorArgs));
      return config;
    }

    private <T> RunnerConfiguration<C> withBean(String name, Class<T> type, Supplier<T> supplier,
            BeanDefinitionCustomizer... customizers) {
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.beanRegistrations = add(config.beanRegistrations,
              new BeanRegistration<>(name, type, supplier, customizers));
      return config;
    }

    private RunnerConfiguration<C> withConfiguration(Configurations configurations) {
      Assert.notNull(configurations, "Configurations must not be null");
      RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
      config.configurations = add(config.configurations, configurations);
      return config;
    }

    private static <T> List<T> add(List<T> list, T element) {
      List<T> result = new ArrayList<>(list);
      result.add(element);
      return result;
    }

  }

}
