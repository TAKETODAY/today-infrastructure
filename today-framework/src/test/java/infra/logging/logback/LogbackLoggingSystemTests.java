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

package infra.logging.logback;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogManager;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.DynamicClassLoadingException;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.MapPropertySource;
import infra.format.support.ApplicationConversionService;
import infra.logging.AbstractLoggingSystemTests;
import infra.logging.LogFile;
import infra.logging.LogLevel;
import infra.logging.LoggerConfiguration;
import infra.logging.LoggingStartupContext;
import infra.logging.LoggingSystem;
import infra.logging.LoggingSystemProperties;
import infra.logging.LoggingSystemProperty;
import infra.logging.SLF4JBridgeHandler;
import infra.mock.env.MockEnvironment;
import infra.test.classpath.ClassPathOverrides;
import infra.test.util.ReflectionTestUtils;
import infra.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link LogbackLoggingSystem}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Ben Hale
 * @author Madhura Bhave
 * @author Vedran Pavic
 * @author Robert Thornton
 * @author Eddú Meléndez
 * @author Scott Frederick
 */
@ExtendWith(OutputCaptureExtension.class)
class LogbackLoggingSystemTests extends AbstractLoggingSystemTests {

  private final LogbackLoggingSystem loggingSystem = new LogbackLoggingSystem(getClass().getClassLoader());

  private Logger logger;

  private MockEnvironment environment;

  private LoggingStartupContext initializationContext;

  private Set<Object> systemPropertyNames;

