/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.logging.logback;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.SubstituteLoggerFactory;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.TurboFilterList;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusListenerConfigHelper;
import ch.qos.logback.core.util.StatusPrinter;
import cn.taketoday.aot.AotDetector;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.logging.AbstractLoggingSystem;
import cn.taketoday.framework.logging.LogFile;
import cn.taketoday.framework.logging.LogLevel;
import cn.taketoday.framework.logging.LoggerConfiguration;
import cn.taketoday.framework.logging.LoggingStartupContext;
import cn.taketoday.framework.logging.LoggingSystem;
import cn.taketoday.framework.logging.LoggingSystemFactory;
import cn.taketoday.framework.logging.LoggingSystemProperties;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.SLF4JBridgeHandler;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link LoggingSystem} for <a href="https://logback.qos.ch">logback</a>.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Ben Hale
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class LogbackLoggingSystem extends AbstractLoggingSystem implements BeanFactoryInitializationAotProcessor {

  private static final String CONFIGURATION_FILE_PROPERTY = "logback.configurationFile";

  private static final LogLevels<Level> LEVELS = new LogLevels<>();

  static {
    LEVELS.map(LogLevel.TRACE, Level.TRACE);
    LEVELS.map(LogLevel.TRACE, Level.ALL);
    LEVELS.map(LogLevel.DEBUG, Level.DEBUG);
    LEVELS.map(LogLevel.INFO, Level.INFO);
    LEVELS.map(LogLevel.WARN, Level.WARN);
    LEVELS.map(LogLevel.ERROR, Level.ERROR);
    LEVELS.map(LogLevel.FATAL, Level.ERROR);
    LEVELS.map(LogLevel.OFF, Level.OFF);
  }

  private static final TurboFilter FILTER = new TurboFilter() {

    @Override
    public FilterReply decide(Marker marker, ch.qos.logback.classic.Logger logger, Level level, String format,
            Object[] params, Throwable t) {
      return FilterReply.DENY;
    }

  };

  public LogbackLoggingSystem(ClassLoader classLoader) {
    super(classLoader);
  }

  @Override
  public LoggingSystemProperties getSystemProperties(ConfigurableEnvironment environment) {
    return new LogbackLoggingSystemProperties(environment, getDefaultValueResolver(environment), null);
  }

  @Override
  protected String[] getStandardConfigLocations() {
    return new String[] { "logback-test.groovy", "logback-test.xml", "logback.groovy", "logback.xml" };
  }

  @Override
  public void beforeInitialize() {
    LoggerContext loggerContext = getLoggerContext();
    if (isAlreadyInitialized(loggerContext)) {
      return;
    }
    super.beforeInitialize();
    configureJdkLoggingBridgeHandler();
    loggerContext.getTurboFilterList().add(FILTER);
  }

  private void configureJdkLoggingBridgeHandler() {
    try {
      removeJdkLoggingBridgeHandler();
      SLF4JBridgeHandler.install();
    }
    catch (Throwable ex) {
      // Ignore. No java.util.logging bridge is installed.
    }
  }

  private void removeJdkLoggingBridgeHandler() {
    try {
      removeDefaultRootHandler();
      SLF4JBridgeHandler.uninstall();
    }
    catch (Throwable ex) {
      // Ignore and continue
    }
  }

  private void removeDefaultRootHandler() {
    try {
      java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
      Handler[] handlers = rootLogger.getHandlers();
      if (handlers.length == 1 && handlers[0] instanceof ConsoleHandler) {
        rootLogger.removeHandler(handlers[0]);
      }
    }
    catch (Throwable ex) {
      // Ignore and continue
    }
  }

  @Override
  public void initialize(LoggingStartupContext startupContext,
          @Nullable String configLocation, @Nullable LogFile logFile) {
    LoggerContext loggerContext = getLoggerContext();
    if (isAlreadyInitialized(loggerContext)) {
      return;
    }

    if (!initializeFromAotGeneratedArtifactsIfPossible(startupContext, logFile)) {
      super.initialize(startupContext, configLocation, logFile);
    }

    loggerContext.putObject(Environment.class.getName(), startupContext.getEnvironment());
    loggerContext.getTurboFilterList().remove(FILTER);
    markAsInitialized(loggerContext);
    if (StringUtils.hasText(System.getProperty(CONFIGURATION_FILE_PROPERTY))) {
      getLogger(LogbackLoggingSystem.class.getName())
              .warn("Ignoring '{}' system property. Please use 'logging.config' instead.", CONFIGURATION_FILE_PROPERTY);
    }
  }

  private boolean initializeFromAotGeneratedArtifactsIfPossible(@Nullable LoggingStartupContext startupContext, LogFile logFile) {
    if (!AotDetector.useGeneratedArtifacts()) {
      return false;
    }
    if (startupContext != null) {
      applySystemProperties(startupContext.getEnvironment(), logFile);
    }
    LoggerContext loggerContext = getLoggerContext();
    stopAndReset(loggerContext);
    InfraJoranConfigurator configurator = new InfraJoranConfigurator(startupContext);
    configurator.setContext(loggerContext);
    boolean configuredUsingAotGeneratedArtifacts = configurator.configureUsingAotGeneratedArtifacts();
    if (configuredUsingAotGeneratedArtifacts) {
      reportConfigurationErrorsIfNecessary(loggerContext);
    }
    return configuredUsingAotGeneratedArtifacts;
  }

  @Override
  protected void loadDefaults(LoggingStartupContext startupContext, @Nullable LogFile logFile) {
    LoggerContext context = getLoggerContext();
    stopAndReset(context);
    withLoggingSuppressed(() -> {
      boolean debug = Boolean.getBoolean("logback.debug");
      if (debug) {
        StatusListenerConfigHelper.addOnConsoleListenerInstance(context, new OnConsoleStatusListener());
      }
      Environment environment = startupContext.getEnvironment();
      // Apply system properties directly in case the same JVM runs multiple apps
      new LogbackLoggingSystemProperties(environment, getDefaultValueResolver(environment), context::putProperty)
              .apply(logFile);
      LogbackConfigurator configurator = debug ? new DebugLogbackConfigurator(context)
                                               : new LogbackConfigurator(context);
      new DefaultLogbackConfiguration(logFile).apply(configurator);
      context.setPackagingDataEnabled(true);
    });
  }

  @Override
  protected void loadConfiguration(@Nullable LoggingStartupContext context,
          String location, @Nullable LogFile logFile) {
    LoggerContext loggerContext = getLoggerContext();
    stopAndReset(loggerContext);
    withLoggingSuppressed(() -> {
      if (context != null) {
        applySystemProperties(context.getEnvironment(), logFile);
      }
      try {
        configureByResourceUrl(context, loggerContext, ResourceUtils.getURL(location));
      }
      catch (Exception ex) {
        throw new IllegalStateException("Could not initialize Logback logging from " + location, ex);
      }
    });
    reportConfigurationErrorsIfNecessary(loggerContext);
  }

  private void reportConfigurationErrorsIfNecessary(LoggerContext loggerContext) {
    StringBuilder errors = new StringBuilder();
    List<Throwable> suppressedExceptions = new ArrayList<>();
    for (Status status : loggerContext.getStatusManager().getCopyOfStatusList()) {
      if (status.getLevel() == Status.ERROR) {
        errors.append((!errors.isEmpty()) ? String.format("%n") : "");
        errors.append(status);
        if (status.getThrowable() != null) {
          suppressedExceptions.add(status.getThrowable());
        }
      }
    }

    if (errors.isEmpty()) {
      if (!StatusUtil.contextHasStatusListener(loggerContext)) {
        StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
      }
      return;
    }
    IllegalStateException ex = new IllegalStateException(
            String.format("Logback configuration error detected: %n%s", errors));
    for (Throwable suppressedException : suppressedExceptions) {
      ex.addSuppressed(suppressedException);
    }
    throw ex;
  }

  private void configureByResourceUrl(LoggingStartupContext startupContext,
          LoggerContext loggerContext, URL url) throws JoranException {
    if (url.getPath().endsWith(".xml")) {
      JoranConfigurator configurator = new InfraJoranConfigurator(startupContext);
      configurator.setContext(loggerContext);
      configurator.doConfigure(url);
    }
    else {
      throw new IllegalArgumentException(
              "Unsupported file extension in '" + url + "'. Only .xml is supported");
    }
  }

  private void stopAndReset(LoggerContext loggerContext) {
    loggerContext.stop();
    loggerContext.reset();
    if (isBridgeHandlerInstalled()) {
      addLevelChangePropagator(loggerContext);
    }
  }

  private boolean isBridgeHandlerInstalled() {
    var rootLogger = LogManager.getLogManager().getLogger("");
    Handler[] handlers = rootLogger.getHandlers();
    return handlers.length == 1 && handlers[0] instanceof SLF4JBridgeHandler;
  }

  private void addLevelChangePropagator(LoggerContext loggerContext) {
    LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
    levelChangePropagator.setResetJUL(true);
    levelChangePropagator.setContext(loggerContext);
    loggerContext.addListener(levelChangePropagator);
  }

  @Override
  public void cleanUp() {
    LoggerContext context = getLoggerContext();
    markAsUninitialized(context);
    super.cleanUp();
    removeJdkLoggingBridgeHandler();
    context.getStatusManager().clear();
    context.getTurboFilterList().remove(FILTER);
  }

  @Override
  protected void reinitialize(LoggingStartupContext startupContext) {
    LoggerContext loggerContext = getLoggerContext();
    loggerContext.reset();
    loggerContext.getStatusManager().clear();
    loadConfiguration(startupContext, getSelfInitializationConfig(), null);
  }

  @Override
  public List<LoggerConfiguration> getLoggerConfigurations() {
    List<LoggerConfiguration> result = new ArrayList<>();
    for (ch.qos.logback.classic.Logger logger : getLoggerContext().getLoggerList()) {
      result.add(getLoggerConfiguration(logger));
    }
    result.sort(CONFIGURATION_COMPARATOR);
    return result;
  }

  @Override
  public LoggerConfiguration getLoggerConfiguration(String loggerName) {
    String name = getLoggerName(loggerName);
    LoggerContext loggerContext = getLoggerContext();
    return getLoggerConfiguration(loggerContext.exists(name));
  }

  private String getLoggerName(String name) {
    if (StringUtils.isEmpty(name) || Logger.ROOT_LOGGER_NAME.equals(name)) {
      return ROOT_LOGGER_NAME;
    }
    return name;
  }

  @Nullable
  private LoggerConfiguration getLoggerConfiguration(@Nullable ch.qos.logback.classic.Logger logger) {
    if (logger == null) {
      return null;
    }
    LogLevel level = LEVELS.convertNativeToSystem(logger.getLevel());
    LogLevel effectiveLevel = LEVELS.convertNativeToSystem(logger.getEffectiveLevel());
    String name = getLoggerName(logger.getName());
    return new LoggerConfiguration(name, level, effectiveLevel);
  }

  @Override
  public Set<LogLevel> getSupportedLogLevels() {
    return LEVELS.getSupported();
  }

  @Override
  public void setLogLevel(String loggerName, LogLevel level) {
    var logger = getLogger(loggerName);
    if (logger != null) {
      logger.setLevel(LEVELS.convertSystemToNative(level));
    }
  }

  @Override
  public Runnable getShutdownHandler() {
    return () -> getLoggerContext().stop();
  }

  private ch.qos.logback.classic.Logger getLogger(String name) {
    LoggerContext factory = getLoggerContext();
    return factory.getLogger(getLoggerName(name));
  }

  private LoggerContext getLoggerContext() {
    ILoggerFactory factory = getLoggerFactory();
    if (factory instanceof LoggerContext context) {
      return context;
    }
    throw new IllegalArgumentException(
            String.format("LoggerFactory is not a Logback LoggerContext but Logback is on "
                            + "the classpath. Either remove Logback or the competing "
                            + "implementation (%s loaded from %s). If you are using "
                            + "WebLogic you will need to add 'org.slf4j' to "
                            + "prefer-application-packages in WEB-INF/weblogic.xml",
                    factory.getClass(), getLocation(factory)));
  }

  private ILoggerFactory getLoggerFactory() {
    ILoggerFactory factory = LoggerFactory.getILoggerFactory();
    while (factory instanceof SubstituteLoggerFactory) {
      try {
        Thread.sleep(50);
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while waiting for non-subtitute logger factory", ex);
      }
      factory = LoggerFactory.getILoggerFactory();
    }
    return factory;
  }

  private Object getLocation(ILoggerFactory factory) {
    try {
      ProtectionDomain protectionDomain = factory.getClass().getProtectionDomain();
      CodeSource codeSource = protectionDomain.getCodeSource();
      if (codeSource != null) {
        return codeSource.getLocation();
      }
    }
    catch (SecurityException ex) {
      // Unable to determine location
    }
    return "unknown location";
  }

  private boolean isAlreadyInitialized(LoggerContext loggerContext) {
    return loggerContext.getObject(LoggingSystem.class.getName()) != null;
  }

  private void markAsInitialized(LoggerContext loggerContext) {
    loggerContext.putObject(LoggingSystem.class.getName(), new Object());
  }

  private void markAsUninitialized(LoggerContext loggerContext) {
    loggerContext.removeObject(LoggingSystem.class.getName());
  }

  @Override
  protected String getDefaultLogCorrelationPattern() {
    return "%correlationId";
  }

  @Override
  public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
    String key = BeanFactoryInitializationAotContribution.class.getName();
    LoggerContext context = getLoggerContext();
    BeanFactoryInitializationAotContribution contribution = (BeanFactoryInitializationAotContribution) context
            .getObject(key);
    context.removeObject(key);
    return contribution;
  }

  private void withLoggingSuppressed(Runnable action) {
    TurboFilterList turboFilters = getLoggerContext().getTurboFilterList();
    turboFilters.add(FILTER);
    try {
      action.run();
    }
    finally {
      turboFilters.remove(FILTER);
    }
  }

  /**
   * {@link LoggingSystemFactory} that returns {@link LogbackLoggingSystem} if possible.
   */
  @Order(Ordered.LOWEST_PRECEDENCE)
  public static class Factory implements LoggingSystemFactory {

    private static final boolean PRESENT = ClassUtils.isPresent(
            "ch.qos.logback.classic.LoggerContext", Factory.class.getClassLoader());

    @Override
    public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
      if (PRESENT) {
        return new LogbackLoggingSystem(classLoader);
      }
      return null;
    }

  }

}
