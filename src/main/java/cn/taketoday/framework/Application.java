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

package cn.taketoday.framework;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.DependencyInjectorAwareInstantiator;
import cn.taketoday.context.AnnotationConfigRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.context.support.ApplicationPropertySourcesProcessor;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.env.CommandLinePropertySource;
import cn.taketoday.core.env.CompositePropertySource;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.SimpleCommandLinePropertySource;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.framework.diagnostics.ApplicationExceptionReporter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Class that can be used to bootstrap and launch a application from a Java main
 * method. By default, class will perform the following steps to bootstrap your
 * application:
 *
 * <ul>
 * <li>Create an appropriate {@link ApplicationContext} instance (depending on your
 * classpath)</li>
 * <li>Register a {@link CommandLinePropertySource} to expose command line arguments as
 * properties</li>
 * <li>Refresh the application context, loading all singleton beans</li>
 * <li>Trigger any {@link CommandLineRunner} beans</li>
 * </ul>
 *
 * In most circumstances the static {@link #run(Class, String[])} method can be called
 * directly from your {@literal main} method to bootstrap your application:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class MyApplication  {
 *
 *   // ... Bean definitions
 *
 *   public static void main(String[] args) {
 *     // Application.run(MyApplication.class, args);
 *     WebApplication.run(MyApplication.class, args);
 *   }
 * }
 * </pre>
 *
 * <p>
 * For more advanced configuration a {@link Application} instance can be created and
 * customized before being run:
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *   Application application = new Application(MyApplication.class);
 *   // ... customize application settings here
 *   application.run(args)
 * }
 * </pre>
 *
 * {@link Application}s can read beans from a {@code mainApplicationClass} or {@code configSources}
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Jeremy Rickard
 * @author Craig Burke
 * @author Michael Simons
 * @author Madhura Bhave
 * @author Brian Clozel
 * @author Ethan Rubinson
 * @author Chris Bono
 * @author TODAY 2021/10/5 23:49
 * @see #run(Class, String[])
 * @see #Application(Class...)
 * @since 4.0
 */
public class Application {
  public static final String PROPERTIES_BINDER_PREFIX = "today.main";
  private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Nullable
  private Class<?> mainApplicationClass;
  private final Class<?>[] configSources;

  private List<ApplicationContextInitializer> initializers;

  private ApplicationContextFactory applicationContextFactory = ApplicationContextFactory.DEFAULT;

  private ApplicationType applicationType;

  @Nullable
  private ConfigurableEnvironment environment;

  private Map<String, Object> defaultProperties;

  private Set<String> additionalProfiles = Collections.emptySet();

  private boolean headless = true;
  private boolean logStartupInfo = true;
  private boolean registerShutdownHook = true;
  private boolean addCommandLineProperties = true;

  private boolean addConversionService = true;

  public Application(Class<?>... configSources) {
    this.configSources = configSources;
    this.mainApplicationClass = deduceMainApplicationClass();
    this.applicationType = ApplicationType.deduceFromClasspath();
    setInitializers(TodayStrategies.getStrategies(ApplicationContextInitializer.class));
  }

  private Class<?> deduceMainApplicationClass() {
    try {
      StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
      for (StackTraceElement stackTraceElement : stackTrace) {
        if ("main".equals(stackTraceElement.getMethodName())) {
          return Class.forName(stackTraceElement.getClassName());
        }
      }
    }
    catch (ClassNotFoundException ex) {
      // Swallow and continue
    }
    return null;
  }

  /**
   * Returns the main application class that has been deduced or explicitly configured.
   *
   * @return the main application class or {@code null}
   */
  @Nullable
  public Class<?> getMainApplicationClass() {
    return this.mainApplicationClass;
  }

  /**
   * Set a specific main application class that will be used as a log source and to
   * obtain version information. By default the main application class will be deduced.
   * Can be set to {@code null} if there is no explicit application class.
   *
   * @param mainApplicationClass the mainApplicationClass to set or {@code null}
   */
  public void setMainApplicationClass(@Nullable Class<?> mainApplicationClass) {
    this.mainApplicationClass = mainApplicationClass;
  }

  /**
   * Returns the type of application that is being run.
   *
   * @return the type of application
   */
  public ApplicationType getApplicationType() {
    return applicationType;
  }

  /**
   * Sets the type of web application to be run. If not explicitly set the type of web
   * application will be deduced based on the classpath.
   *
   * @param applicationType the application type
   */
  public void setApplicationType(ApplicationType applicationType) {
    Assert.notNull(applicationType, "ApplicationType is required");
    this.applicationType = applicationType;
  }