  @BeforeEach
  void setup() {
    for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
      System.getProperties().remove(property.getEnvironmentVariableName());
    }
    this.systemPropertyNames = new HashSet<>(System.getProperties().keySet());
    this.loggingSystem.cleanUp();
    this.logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(getClass());
    this.environment = new MockEnvironment();
    var conversionService = ApplicationConversionService.getSharedInstance();
    this.environment.setConversionService(conversionService);
    this.initializationContext = new LoggingStartupContext(this.environment);
    this.loggingSystem.statusPrinter.setPrintStream(System.out);
  }

  @AfterEach
  void cleanUp() {
    System.getProperties().keySet().retainAll(this.systemPropertyNames);
    this.loggingSystem.cleanUp();
    ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
  }

  @Test
  void logbackDefaultsConfigurationDoesNotTriggerDeprecation(CapturedOutput output) {
    initialize(this.initializationContext, "classpath:logback-include-defaults.xml", null);
    this.logger.info("Hello world");
    assertThat(getLineWithText(output, "Hello world")).isEqualTo("[INFO] - Hello world");
    assertThat(output.toString()).doesNotContain("WARN").doesNotContain("deprecated");
  }

  @Test
  void logbackBaseConfigurationDoesNotTriggerDeprecation(CapturedOutput output) {
    initialize(this.initializationContext, "classpath:logback-include-base.xml", null);
    this.logger.info("Hello world");
    assertThat(getLineWithText(output, "Hello world")).contains(" INFO ").endsWith(": Hello world");
    assertThat(output.toString()).doesNotContain("WARN").doesNotContain("deprecated");
  }

  @Test
  @ClassPathOverrides({ "org.jboss.logging:jboss-logging:3.5.0.Final", "org.apache.logging.log4j:log4j-core:2.19.0" })
  void jbossLoggingRoutesThroughLog4j2ByDefault() {
    System.getProperties().remove("org.jboss.logging.provider");
    org.jboss.logging.Logger jbossLogger = org.jboss.logging.Logger.getLogger(getClass());
    assertThat(jbossLogger.getClass().getName()).isEqualTo("org.jboss.logging.Log4j2Logger");
  }

  @Test
  @ClassPathOverrides("org.jboss.logging:jboss-logging:3.5.0.Final")
  void jbossLoggingRoutesThroughSlf4jWhenLoggingSystemIsInitialized() {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    assertThat(org.jboss.logging.Logger.getLogger(getClass()).getClass().getName())
            .isEqualTo("org.jboss.logging.Slf4jLocationAwareLogger");
  }

  @Test
  void noFile(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    this.logger.info("Hidden");
    initialize(this.initializationContext, null, null);
    this.logger.info("Hello world");
    assertThat(output).contains("Hello world").doesNotContain("Hidden");
    Assertions.assertThat(getLineWithText(output, "Hello world")).contains("INFO");
    assertThat(new File(tmpDir() + "/infra-app.log")).doesNotExist();
  }

  @Test
  void withFile(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    this.logger.info("Hidden");
    initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
    this.logger.info("Hello world");
    File file = new File(tmpDir() + "/infra-app.log");
    assertThat(output).doesNotContain("LOGBACK:");
    assertThat(output).contains("Hello world").doesNotContain("Hidden");
    Assertions.assertThat(getLineWithText(output, "Hello world")).contains("INFO");
    assertThat(file).exists();
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("INFO");
    assertThat(maxFileSize()).hasToString("10 MB");
    assertThat(getRollingPolicy().getMaxHistory()).isEqualTo(7);
  }

  private static Object maxFileSize() {
    return ReflectionTestUtils.getField(getRollingPolicy(), "maxFileSize");
  }

  @Test
  void defaultConfigConfiguresAConsoleAppender() {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    assertThat(getConsoleAppender()).isNotNull();
  }

  @Test
  void testNonDefaultConfigLocation(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, "classpath:logback-nondefault.xml",
            getLogFile(tmpDir() + "/tmp.log", null));
    this.logger.info("Hello world");
    assertThat(output).doesNotContain("DEBUG")
            .contains("Hello world")
            .contains(tmpDir() + "/tmp.log")
            .endsWith("BOOTBOOT");
    assertThat(new File(tmpDir() + "/tmp.log")).doesNotExist();
  }

  @Test
  void testLogbackSpecificSystemProperty(CapturedOutput output) {
    System.setProperty("logback.configurationFile", "/foo/my-file.xml");
    try {
      this.loggingSystem.beforeInitialize();
      initialize(this.initializationContext, null, null);
      assertThat(output)
              .contains("Ignoring 'logback.configurationFile' system property. Please use 'logging.config' instead.");
    }
    finally {
      System.clearProperty("logback.configurationFile");
    }
  }

  @Test
  void testNonexistentConfigLocation() {
    this.loggingSystem.beforeInitialize();
    assertThatIllegalStateException()
            .isThrownBy(() -> initialize(this.initializationContext, "classpath:logback-nonexistent.xml", null));
  }

  @Test
  void getSupportedLevels() {
    assertThat(this.loggingSystem.getSupportedLogLevels()).isEqualTo(
            EnumSet.of(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.OFF));
  }

  @Test
  void setLevel(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    this.logger.debug("Hello");
    this.loggingSystem.setLogLevel("infra", LogLevel.DEBUG);
    this.logger.debug("Hello");
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "Hello")).isOne();
  }

  @Test
  void setLevelToNull(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    this.logger.debug("Hello");
    this.loggingSystem.setLogLevel("infra", LogLevel.DEBUG);
    this.logger.debug("Hello");
    this.loggingSystem.setLogLevel("infra", null);
    this.logger.debug("Hello");
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "Hello")).isOne();
  }

  @Test
  void getLoggerConfigurations() {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
    List<LoggerConfiguration> configurations = this.loggingSystem.getLoggerConfigurations();
    assertThat(configurations).isNotEmpty();
    assertThat(configurations.get(0).getName()).isEqualTo(LoggingSystem.ROOT_LOGGER_NAME);
  }

  @Test
  void getLoggerConfiguration() {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
    LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration(getClass().getName());
    assertThat(configuration)
            .isEqualTo(new LoggerConfiguration(getClass().getName(), LogLevel.DEBUG, LogLevel.DEBUG));
  }

  @Test
  void getLoggerConfigurationForLoggerThatDoesNotExistShouldReturnNull() {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration("doesnotexist");
    assertThat(configuration).isNull();
  }

  @Test
  void getLoggerConfigurationForALL() {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    Logger logger = (Logger) LoggerFactory.getILoggerFactory().getLogger(getClass().getName());
    logger.setLevel(Level.ALL);
    LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration(getClass().getName());
    assertThat(configuration)
            .isEqualTo(new LoggerConfiguration(getClass().getName(), LogLevel.TRACE, LogLevel.TRACE));
  }

  @Test
  void systemLevelTraceShouldReturnNativeLevelTraceNotAll() {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.TRACE);
    Logger logger = (Logger) LoggerFactory.getILoggerFactory().getLogger(getClass().getName());
    assertThat(logger.getLevel()).isEqualTo(Level.TRACE);
  }

  @Test
  void loggingThatUsesJulIsCaptured(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(getClass().getName());
    julLogger.info("Hello world");
    assertThat(output).contains("Hello world");
  }

  @Test
  void loggingLevelIsPropagatedToJul(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
    java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(getClass().getName());
    julLogger.fine("Hello debug world");
    assertThat(output).contains("Hello debug world");
  }

  @Test
  void bridgeHandlerLifecycle() {
    assertThat(bridgeHandlerInstalled()).isFalse();
    this.loggingSystem.beforeInitialize();
    assertThat(bridgeHandlerInstalled()).isTrue();
    this.loggingSystem.cleanUp();
    assertThat(bridgeHandlerInstalled()).isFalse();
  }

  @Test
  void standardConfigLocations() {
    String[] locations = this.loggingSystem.getStandardConfigLocations();
    assertThat(locations).containsExactly("logback-test.groovy", "logback-test.xml", "logback.groovy",
            "logback.xml");
  }

  @Test
  void configLocations() {
    String[] locations = getConfigLocations(this.loggingSystem);
    assertThat(locations).containsExactly("logback-test-infra.groovy", "logback-test-infra.xml",
            "logback-infra.groovy", "logback-infra.xml");
  }

  private boolean bridgeHandlerInstalled() {
    java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
    Handler[] handlers = rootLogger.getHandlers();
    for (Handler handler : handlers) {
      if (handler instanceof SLF4JBridgeHandler) {
        return true;
      }
    }
    return false;
  }

  @Test
  void testConsolePatternProperty(CapturedOutput output) {
    this.environment.setProperty("logging.pattern.console", "%logger %msg");
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    initialize(LoggingStartupContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).doesNotContain("INFO");
  }

  @Test
  void testLevelPatternProperty(CapturedOutput output) {
    this.environment.setProperty("logging.pattern.level", "X%clr(%p)X");
    new LoggingSystemProperties(this.environment).apply();
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    initialize(LoggingStartupContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).contains("XINFOX");
  }

  @Test
  void testFilePatternProperty(CapturedOutput output) {
    this.environment.setProperty("logging.pattern.file", "%logger %msg");
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(LoggingStartupContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).contains("INFO");
    Assertions.assertThat(getLineWithText(file, "Hello world")).doesNotContain("INFO");
  }

  @Test
  void testCleanHistoryOnStartProperty() {
    this.environment.setProperty("logging.file.clean-history-on-start", "true");
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(LoggingStartupContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("INFO");
    assertThat(getRollingPolicy().isCleanHistoryOnStart()).isTrue();
  }

  @Test
  void testCleanHistoryOnStartPropertyWithXmlConfiguration() {
    this.environment.setProperty("logging.file.clean-history-on-start", "true");
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(LoggingStartupContext, "classpath:logback-include-base.xml", logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("INFO");
    assertThat(getRollingPolicy().isCleanHistoryOnStart()).isTrue();
  }

  @Test
  void testMaxFileSizePropertyWithLogbackFileSize() {
    testMaxFileSizeProperty("100 MB", "100 MB");
  }

  @Test
  void testMaxFileSizePropertyWithDataSize() {
    testMaxFileSizeProperty("15MB", "15 MB");
  }

  @Test
  void testMaxFileSizePropertyWithBytesValue() {
    testMaxFileSizeProperty(String.valueOf(10 * 1024 * 1024), "10 MB");
  }

  private void testMaxFileSizeProperty(String sizeValue, String expectedFileSize) {
    this.environment.setProperty("logging.file.max-size", sizeValue);
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(LoggingStartupContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("INFO");
    assertThat(maxFileSize()).hasToString(expectedFileSize);
  }

  @Test
  void testMaxFileSizePropertyWithXmlConfiguration() {
    this.environment.setProperty("logging.file.max-size", "100MB");
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(LoggingStartupContext, "classpath:logback-include-base.xml", logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("INFO");
    assertThat(maxFileSize()).hasToString("100 MB");
  }

  @Test
  void testMaxHistoryProperty() {
    this.environment.setProperty("logging.file.max-history", "30");
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(LoggingStartupContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("INFO");
    assertThat(getRollingPolicy().getMaxHistory()).isEqualTo(30);
  }

  @Test
  void testMaxHistoryPropertyWithXmlConfiguration() {
    this.environment.setProperty("logging.file.max-history", "30");
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(LoggingStartupContext, "classpath:logback-include-base.xml", logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("INFO");
    assertThat(getRollingPolicy().getMaxHistory()).isEqualTo(30);
  }

  @Test
  void testTotalSizeCapPropertyWithLogbackFileSize() {
    testTotalSizeCapProperty("101 MB", "101 MB");
  }

  @Test
  void testTotalSizeCapPropertyWithDataSize() {
    testTotalSizeCapProperty("10MB", "10 MB");
  }

  @Test
  void testTotalSizeCapPropertyWithBytesValue() {
    testTotalSizeCapProperty(String.valueOf(10 * 1024 * 1024), "10 MB");
  }

  private void testTotalSizeCapProperty(String sizeValue, String expectedFileSize) {
    this.environment.setProperty("logging.file.total-size-cap", sizeValue);
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(LoggingStartupContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("INFO");
    assertThat(getTotalSizeCap()).hasToString(expectedFileSize);
  }

  @Test
  void testTotalSizeCapPropertyWithXmlConfiguration() {
    String expectedSize = "101 MB";
    this.environment.setProperty("logging.file.total-size-cap", expectedSize);
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(LoggingStartupContext, "classpath:logback-include-base.xml", logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("INFO");
    assertThat(getTotalSizeCap()).hasToString(expectedSize);
  }

  private static Object getTotalSizeCap() {
    return ReflectionTestUtils.getField(getRollingPolicy(), "totalSizeCap");
  }

  @Test
  void exceptionsIncludeClassPackaging(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
    this.logger.warn("Expected exception", new RuntimeException("Expected"));
    String fileContents = contentOf(new File(tmpDir() + "/infra-app.log"));
    assertThat(fileContents).contains("[junit-");
    assertThat(output).contains("[junit-");
  }

  @Test
  void customExceptionConversionWord(CapturedOutput output) {
    System.setProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD.getEnvironmentVariableName(), "%ex");
    try {
      this.loggingSystem.beforeInitialize();
      this.logger.info("Hidden");
      initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
      this.logger.warn("Expected exception", new RuntimeException("Expected", new RuntimeException("Cause")));
      String fileContents = contentOf(new File(tmpDir() + "/infra-app.log"));
      assertThat(fileContents).contains("java.lang.RuntimeException: Expected").doesNotContain("Wrapped by:");
      assertThat(output).contains("java.lang.RuntimeException: Expected").doesNotContain("Wrapped by:");
    }
    finally {
      System.clearProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD.getEnvironmentVariableName());
    }
  }

  @Test
  void initializeShouldSetSystemProperty() {
    // gh-5491
    this.loggingSystem.beforeInitialize();
    this.logger.info("Hidden");
    LogFile logFile = getLogFile(tmpDir() + "/example.log", null, false);
    initialize(this.initializationContext, "classpath:logback-nondefault.xml", logFile);
    assertThat(System.getProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName()))
            .endsWith("example.log");
  }

  @Test
  void initializeShouldApplyLogbackSystemPropertiesToTheContext() {
    this.environment.setProperty("logging.logback.rollingpolicy.file-name-pattern", "file-name-pattern");
    this.environment.setProperty("logging.logback.rollingpolicy.clean-history-on-start", "true");
    this.environment.setProperty("logging.logback.rollingpolicy.max-file-size", "10MB");
    this.environment.setProperty("logging.logback.rollingpolicy.total-size-cap", "100MB");
    this.environment.setProperty("logging.logback.rollingpolicy.max-history", "20");
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Map<String, String> properties = loggerContext.getCopyOfPropertyMap();
    assertThat(properties).containsEntry("CONSOLE_LOG_CHARSET", Charset.defaultCharset().name());
  }

  private boolean isPublicStaticFinal(Field field) {
    int modifiers = field.getModifiers();
    return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
  }

  @Test
  void initializationIsOnlyPerformedOnceUntilCleanedUp() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    LoggerContextListener listener = mock(LoggerContextListener.class);
    loggerContext.addListener(listener);
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    then(listener).should().onReset(loggerContext);
    this.loggingSystem.cleanUp();
    loggerContext.addListener(listener);
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    then(listener).should(times(2)).onReset(loggerContext);
  }

  @Test
  void testDateformatPatternDefault(CapturedOutput output) {
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    initialize(LoggingStartupContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world"))
            .containsPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}([-+]\\d{2}:\\d{2}|Z)");
  }

  @Test
  void testDateformatPatternProperty(CapturedOutput output) {
    this.environment.setProperty("logging.pattern.dateformat", "dd-MM-yyyy");
    new LoggingSystemProperties(this.environment).apply();
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    initialize(LoggingStartupContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).containsPattern("\\d{2}-\\d{2}-\\d{4}\\s");
  }

  @Test
    // gh-24835
  void testDateformatPatternPropertyDirect(CapturedOutput output) {
    this.environment.setProperty("logging.pattern.dateformat", "yyyy");
    new LoggingSystemProperties(this.environment).apply();
    this.environment.setProperty("logging.pattern.dateformat", "dd-MM-yyyy");
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    initialize(LoggingStartupContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).containsPattern("\\d{2}-\\d{2}-\\d{4}\\s");
  }

  @Test
  void noDebugOutputIsProducedByDefault(CapturedOutput output) {
    System.clearProperty("logback.debug");
    this.loggingSystem.beforeInitialize();
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(this.initializationContext, null, logFile);
    assertThat(output).doesNotContain("LevelChangePropagator").doesNotContain("SizeAndTimeBasedFNATP");
  }

  @Test
  void logbackDebugPropertyIsHonored(CapturedOutput output) {
    System.setProperty("logback.debug", "true");
    try {
      this.loggingSystem.beforeInitialize();
      File file = new File(tmpDir(), "logback-test.log");
      LogFile logFile = getLogFile(file.getPath(), null);
      initialize(this.initializationContext, null, logFile);
      assertThat(output).contains("LevelChangePropagator")
              .contains("SizeAndTimeBasedFNATP")
              .contains("DebugLogbackConfigurator");
    }
    finally {
      System.clearProperty("logback.debug");
    }
  }

  @Test
  void testRollingFileNameProperty() {
    String rollingFile = "my.log.%d{yyyyMMdd}.%i.gz";
    this.environment.setProperty("logging.pattern.rolling-file-name", rollingFile);
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    File file = new File(tmpDir(), "my.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(LoggingStartupContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("INFO");
    assertThat(getRollingPolicy().getFileNamePattern()).isEqualTo(rollingFile);
  }

  @Test
  void customCharset() {
    this.environment.setProperty("logging.charset.console", "UTF-16");
    LoggingStartupContext LoggingStartupContext = new LoggingStartupContext(this.environment);
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(LoggingStartupContext, null, logFile);
    this.logger.info("Hello world");
    LayoutWrappingEncoder<?> encoder = (LayoutWrappingEncoder<?>) getConsoleAppender().getEncoder();
    assertThat(encoder.getCharset()).isEqualTo(StandardCharsets.UTF_16);
  }

  @Test
  void whenContextHasNoAotContributionThenProcessAheadOfTimeReturnsNull() {
    BeanFactoryInitializationAotContribution contribution = this.loggingSystem.processAheadOfTime(null);
    assertThat(contribution).isNull();
  }

  @Test
  void whenContextHasAotContributionThenProcessAheadOfTimeClearsAndReturnsIt() {
    LoggerContext context = ((LoggerContext) LoggerFactory.getILoggerFactory());
    context.putObject(BeanFactoryInitializationAotContribution.class.getName(),
            mock(BeanFactoryInitializationAotContribution.class));
    BeanFactoryInitializationAotContribution contribution = this.loggingSystem.processAheadOfTime(null);
    assertThat(context.getObject(BeanFactoryInitializationAotContribution.class.getName())).isNull();
    assertThat(contribution).isNotNull();
  }

  @Test
  void infraProfileIfNestedWithinSecondPhaseElementSanityChecker(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, "classpath:logback-infraprofile-in-root.xml", null);
    this.logger.info("Hello world");
    assertThat(output).contains("<infra-profile> elements cannot be nested within an");
  }

  @Test
  void correlationLoggingToFileWhenExpectCorrelationIdTrueAndMdcContent() {
    this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(this.initializationContext, null, logFile);
    MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world"))
            .contains(" [01234567890123456789012345678901-0123456789012345] ");
  }

  @Test
  void correlationLoggingToConsoleWhenExpectCorrelationIdTrueAndMdcContent(CapturedOutput output) {
    this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
    initialize(this.initializationContext, null, null);
    MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world"))
            .contains(" [01234567890123456789012345678901-0123456789012345] ");
  }

  @Test
  void correlationLoggingToConsoleWhenExpectCorrelationIdFalseAndMdcContent(CapturedOutput output) {
    this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "false");
    initialize(this.initializationContext, null, null);
    MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).doesNotContain("0123456789012345");
  }

  @Test
  void correlationLoggingToConsoleWhenExpectCorrelationIdTrueAndNoMdcContent(CapturedOutput output) {
    this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
    initialize(this.initializationContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world"))
            .contains(" [                                                 ] ");
  }

  @Test
  void correlationLoggingToConsoleWhenHasCorrelationPattern(CapturedOutput output) {
    this.environment.setProperty("logging.pattern.correlation", "%correlationId{spanId(0),traceId(0)}");
    initialize(this.initializationContext, null, null);
    MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world"))
            .contains(" [0123456789012345-01234567890123456789012345678901] ");
  }

  @Test
  void correlationLoggingToConsoleWhenUsingXmlConfiguration(CapturedOutput output) {
    this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
    initialize(this.initializationContext, "classpath:logback-include-base.xml", null);
    MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world"))
            .contains(" [01234567890123456789012345678901-0123456789012345] ");
  }

  @Test
  void correlationLoggingToFileWhenUsingFileConfiguration() {
    this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(this.initializationContext, "classpath:logback-include-base.xml", logFile);
    MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world"))
            .contains(" [01234567890123456789012345678901-0123456789012345] ");
  }

  @Test
  void applicationNameLoggingToConsoleWhenHasApplicationName(CapturedOutput output) {
    this.environment.setProperty("app.name", "myapp");
    initialize(this.initializationContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).contains("[myapp] ");
  }

  @Test
  void applicationNameLoggingToConsoleWhenHasApplicationNameWithParenthesis(CapturedOutput output) {
    this.environment.setProperty("app.name", "myapp (dev)");
    initialize(this.initializationContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).contains("[myapp (dev)] ");
  }

  @Test
  void applicationNameLoggingToConsoleWhenDisabled(CapturedOutput output) {
    this.environment.setProperty("app.name", "myapp");
    this.environment.setProperty("logging.include-application-name", "false");
    initialize(this.initializationContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).doesNotContain("myapp").doesNotContain("null");
  }

  @Test
  void applicationNameLoggingToFileWhenHasApplicationName() {
    this.environment.setProperty("app.name", "myapp");
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(this.initializationContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("[myapp] ");
  }

  @Test
  void applicationNameLoggingToFileWhenHasApplicationNameWithParenthesis() {
    this.environment.setProperty("app.name", "myapp (dev)");
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(this.initializationContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("[myapp (dev)] ");
  }

  @Test
  void applicationNameLoggingToFileWhenDisabled(CapturedOutput output) {
    this.environment.setProperty("app.name", "myapp");
    this.environment.setProperty("logging.include-application-name", "false");
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(this.initializationContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).doesNotContain("myapp").doesNotContain("null");
  }

  @Test
  void whenConfigurationErrorIsDetectedUnderlyingCausesAreIncludedAsSuppressedExceptions() {
    this.loggingSystem.beforeInitialize();
    assertThatIllegalStateException()
            .isThrownBy(() -> initialize(this.initializationContext, "classpath:logback-broken.xml",
                    getLogFile(tmpDir() + "/tmp.log", null)))
            .satisfies((ex) -> assertThat(ex.getSuppressed())
                    .hasAtLeastOneElementOfType(DynamicClassLoadingException.class));
  }

  @Test
  void whenConfigLocationIsNotXmlThenIllegalArgumentExceptionShouldBeThrown() {
    this.loggingSystem.beforeInitialize();
    assertThatIllegalStateException()
            .isThrownBy(() -> initialize(this.initializationContext, "classpath:logback-invalid-format.txt",
                    getLogFile(tmpDir() + "/tmp.log", null)))
            .satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageStartingWith("Unsupported file extension"));
  }

  @Test
  void whenConfigLocationIsXmlAndHasQueryParametersThenIllegalArgumentExceptionShouldNotBeThrown() {
    this.loggingSystem.beforeInitialize();
    assertThatIllegalStateException()
            .isThrownBy(() -> initialize(this.initializationContext, "file:///logback-nonexistent.xml?raw=true",
                    getLogFile(tmpDir() + "/tmp.log", null)))
            .satisfies((ex) -> assertThat(ex.getCause()).isNotInstanceOf(IllegalArgumentException.class));
  }

  @Test
  void shouldRespectConsoleThreshold(CapturedOutput output) {
    this.environment.setProperty("logging.threshold.console", "warn");
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    this.logger.info("Some info message");
    this.logger.warn("Some warn message");
    assertThat(output).doesNotContain("Some info message").contains("Some warn message");
  }

  @Test
  void shouldRespectFileThreshold() {
    this.environment.setProperty("logging.threshold.file", "warn");
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
    this.logger.info("Some info message");
    this.logger.warn("Some warn message");
    Path file = Path.of(tmpDir(), "infra-app.log");
    assertThat(file).content(StandardCharsets.UTF_8)
            .doesNotContain("Some info message")
            .contains("Some warn message");
  }

  @Test
  void applyingSystemPropertiesDoesNotCauseUnwantedStatusWarnings(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    this.environment.getPropertySources()
            .addFirst(new MapPropertySource("test", Map.of("logging.pattern.console", "[CONSOLE]%m")));
    this.loggingSystem.initialize(this.initializationContext, "classpath:logback-nondefault.xml", null);
    assertThat(output).doesNotContain("WARN");
  }

  @Test
  void applicationGroupLoggingToConsoleWhenHasApplicationGroup(CapturedOutput output) {
    this.environment.setProperty("app.group", "mygroup");
    initialize(this.initializationContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).contains("[mygroup] ");
  }

  @Test
  void applicationGroupLoggingToConsoleWhenHasApplicationGroupWithParenthesis(CapturedOutput output) {
    this.environment.setProperty("app.group", "mygroup (dev)");
    initialize(this.initializationContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).contains("[mygroup (dev)] ");
  }

  @Test
  void applicationGroupLoggingToConsoleWhenDisabled(CapturedOutput output) {
    this.environment.setProperty("app.group", "mygroup");
    this.environment.setProperty("logging.include-application-group", "false");
    initialize(this.initializationContext, null, null);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(output, "Hello world")).doesNotContain("mygroup").doesNotContain("null");
  }

  @Test
  void applicationGroupLoggingToFileWhenHasApplicationGroup() {
    this.environment.setProperty("app.group", "mygroup");
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(this.initializationContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("[mygroup] ");
  }

  @Test
  void applicationGroupLoggingToFileWhenHasApplicationGroupWithParenthesis() {
    this.environment.setProperty("app.group", "mygroup (dev)");
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(this.initializationContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).contains("[mygroup (dev)] ");
  }

  @Test
  void applicationGroupLoggingToFileWhenDisabled(CapturedOutput output) {
    this.environment.setProperty("app.group", "myGroup");
    this.environment.setProperty("logging.include-application-group", "false");
    File file = new File(tmpDir(), "logback-test.log");
    LogFile logFile = getLogFile(file.getPath(), null);
    initialize(this.initializationContext, null, logFile);
    this.logger.info("Hello world");
    Assertions.assertThat(getLineWithText(file, "Hello world")).doesNotContain("myGroup").doesNotContain("null");
  }

  @Test
  void shouldNotContainAnsiEscapeCodes(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    this.logger.info("Hello world");
    assertThat(output).doesNotContain("\033[");
  }

  @Test
  void getEnvironment() {
    this.loggingSystem.beforeInitialize();
    initialize(this.initializationContext, null, null);
    assertThat(this.logger.getLoggerContext().getObject(Environment.class.getName())).isSameAs(this.environment);
  }

  @Test
  void getEnvironmentWhenUsingFile() {
    this.loggingSystem.beforeInitialize();
    LogFile logFile = getLogFile(tmpDir() + "/example.log", null, false);
    initialize(this.initializationContext, "classpath:logback-nondefault.xml", logFile);
    assertThat(this.logger.getLoggerContext().getObject(Environment.class.getName())).isSameAs(this.environment);
  }

  private void initialize(LoggingStartupContext context, String configLocation, LogFile logFile) {
    this.loggingSystem.getSystemProperties((ConfigurableEnvironment) context.getEnvironment()).apply(logFile);
    this.loggingSystem.beforeInitialize();
    this.loggingSystem.initialize(context, configLocation, logFile);
  }

  private static Logger getRootLogger() {
    ILoggerFactory factory = LoggerFactory.getILoggerFactory();
    LoggerContext context = (LoggerContext) factory;
    return context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
  }

  private static ConsoleAppender<?> getConsoleAppender() {
    return (ConsoleAppender<?>) getRootLogger().getAppender("CONSOLE");
  }

  private static RollingFileAppender<?> getFileAppender() {
    return (RollingFileAppender<?>) getRootLogger().getAppender("FILE");
  }

  private static SizeAndTimeBasedRollingPolicy<?> getRollingPolicy() {
    return (SizeAndTimeBasedRollingPolicy<?>) getFileAppender().getRollingPolicy();
  }

}
