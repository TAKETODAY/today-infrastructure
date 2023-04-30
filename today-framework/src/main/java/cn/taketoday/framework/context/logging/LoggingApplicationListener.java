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

package cn.taketoday.framework.context.logging;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.context.event.GenericApplicationListener;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.context.event.ApplicationEnvironmentPreparedEvent;
import cn.taketoday.framework.context.event.ApplicationFailedEvent;
import cn.taketoday.framework.context.event.ApplicationPreparedEvent;
import cn.taketoday.framework.context.event.ApplicationStartingEvent;
import cn.taketoday.framework.logging.LogFile;
import cn.taketoday.framework.logging.LogLevel;
import cn.taketoday.framework.logging.LoggerGroup;
import cn.taketoday.framework.logging.LoggerGroups;
import cn.taketoday.framework.logging.LoggingStartupContext;
import cn.taketoday.framework.logging.LoggingSystem;
import cn.taketoday.framework.logging.LoggingSystemProperties;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;

/**
 * An {@link ApplicationListener} that configures the {@link LoggingSystem}. If the
 * environment contains a {@code logging.config} property it will be used to bootstrap the
 * logging system, otherwise a default configuration is used. Regardless, logging levels
 * will be customized if the environment contains {@code logging.level.*} entries and
 * logging groups can be defined with {@code logging.group}.
 * <p>
 * Debug and trace logging for Infra, Tomcat, Jetty and Hibernate will be enabled when
 * the environment contains {@code debug} or {@code trace} properties that aren't set to
 * {@code "false"} (i.e. if you start your application using
 * {@literal java -jar myapp.jar [--debug | --trace]}). If you prefer to ignore these
 * properties you can set {@link #setParseArgs(boolean) parseArgs} to {@code false}.
 * <p>
 * By default, log output is only written to the console. If a log file is required, the
 * {@code logging.file.path} and {@code logging.file.name} properties can be used.
 * <p>
 * Some system properties may be set as side effects, and these can be useful if the
 * logging configuration supports placeholders (i.e. log4j or logback):
 * <ul>
 * <li>{@code LOG_FILE} is set to the value of path of the log file that should be written
 * (if any).</li>
 * <li>{@code PID} is set to the value of the current process ID if it can be determined.
 * </li>
 * </ul>
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author HaiTao Zhang
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LoggingSystem#get(ClassLoader)
 * @since 4.0
 */
public class LoggingApplicationListener implements GenericApplicationListener {

  private static final ConfigurationPropertyName LOGGING_LEVEL = ConfigurationPropertyName.of("logging.level");

  private static final ConfigurationPropertyName LOGGING_GROUP = ConfigurationPropertyName.of("logging.group");

  private static final Bindable<Map<String, LogLevel>> STRING_LOGLEVEL_MAP = Bindable.mapOf(String.class,
          LogLevel.class);

  private static final Bindable<Map<String, List<String>>> STRING_STRINGS_MAP = Bindable
          .of(ResolvableType.fromClassWithGenerics(MultiValueMap.class, String.class, String.class).asMap());

  /**
   * The default order for the LoggingApplicationListener.
   */
  public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

  /**
   * The name of the Infra property that contains a reference to the logging
   * configuration to load.
   */
  public static final String CONFIG_PROPERTY = "logging.config";

  /**
   * The name of the Infra property that controls the registration of a shutdown hook
   * to shut down the logging system when the JVM exits.
   *
   * @see LoggingSystem#getShutdownHandler
   */
  public static final String REGISTER_SHUTDOWN_HOOK_PROPERTY = "logging.register-shutdown-hook";

  /**
   * The name of the {@link LoggingSystem} bean.
   */
  public static final String LOGGING_SYSTEM_BEAN_NAME = "infraLoggingSystem";

  /**
   * The name of the {@link LogFile} bean.
   */
  public static final String LOG_FILE_BEAN_NAME = "infraLogFile";

  /**
   * The name of the {@link LoggerGroups} bean.
   */
  public static final String LOGGER_GROUPS_BEAN_NAME = "infraLoggerGroups";

  /**
   * The name of the {@link Lifecycle} bean used to handle cleanup.
   */
  private static final String LOGGING_LIFECYCLE_BEAN_NAME = "infraLoggingLifecycle";

  private static final Map<String, List<String>> DEFAULT_GROUP_LOGGERS;