  /**
   * Run the application, creating and refreshing a new
   * {@link ApplicationContext}.
   *
   * @param args the application arguments (usually passed from a Java main method)
   * @return a running {@link ApplicationContext}
   */
  public ConfigurableApplicationContext run(String... args) {
    long startTime = System.nanoTime();
    ApplicationArguments arguments = new ApplicationArguments(args);
    // prepare startup
    prepareStartup(arguments);

    ConfigurableApplicationContext context = null;
    ApplicationStartupListeners listeners = getStartupListeners();
    listeners.starting(mainApplicationClass, arguments);
    try {
      ConfigurableEnvironment environment = prepareEnvironment(listeners, arguments);

      context = createApplicationContext();
      // prepare context
      prepareContext(context, listeners, arguments, environment);
      // refresh context
      refreshContext(context);
      // after refresh
      afterRefresh(context, arguments);

      Duration timeTakenToStartup = Duration.ofNanos(System.nanoTime() - startTime);
      if (this.logStartupInfo) {
        new StartupLogging(this.mainApplicationClass).logStarted(getApplicationLog(), timeTakenToStartup);
      }
      listeners.started(context, timeTakenToStartup);
      callRunners(context, arguments);
    }
    catch (Throwable e) {
      handleRunFailure(context, e, listeners);
      if (context != null && context.isActive()) {
        context.close();
      }
      throw ExceptionUtils.sneakyThrow(e);
    }

    try {
      Duration timeTakenToReady = Duration.ofNanos(System.nanoTime() - startTime);
      listeners.ready(context, timeTakenToReady);
      return context;
    }
    catch (Throwable ex) {
      handleRunFailure(context, ex, null);
      throw new IllegalStateException(ex);
    }
  }

  private void refreshContext(ConfigurableApplicationContext context) {
    if (this.registerShutdownHook) {
      context.registerShutdownHook();
    }
    refresh(context);
  }

  /**
   * Refresh the underlying {@link ApplicationContext}.
   *
   * @param context the application context to refresh
   */
  protected void refresh(ConfigurableApplicationContext context) {
    context.refresh();
  }

  /**
   * Called after the context has been refreshed.
   *
   * @param context the application context
   * @param args the application arguments
   */
  protected void afterRefresh(ConfigurableApplicationContext context, ApplicationArguments args) { }

  private ApplicationStartupListeners getStartupListeners() {
    List<ApplicationStartupListener> strategies = TodayStrategies.getStrategies(ApplicationStartupListener.class);
    return new ApplicationStartupListeners(log, strategies);
  }

  private ConfigurableEnvironment prepareEnvironment(
          ApplicationStartupListeners listeners, ApplicationArguments applicationArguments) {
    // Create and configure the environment
    ConfigurableEnvironment environment = getOrCreateEnvironment();
    configureEnvironment(environment, applicationArguments.getSourceArgs());

    ConfigurationPropertySources.attach(environment);
    listeners.environmentPrepared(environment);
    DefaultPropertiesPropertySource.moveToEnd(environment);

    bindToApplication(environment);

    return environment;
  }

  /**
   * Bind the environment to the {@link Application}.
   *
   * @param environment the environment to bind
   */
  protected void bindToApplication(ConfigurableEnvironment environment) {
    try {
      Binder.get(environment).bind(PROPERTIES_BINDER_PREFIX, Bindable.ofInstance(this));
    }
    catch (Exception ex) {
      throw new IllegalStateException("Cannot bind to Application", ex);
    }
  }

  /**
   * Strategy method used to create the {@link ApplicationContext}. By default this
   * method will respect any explicitly set application context class or factory before
   * falling back to a suitable default.
   *
   * @return the application context (not yet refreshed)
   * @see #setApplicationContextFactory(ApplicationContextFactory)
   */
  protected ConfigurableApplicationContext createApplicationContext() {
    return this.applicationContextFactory.create(this.applicationType);
  }

  protected void prepareStartup(ApplicationArguments arguments) {
    configureHeadlessProperty();
  }