  static {
    MultiValueMap<String, String> loggers = new LinkedMultiValueMap<>();
    loggers.add("web", "cn.taketoday.core.codec");
    loggers.add("web", "cn.taketoday.http");
    loggers.add("web", "cn.taketoday.web");
    loggers.add("web", "cn.taketoday.framework.actuate.endpoint.web");
    loggers.add("web", "cn.taketoday.framework.web.servlet.ServletContextInitializerBeans");
    loggers.add("sql", "cn.taketoday.jdbc.core");
    loggers.add("sql", "org.hibernate.SQL");
    loggers.add("sql", "today.infra.SQL");
    loggers.add("sql", "today.infra.SQL_SLOW");
    loggers.add("sql", "org.jooq.tools.LoggerListener");
    DEFAULT_GROUP_LOGGERS = Collections.unmodifiableMap(loggers);
  }

  private static final Map<LogLevel, List<String>> INFRA_LOGGING_LOGGERS;

  static {
    MultiValueMap<LogLevel, String> loggers = new LinkedMultiValueMap<>();
    loggers.add(LogLevel.DEBUG, "sql");
    loggers.add(LogLevel.DEBUG, "web");
    loggers.add(LogLevel.DEBUG, "cn.taketoday.framework");
    loggers.add(LogLevel.TRACE, "cn.taketoday");
    loggers.add(LogLevel.TRACE, "org.apache.tomcat");
    loggers.add(LogLevel.TRACE, "org.apache.catalina");
    loggers.add(LogLevel.TRACE, "org.eclipse.jetty");
    loggers.add(LogLevel.TRACE, "org.hibernate.tool.hbm2ddl");
    INFRA_LOGGING_LOGGERS = Collections.unmodifiableMap(loggers);
  }

  private static final Class<?>[] EVENT_TYPES = {
          ApplicationStartingEvent.class,
          ApplicationEnvironmentPreparedEvent.class,
          ApplicationPreparedEvent.class,
          ContextClosedEvent.class,
          ApplicationFailedEvent.class
  };

  private static final Class<?>[] SOURCE_TYPES = { Application.class, ApplicationContext.class };

  private static final AtomicBoolean shutdownHookRegistered = new AtomicBoolean();

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private LoggingSystem loggingSystem;

  private LogFile logFile;

  @Nullable
  private LoggerGroups loggerGroups;

  private int order = DEFAULT_ORDER;

  private boolean parseArgs = true;

  @Nullable
  private LogLevel infraLogging = null;

  @Override
  public boolean supportsEventType(ResolvableType resolvableType) {
    return isAssignableFrom(resolvableType.getRawClass(), EVENT_TYPES);
  }

  @Override
  public boolean supportsSourceType(Class<?> sourceType) {
    return isAssignableFrom(sourceType, SOURCE_TYPES);
  }

  private boolean isAssignableFrom(Class<?> type, Class<?>... supportedTypes) {
    if (type != null) {
      for (Class<?> supportedType : supportedTypes) {
        if (supportedType.isAssignableFrom(type)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof ApplicationStartingEvent startingEvent) {
      onApplicationStartingEvent(startingEvent);
    }
    else if (event instanceof ApplicationEnvironmentPreparedEvent environmentPreparedEvent) {
      onApplicationEnvironmentPreparedEvent(environmentPreparedEvent);
    }
    else if (event instanceof ApplicationPreparedEvent preparedEvent) {
      onApplicationPreparedEvent(preparedEvent);
    }
    else if (event instanceof ContextClosedEvent) {
      onContextClosedEvent((ContextClosedEvent) event);
    }
    else if (event instanceof ApplicationFailedEvent) {
      onApplicationFailedEvent();
    }
  }

  private void onApplicationStartingEvent(ApplicationStartingEvent event) {
    this.loggingSystem = LoggingSystem.get(event.getApplication().getClassLoader());
    this.loggingSystem.beforeInitialize();
  }

  private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
    Application application = event.getApplication();
    if (this.loggingSystem == null) {
      this.loggingSystem = LoggingSystem.get(application.getClassLoader());
    }
    initialize(event.getEnvironment(), application.getClassLoader());
  }

  private void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
    ConfigurableApplicationContext applicationContext = event.getApplicationContext();
    ConfigurableBeanFactory beanFactory = applicationContext.getBeanFactory();
    if (!beanFactory.containsBean(LOGGING_SYSTEM_BEAN_NAME)) {
      beanFactory.registerSingleton(LOGGING_SYSTEM_BEAN_NAME, this.loggingSystem);
    }
    if (this.logFile != null && !beanFactory.containsBean(LOG_FILE_BEAN_NAME)) {
      beanFactory.registerSingleton(LOG_FILE_BEAN_NAME, this.logFile);
    }
    if (this.loggerGroups != null && !beanFactory.containsBean(LOGGER_GROUPS_BEAN_NAME)) {
      beanFactory.registerSingleton(LOGGER_GROUPS_BEAN_NAME, this.loggerGroups);
    }
    if (!beanFactory.containsBean(LOGGING_LIFECYCLE_BEAN_NAME) && applicationContext.getParent() == null) {
      beanFactory.registerSingleton(LOGGING_LIFECYCLE_BEAN_NAME, new Lifecycle());
    }
  }