  private void prepareContext(
          ConfigurableApplicationContext context, ApplicationStartupListeners listeners,
          ApplicationArguments arguments, ConfigurableEnvironment environment) {
    context.setEnvironment(environment);

    postProcessApplicationContext(context);
    applyInitializers(context);

    listeners.contextPrepared(context);
    if (this.logStartupInfo) {
      logStartupInfo(context.getParent() == null);
      logStartupProfileInfo(context);
    }

    // Add specific singleton beans
    ConfigurableBeanFactory beanFactory = context.getBeanFactory();
    beanFactory.registerSingleton(this);
    beanFactory.registerSingleton(ApplicationArguments.BEAN_NAME, arguments);

    // loading context from config classes
    if (context instanceof AnnotationConfigRegistry configRegistry) {
      if (ObjectUtils.isNotEmpty(configSources)) {
        if (mainApplicationClass != null) {
          HashSet<Class<?>> classes = new HashSet<>(Set.of(configSources));
          classes.add(mainApplicationClass);
          for (Class<?> configSource : classes) {
            configRegistry.register(configSource); // @since 1.0.2 import startup class
          }
        }
        else {
          for (Class<?> configSource : configSources) {
            configRegistry.register(configSource); // @since 1.0.2 import startup class
          }
        }
      }
      else if (mainApplicationClass != null) {
        configRegistry.register(mainApplicationClass); // @since 1.0.2 import startup class
      }
    }

    listeners.contextLoaded(context);
  }

  /**
   * Apply any relevant post processing the {@link ApplicationContext}. Subclasses can
   * apply additional processing as required.
   *
   * @param context the application context
   */
  protected void postProcessApplicationContext(ConfigurableApplicationContext context) {

    if (this.addConversionService) {
      context.getBeanFactory().setConversionService(context.getEnvironment().getConversionService());
    }
  }

  private ConfigurableEnvironment getOrCreateEnvironment() {
    if (this.environment != null) {
      return this.environment;
    }
    return switch (applicationType) {
      case SERVLET_WEB -> new ApplicationServletEnvironment();
      case REACTIVE_WEB -> new ApplicationReactiveWebEnvironment();
      default -> new ApplicationEnvironment();
    };
  }