  private void onContextClosedEvent(ContextClosedEvent event) {
    ApplicationContext applicationContext = event.getApplicationContext();
    if (applicationContext.getParent() != null || applicationContext.containsBean(LOGGING_LIFECYCLE_BEAN_NAME)) {
      return;
    }
    cleanupLoggingSystem();
  }

  void cleanupLoggingSystem() {
    if (this.loggingSystem != null) {
      this.loggingSystem.cleanUp();
    }
  }

  private void onApplicationFailedEvent() {
    cleanupLoggingSystem();
  }

  /**
   * Initialize the logging system according to preferences expressed through the
   * {@link Environment} and the classpath.
   *
   * @param environment the environment
   * @param classLoader the classloader
   */
  protected void initialize(ConfigurableEnvironment environment, ClassLoader classLoader) {
    getLoggingSystemProperties(environment).apply();
    this.logFile = LogFile.get(environment);
    if (this.logFile != null) {
      this.logFile.applyToSystemProperties();
    }
    this.loggerGroups = new LoggerGroups(DEFAULT_GROUP_LOGGERS);
    initializeEarlyLoggingLevel(environment);
    initializeSystem(environment, this.loggingSystem, this.logFile);
    initializeFinalLoggingLevels(environment, this.loggingSystem);
    registerShutdownHookIfNecessary(environment, this.loggingSystem);
  }

  private LoggingSystemProperties getLoggingSystemProperties(ConfigurableEnvironment environment) {
    return loggingSystem != null ?
           loggingSystem.getSystemProperties(environment) :
           new LoggingSystemProperties(environment);
  }

  private void initializeEarlyLoggingLevel(ConfigurableEnvironment environment) {
    if (this.parseArgs && this.infraLogging == null) {
      if (isSet(environment, "debug")) {
        this.infraLogging = LogLevel.DEBUG;
      }
      if (isSet(environment, "trace")) {
        this.infraLogging = LogLevel.TRACE;
      }
    }
  }

  private boolean isSet(ConfigurableEnvironment environment, String property) {
    String value = environment.getProperty(property);
    return (value != null && !value.equals("false"));
  }

  private void initializeSystem(ConfigurableEnvironment environment, LoggingSystem system, LogFile logFile) {
    String logConfig = environment.getProperty(CONFIG_PROPERTY);
    if (StringUtils.isNotEmpty(logConfig)) {
      logConfig = logConfig.strip();
    }
    try {
      LoggingStartupContext startupContext = new LoggingStartupContext(environment);
      if (ignoreLogConfig(logConfig)) {
        system.initialize(startupContext, null, logFile);
      }
      else {
        system.initialize(startupContext, logConfig, logFile);
      }
    }
    catch (Exception ex) {
      Throwable exceptionToReport = ex;
      while (exceptionToReport != null && !(exceptionToReport instanceof FileNotFoundException)) {
        exceptionToReport = exceptionToReport.getCause();
      }
      exceptionToReport = (exceptionToReport != null) ? exceptionToReport : ex;
      // NOTE: We can't use the logger here to report the problem
      System.err.println("Logging system failed to initialize using configuration from '" + logConfig + "'");
      exceptionToReport.printStackTrace(System.err);
      throw new IllegalStateException(ex);
    }
  }

  private boolean ignoreLogConfig(String logConfig) {
    return StringUtils.isEmpty(logConfig) || logConfig.startsWith("-D");
  }