  /**
   * Template method delegating to
   * {@link #configurePropertySources(ConfigurableEnvironment, String[])} and
   * {@link #configureProfiles(ConfigurableEnvironment, String[])} in that order.
   * Override this method for complete control over Environment customization, or one of
   * the above for fine-grained control over property sources or profiles, respectively.
   *
   * @param environment this application's environment
   * @param args arguments passed to the {@code run} method
   * @see #configureProfiles(ConfigurableEnvironment, String[])
   * @see #configurePropertySources(ConfigurableEnvironment, String[])
   */
  protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
    if (this.addConversionService) {
      environment.setConversionService(new ApplicationConversionService());
    }
    configurePropertySources(environment, args);
    configureProfiles(environment, args);
  }

  /**
   * Configure which profiles are active (or active by default) for this application
   * environment. Additional profiles may be activated during configuration file
   * processing via the {@code context.profiles.active} property.
   *
   * @param environment this application's environment
   * @param args arguments passed to the {@code run} method
   * @see #configureEnvironment(ConfigurableEnvironment, String[])
   */
  protected void configureProfiles(ConfigurableEnvironment environment, String[] args) { }

  /**
   * Add, remove or re-order any {@link PropertySource}s in this application's
   * environment.
   *
   * @param environment this application's environment
   * @param args arguments passed to the {@code run} method
   * @see #configureEnvironment(ConfigurableEnvironment, String[])
   */
  protected void configurePropertySources(ConfigurableEnvironment environment, String[] args) {
    // load application.properties or application.yaml, application.yml
    try {
      new ApplicationPropertySourcesProcessor().postProcessEnvironment(environment);
    }
    catch (IOException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }

    PropertySources sources = environment.getPropertySources();
    if (CollectionUtils.isNotEmpty(this.defaultProperties)) {
      DefaultPropertiesPropertySource.addOrMerge(this.defaultProperties, sources);
    }

    // load outside PropertySources
    List<EnvironmentPostProcessor> postProcessors = getEnvironmentPostProcessors();
    for (EnvironmentPostProcessor postProcessor : postProcessors) {
      postProcessor.postProcessEnvironment(environment, this);
    }

    // CommandLine
    if (this.addCommandLineProperties && args.length > 0) {
      String name = CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME;
      if (sources.contains(name)) {
        PropertySource<?> source = sources.get(name);
        CompositePropertySource composite = new CompositePropertySource(name);
        composite.addPropertySource(
                new SimpleCommandLinePropertySource("applicationCommandLineArgs", args));
        composite.addPropertySource(source);
        sources.replace(name, composite);
      }
      else {
        sources.addFirst(new SimpleCommandLinePropertySource(args));
      }
    }
  }

  protected List<EnvironmentPostProcessor> getEnvironmentPostProcessors() {
    return TodayStrategies.getStrategies(EnvironmentPostProcessor.class);
  }

  /**
   * Sets the factory that will be called to create the application context.
   *
   * @param applicationContextFactory the factory for the context
   */
  public void setApplicationContextFactory(
          ApplicationContextFactory applicationContextFactory) {
    this.applicationContextFactory =
            applicationContextFactory != null
            ? applicationContextFactory : ApplicationContextFactory.DEFAULT;
  }

  /**
   * Sets the {@link ApplicationContextInitializer} that will be applied to the
   * {@link ApplicationContext}.
   *
   * @param initializers the initializers to set
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void setInitializers(Collection<ApplicationContextInitializer> initializers) {
    this.initializers = new ArrayList(initializers);
  }

  /**
   * Add {@link ApplicationContextInitializer}s to be applied to the
   * {@link ApplicationContext}.
   *
   * @param initializers the initializers to add
   */
  public void addInitializers(ApplicationContextInitializer... initializers) {
    CollectionUtils.addAll(this.initializers, initializers);
  }

  /**
   * Returns read-only ordered Set of the {@link ApplicationContextInitializer}s that
   * will be applied to the {@link ApplicationContext}.
   *
   * @return the initializers
   */
  public Set<ApplicationContextInitializer> getInitializers() {
    return asUnmodifiableOrderedSet(this.initializers);
  }

  /**
   * Apply any {@link ApplicationContextInitializer}s to the context before it is
   * refreshed.
   *
   * @param context the configured ApplicationContext (not refreshed yet)
   * @see ConfigurableApplicationContext#refresh()
   */
  protected void applyInitializers(ConfigurableApplicationContext context) {
    for (ApplicationContextInitializer initializer : getInitializers()) {
      initializer.initialize(context);
    }
  }

  /**
   * Called to log startup information, subclasses may override to add additional
   * logging.
   *
   * @param isRoot true if this application is the root of a context hierarchy
   */
  protected void logStartupInfo(boolean isRoot) {
    if (isRoot) {
      new StartupLogging(this.mainApplicationClass).logStarting(getApplicationLog());
    }
  }

  /**
   * Called to log active profile information.
   *
   * @param context the application context
   */
  protected void logStartupProfileInfo(ConfigurableApplicationContext context) {
    Logger log = getApplicationLog();
    if (log.isInfoEnabled()) {
      List<String> activeProfiles = quoteProfiles(context.getEnvironment().getActiveProfiles());
      if (ObjectUtils.isEmpty(activeProfiles)) {
        List<String> defaultProfiles = quoteProfiles(context.getEnvironment().getDefaultProfiles());
        String message = String.format("%s default %s: ", defaultProfiles.size(),
                (defaultProfiles.size() <= 1) ? "profile" : "profiles");
        log.info("No active profile set, falling back to " + message
                + StringUtils.collectionToDelimitedString(defaultProfiles, ", "));
      }
      else {
        String message = (activeProfiles.size() == 1) ? "1 profile is active: "
                                                      : activeProfiles.size() + " profiles are active: ";
        log.info("The following " + message + StringUtils.collectionToDelimitedString(activeProfiles, ", "));
      }
    }
  }

  private List<String> quoteProfiles(String[] profiles) {
    return Arrays.stream(profiles).map((profile) -> "\"" + profile + "\"").collect(Collectors.toList());
  }

  /**
   * Returns the {@link Logger} for the application. By default will be deduced.
   *
   * @return the application log
   */
  protected Logger getApplicationLog() {
    if (this.mainApplicationClass == null) {
      return log;
    }
    return LoggerFactory.getLogger(this.mainApplicationClass);
  }

  /**
   * Sets if the application is headless and should not instantiate AWT. Defaults to
   * {@code true} to prevent java icons appearing.
   *
   * @param headless if the application is headless
   */
  public void setHeadless(boolean headless) {
    this.headless = headless;
  }

  /**
   * Sets if the created {@link ApplicationContext} should have a shutdown hook
   * registered. Defaults to {@code true} to ensure that JVM shutdowns are handled
   * gracefully.
   *
   * @param registerShutdownHook if the shutdown hook should be registered
   */
  public void setRegisterShutdownHook(boolean registerShutdownHook) {
    this.registerShutdownHook = registerShutdownHook;
  }

  private void configureHeadlessProperty() {
    System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, System.getProperty(
            SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(this.headless)));
  }

  /**
   * Sets the underlying environment that should be used with the created application
   * context.
   *
   * @param environment the environment
   */
  public void setEnvironment(@Nullable ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  /**
   * Sets if a {@link CommandLinePropertySource} should be added to the application
   * context in order to expose arguments. Defaults to {@code true}.
   *
   * @param addCommandLineProperties if command line arguments should be exposed
   */
  public void setAddCommandLineProperties(boolean addCommandLineProperties) {
    this.addCommandLineProperties = addCommandLineProperties;
  }

  /**
   * Sets if the application information should be logged when the application starts.
   * Defaults to {@code true}.
   *
   * @param logStartupInfo if startup info should be logged.
   */
  public void setLogStartupInfo(boolean logStartupInfo) {
    this.logStartupInfo = logStartupInfo;
  }

  /**
   * Sets if the {@link ApplicationConversionService} should be added to the application
   * context's {@link Environment}.
   *
   * @param addConversionService if the application conversion service should be added
   */
  public void setAddConversionService(boolean addConversionService) {
    this.addConversionService = addConversionService;
  }

  /**
   * Set default environment properties which will be used in addition to those in the
   * existing {@link Environment}.
   *
   * @param defaultProperties the additional properties to set
   */
  public void setDefaultProperties(@Nullable Map<String, Object> defaultProperties) {
    this.defaultProperties = defaultProperties;
  }

  /**
   * Set additional profile values to use (on top of those set in system or command line
   * properties).
   *
   * @param profiles the additional profiles to set
   */
  public void setAdditionalProfiles(String... profiles) {
    this.additionalProfiles = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(profiles)));
  }

  /**
   * Return an immutable set of any additional profiles in use.
   *
   * @return the additional profiles
   */
  public Set<String> getAdditionalProfiles() {
    return this.additionalProfiles;
  }

  /**
   * Convenient alternative to {@link #setDefaultProperties(Map)}.
   *
   * @param defaultProperties some {@link Properties}
   */
  public void setDefaultProperties(Properties defaultProperties) {
    this.defaultProperties = new HashMap<>();
    for (Object key : Collections.list(defaultProperties.propertyNames())) {
      this.defaultProperties.put((String) key, defaultProperties.get(key));
    }
  }

  private void handleRunFailure(
          ConfigurableApplicationContext context, Throwable exception, @Nullable ApplicationStartupListeners listeners) {
    try {
      try {
        handleExitCode(context, exception);
        if (listeners != null) {
          listeners.failed(context, exception);
        }
      }
      finally {
        reportFailure(getExceptionReporters(context), exception);
        if (context != null) {
          context.close();
        }
      }
    }
    catch (Exception ex) {
      log.warn("Unable to close ApplicationContext", ex);
    }
  }

  private List<ApplicationExceptionReporter> getExceptionReporters(ConfigurableApplicationContext context) {
    try {
      return TodayStrategies.getStrategies(
              ApplicationExceptionReporter.class, DependencyInjectorAwareInstantiator.forFunction(context));
    }
    catch (Throwable ex) {
      return Collections.emptyList();
    }
  }

  private void reportFailure(List<ApplicationExceptionReporter> exceptionReporters, Throwable failure) {
    try {
      for (ApplicationExceptionReporter reporter : exceptionReporters) {
        if (reporter.reportException(failure)) {
          registerLoggedException(failure);
          return;
        }
      }
    }
    catch (Throwable ex) {
      // Continue with normal handling of the original failure
    }
    if (log.isErrorEnabled()) {
      log.error("Application run failed", failure);
      registerLoggedException(failure);
    }
  }

  /**
   * Register that the given exception has been logged. By default, if the running in
   * the main thread, this method will suppress additional printing of the stacktrace.
   *
   * @param exception the exception that was logged
   */
  protected void registerLoggedException(Throwable exception) {
    ApplicationExceptionHandler handler = getApplicationExceptionHandler();
    if (handler != null) {
      handler.registerLoggedException(exception);
    }
  }

  ApplicationExceptionHandler getApplicationExceptionHandler() {
    if (isMainThread(Thread.currentThread())) {
      return ApplicationExceptionHandler.forCurrentThread();
    }
    return null;
  }

  private boolean isMainThread(Thread currentThread) {
    return ("main".equals(currentThread.getName())
            || "restartedMain".equals(currentThread.getName()))
            && "main".equals(currentThread.getThreadGroup().getName());
  }

  private void handleExitCode(ConfigurableApplicationContext context, Throwable exception) {
    int exitCode = getExitCodeFromException(context, exception);
    if (exitCode != 0) {
      if (context != null) {
        context.publishEvent(new ExitCodeEvent(context, exitCode));
      }
    }
  }

  private int getExitCodeFromException(ConfigurableApplicationContext context, Throwable exception) {
    int exitCode = getExitCodeFromMappedException(context, exception);
    if (exitCode == 0) {
      exitCode = getExitCodeFromExitCodeGeneratorException(exception);
    }
    return exitCode;
  }

  private int getExitCodeFromMappedException(ConfigurableApplicationContext context, Throwable exception) {
    if (context == null || context.getState() == ApplicationContext.State.NONE) {
      return 0;
    }
    ExitCodeGenerators generators = new ExitCodeGenerators();
    List<ExitCodeExceptionMapper> beans = context.getBeanFactory().getBeans(ExitCodeExceptionMapper.class);
    generators.addAll(exception, beans);
    return generators.getExitCode();
  }

  private int getExitCodeFromExitCodeGeneratorException(Throwable exception) {
    if (exception == null) {
      return 0;
    }
    if (exception instanceof ExitCodeGenerator) {
      return ((ExitCodeGenerator) exception).getExitCode();
    }
    return getExitCodeFromExitCodeGeneratorException(exception.getCause());
  }

  private void callRunners(ApplicationContext context, ApplicationArguments args) {
    ArrayList<Object> runners = new ArrayList<>();
    runners.addAll(context.getBeansOfType(ApplicationRunner.class).values());
    runners.addAll(context.getBeansOfType(CommandLineRunner.class).values());
    AnnotationAwareOrderComparator.sort(runners);

    String[] sourceArgs = args.getSourceArgs();
    for (Object runner : new LinkedHashSet<>(runners)) {
      if (runner instanceof ApplicationRunner applicationRunner) {
        try {
          applicationRunner.run(args);
        }
        catch (Exception ex) {
          throw new IllegalStateException("Failed to execute ApplicationRunner", ex);
        }
      }

      if (runner instanceof CommandLineRunner commandLineRunner) {
        try {
          commandLineRunner.run(sourceArgs);
        }
        catch (Exception ex) {
          throw new IllegalStateException("Failed to execute CommandLineRunner", ex);
        }
      }
    }
  }

  /**
   * Static helper that can be used to run a {@link Application} from the
   * specified sources using default settings and user supplied arguments.
   *
   * @param configClass the primary sources to load
   * @param args the application arguments (usually passed from a Java main method)
   * @return the running {@link ApplicationContext}
   */
  public static ConfigurableApplicationContext run(Class<?> configClass, String... args) {
    return new Application(configClass).run(args);
  }

  /**
   * Static helper that can be used to exit a {@link Application} and obtain a
   * code indicating success (0) or otherwise. Does not throw exceptions but should
   * print stack traces of any encountered. Applies the specified
   * {@link ExitCodeGenerator} in addition to any Framework beans that implement
   * {@link ExitCodeGenerator}. In the case of multiple exit codes the highest value
   * will be used (or if all values are negative, the lowest value will be used)
   *
   * @param context the context to close if possible
   * @param exitCodeGenerators exit code generators
   * @return the outcome (0 if successful)
   */
  public static int exit(ApplicationContext context, ExitCodeGenerator... exitCodeGenerators) {
    Assert.notNull(context, "Context must not be null");
    int exitCode = 0;
    try {
      try {
        ExitCodeGenerators generators = new ExitCodeGenerators();
        Collection<ExitCodeGenerator> beans = context.getBeansOfType(ExitCodeGenerator.class).values();
        generators.addAll(exitCodeGenerators);
        generators.addAll(beans);
        exitCode = generators.getExitCode();
        if (exitCode != 0) {
          context.publishEvent(new ExitCodeEvent(context, exitCode));
        }
      }
      finally {
        close(context);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
      exitCode = (exitCode != 0) ? exitCode : 1;
    }
    return exitCode;
  }

  private static void close(ApplicationContext context) {
    if (context instanceof ConfigurableApplicationContext closable) {
      closable.close();
    }
  }

  private static <E> Set<E> asUnmodifiableOrderedSet(Collection<E> elements) {
    ArrayList<E> list = new ArrayList<>(elements);
    list.sort(AnnotationAwareOrderComparator.INSTANCE);
    return new LinkedHashSet<>(list);
  }

}