  private void initializeFinalLoggingLevels(ConfigurableEnvironment environment, LoggingSystem system) {
    bindLoggerGroups(environment);
    if (this.infraLogging != null) {
      initializeInfraLogging(system, infraLogging);
    }
    setLogLevels(system, environment);
  }

  private void bindLoggerGroups(ConfigurableEnvironment environment) {
    if (loggerGroups != null) {
      Binder binder = Binder.get(environment);
      binder.bind(LOGGING_GROUP, STRING_STRINGS_MAP)
              .ifBound(loggerGroups::putAll);
    }
  }

  /**
   * Initialize loggers based on the {@link #setInfraLogging(LogLevel)
   * infraLogging} setting. By default this implementation will pick an appropriate
   * set of loggers to configure based on the level.
   *
   * @param system the logging system
   * @param level the infra logging level requested
   */
  protected void initializeInfraLogging(LoggingSystem system, LogLevel level) {
    BiConsumer<String, LogLevel> configurer = getLogLevelConfigurer(system);
    for (String name : INFRA_LOGGING_LOGGERS.getOrDefault(level, Collections.emptyList())) {
      configureLogLevel(name, level, configurer);
    }
  }

  /**
   * Set logging levels based on relevant {@link Environment} properties.
   *
   * @param system the logging system
   * @param environment the environment
   */
  protected void setLogLevels(LoggingSystem system, ConfigurableEnvironment environment) {
    BiConsumer<String, LogLevel> customizer = getLogLevelConfigurer(system);
    Binder binder = Binder.get(environment);
    var levels = binder.bind(LOGGING_LEVEL, STRING_LOGLEVEL_MAP).orElseGet(Collections::emptyMap);
    for (Map.Entry<String, LogLevel> entry : levels.entrySet()) {
      configureLogLevel(entry.getKey(), entry.getValue(), customizer);
    }
  }

  private void configureLogLevel(String name, LogLevel level, BiConsumer<String, LogLevel> configurer) {
    if (loggerGroups != null) {
      LoggerGroup group = loggerGroups.get(name);
      if (group != null && group.hasMembers()) {
        group.configureLogLevel(level, configurer);
        return;
      }
    }
    configurer.accept(name, level);
  }

  private BiConsumer<String, LogLevel> getLogLevelConfigurer(LoggingSystem system) {
    return (name, level) -> {
      try {
        name = name.equalsIgnoreCase(LoggingSystem.ROOT_LOGGER_NAME) ? null : name;
        system.setLogLevel(name, level);
      }
      catch (RuntimeException ex) {
        logger.error(LogMessage.format("Cannot set level '%s' for '%s'", level, name));
      }
    };
  }

  private void registerShutdownHookIfNecessary(Environment environment, LoggingSystem loggingSystem) {
    if (environment.getProperty(REGISTER_SHUTDOWN_HOOK_PROPERTY, Boolean.class, true)) {
      Runnable shutdownHandler = loggingSystem.getShutdownHandler();
      if (shutdownHandler != null && shutdownHookRegistered.compareAndSet(false, true)) {
        registerShutdownHook(shutdownHandler);
      }
    }
  }

  void registerShutdownHook(Runnable shutdownHandler) {
    Application.getShutdownHandlers().add(shutdownHandler);
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  /**
   * Sets a custom logging level to be used for Infra and related libraries.
   *
   * @param level the logging level
   */
  public void setInfraLogging(@Nullable LogLevel level) {
    this.infraLogging = level;
  }

  /**
   * Sets if initialization arguments should be parsed for {@literal debug} and
   * {@literal trace} properties (usually defined from {@literal --debug} or
   * {@literal --trace} command line args). Defaults to {@code true}.
   *
   * @param parseArgs if arguments should be parsed
   */
  public void setParseArgs(boolean parseArgs) {
    this.parseArgs = parseArgs;
  }

  private class Lifecycle implements SmartLifecycle {

    private volatile boolean running;

    @Override
    public void start() {
      this.running = true;
    }

    @Override
    public void stop() {
      this.running = false;
      cleanupLoggingSystem();
    }

    @Override
    public boolean isRunning() {
      return this.running;
    }

    @Override
    public int getPhase() {
      // Shutdown late and always after WebServerStartStopLifecycle
      return Integer.MIN_VALUE + 1;
    }

  }

}
