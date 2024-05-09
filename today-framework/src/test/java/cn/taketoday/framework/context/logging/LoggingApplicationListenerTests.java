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

package cn.taketoday.framework.context.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.context.event.SimpleApplicationEventMulticaster;
import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.ApplicationPid;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationArguments;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.DefaultBootstrapContext;
import cn.taketoday.framework.context.event.ApplicationFailedEvent;
import cn.taketoday.framework.context.event.ApplicationStartingEvent;
import cn.taketoday.framework.logging.AbstractLoggingSystem;
import cn.taketoday.framework.logging.LogFile;
import cn.taketoday.framework.logging.LogLevel;
import cn.taketoday.framework.logging.LoggerConfiguration;
import cn.taketoday.framework.logging.LoggerGroups;
import cn.taketoday.framework.logging.LoggingStartupContext;
import cn.taketoday.framework.logging.LoggingSystem;
import cn.taketoday.framework.logging.LoggingSystemProperty;
import cn.taketoday.framework.logging.java.JavaLoggingSystem;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.logging.SLF4JBridgeHandler;
import cn.taketoday.test.classpath.ClassPathExclusions;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link LoggingApplicationListener} with Logback.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Ben Hale
 * @author Fahim Farook
 * @author Eddú Meléndez
 */
@ExtendWith(OutputCaptureExtension.class)
@ClassPathExclusions("log4j*.jar")
class LoggingApplicationListenerTests {

  private static final String[] NO_ARGS = {};

  private final LoggingApplicationListener listener = new LoggingApplicationListener();

  private final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

  private final ch.qos.logback.classic.Logger logger = this.loggerContext.getLogger(getClass());

  private final DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

  private final Application infraApplication = new Application(TestConfiguration.class);

  private final GenericApplicationContext context = new GenericApplicationContext();

  @TempDir
  public Path tempDir;

  private File logFile;

  private Set<Object> systemPropertyNames;

  private CapturedOutput output;

  @BeforeEach
  void init(CapturedOutput output) throws IOException {
    this.systemPropertyNames = new HashSet<>(System.getProperties().keySet());
    this.output = output;
    this.logFile = new File(this.tempDir.toFile(), "foo.log");
    LogManager.getLogManager().readConfiguration(JavaLoggingSystem.class.getResourceAsStream("logging.properties"));
    multicastEvent(new ApplicationStartingEvent(this.bootstrapContext, new Application(),
            new ApplicationArguments(NO_ARGS)));
    new File(this.tempDir.toFile(), "infra-app.log").delete();
    ConfigurableEnvironment environment = this.context.getEnvironment();
    ConfigurationPropertySources.attach(environment);
  }

  @AfterEach
  void clear() {
    LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());
    loggingSystem.setLogLevel("ROOT", LogLevel.INFO);
    loggingSystem.cleanUp();
    if (loggingSystem.getShutdownHandler() != null) {
      loggingSystem.getShutdownHandler().run();
    }
    System.clearProperty(LoggingSystem.class.getName());
    System.clearProperty(LoggingSystem.SYSTEM_PROPERTY);
    System.getProperties().keySet().retainAll(this.systemPropertyNames);
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void baseConfigLocation() {
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.info("Hello world", new RuntimeException("Expected"));
    assertThat(this.output).contains("Hello world");
    assertThat(this.output).doesNotContain("???");
    assertThat(this.output).contains("[junit-");
    assertThat(new File(this.tempDir + "/infra-app.log")).doesNotExist();
  }

  @Test
  void overrideConfigLocation() {
    addPropertiesToEnvironment(this.context, "logging.config=classpath:logback-nondefault.xml");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.info("Hello world");
    assertThat(this.output).contains("Hello world").doesNotContain("???").startsWith("null ").endsWith("BOOTBOOT");
  }

  @Test
  @ClassPathExclusions("janino-*.jar")
  void tryingToUseJaninoWhenItIsNotOnTheClasspathFailsGracefully(CapturedOutput output) {
    addPropertiesToEnvironment(this.context, "logging.config=classpath:logback-janino.xml");
    assertThatIllegalStateException()
            .isThrownBy(() -> this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader()));
    assertThat(output)
            .contains("Logging system failed to initialize using configuration from 'classpath:logback-janino.xml'");
  }

  @Test
  void trailingWhitespaceInLoggingConfigShouldBeTrimmed() {
    addPropertiesToEnvironment(this.context, "logging.config=classpath:logback-nondefault.xml ");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.info("Hello world");
    assertThat(this.output).contains("Hello world").doesNotContain("???").startsWith("null ").endsWith("BOOTBOOT");
  }

  @Test
  void overrideConfigDoesNotExist() {
    addPropertiesToEnvironment(this.context, "logging.config=doesnotexist.xml");
    assertThatIllegalStateException()
            .isThrownBy(() -> this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader()));
    assertThat(this.output)
            .contains("Logging system failed to initialize using configuration from 'doesnotexist.xml'")
            .doesNotContain("JoranException");
  }

  @Test
  void azureDefaultLoggingConfigDoesNotCauseAFailure() {
    addPropertiesToEnvironment(this.context,
            "logging.config=-Djava.util.logging.config.file=\"d:\\home\\site\\wwwroot\\bin\\apache-tomcat-7.0.52\\conf\\logging.properties\"");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.info("Hello world");
    assertThat(this.output).contains("Hello world").doesNotContain("???");
    assertThat(new File(this.tempDir.toFile(), "/infra-app.log")).doesNotExist();
  }

  @Test
  void tomcatNopLoggingConfigDoesNotCauseAFailure() {
    addPropertiesToEnvironment(this.context, "LOGGING_CONFIG=-Dnop");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.info("Hello world");
    assertThat(this.output).contains("Hello world").doesNotContain("???");
    assertThat(new File(this.tempDir.toFile(), "/infra-app.log")).doesNotExist();
  }

  @Test
  void overrideConfigBroken() {
    addPropertiesToEnvironment(this.context, "logging.config=classpath:logback-broken.xml");
    assertThatIllegalStateException().isThrownBy(() -> {
      this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
      assertThat(this.output).contains(
              "Logging system failed to initialize using configuration from 'classpath:logback-broken.xml'");
      assertThat(this.output).contains("ConsolAppender");
    });
  }

  @Test
  void addLogFileProperty() {
    addPropertiesToEnvironment(this.context, "logging.config=classpath:logback-nondefault.xml",
            "logging.file.name=" + this.logFile);
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    var logger = LoggerFactory.getLogger(LoggingApplicationListenerTests.class);
    String existingOutput = this.output.toString();
    logger.info("Hello world");
    String output = this.output.toString().substring(existingOutput.length()).trim();
    assertThat(output).startsWith(this.logFile.getAbsolutePath());
  }

  @Test
  void addLogFilePropertyWithDefault() {
    assertThat(this.logFile).doesNotExist();
    addPropertiesToEnvironment(this.context, "logging.file.name=" + this.logFile);
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    var logger = LoggerFactory.getLogger(LoggingApplicationListenerTests.class);
    logger.info("Hello world");
    assertThat(this.logFile).isFile();
  }

  @Test
  void addLogPathProperty() {
    addPropertiesToEnvironment(this.context, "logging.config=classpath:logback-nondefault.xml",
            "logging.file.path=" + this.tempDir);
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
    String existingOutput = this.output.toString();
    logger.info("Hello world");
    String output = this.output.toString().substring(existingOutput.length()).trim();
    assertThat(output).startsWith(new File(this.tempDir.toFile(), "infra-app.log").getAbsolutePath());
  }

  @Test
  void parseDebugArg() {
    addPropertiesToEnvironment(this.context, "debug");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    this.logger.trace("testattrace");
    assertThat(this.output).contains("testatdebug");
    assertThat(this.output).doesNotContain("testattrace");
  }

  @Test
  void parseDebugArgExpandGroups() {
    addPropertiesToEnvironment(this.context, "debug");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.loggerContext.getLogger("cn.taketoday.framework.actuate.endpoint.web").debug("testdebugwebgroup");
    this.loggerContext.getLogger("org.hibernate.SQL").debug("testdebugsqlgroup");
    assertThat(this.output).contains("testdebugwebgroup");
    assertThat(this.output).contains("testdebugsqlgroup");
    LoggerGroups loggerGroups = (LoggerGroups) ReflectionTestUtils.getField(this.listener, "loggerGroups");
    assertThat(loggerGroups.get("web").getConfiguredLevel()).isEqualTo(LogLevel.DEBUG);
  }

  @Test
  void parseTraceArg() {
    addPropertiesToEnvironment(this.context, "trace");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    this.logger.trace("testattrace");
    assertThat(this.output).contains("testatdebug");
    assertThat(this.output).contains("testattrace");
  }

  @Test
  void disableDebugArg() {
    disableDebugTraceArg("debug=false");
  }

  @Test
  void disableTraceArg() {
    disableDebugTraceArg("trace=false");
  }

  private void disableDebugTraceArg(String... environment) {
    addPropertiesToEnvironment(this.context, environment);
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    this.logger.trace("testattrace");
    assertThat(this.output).doesNotContain("testatdebug");
    assertThat(this.output).doesNotContain("testattrace");
  }

  @Test
  void parseLevels() {
    addPropertiesToEnvironment(this.context, "logging.level.cn.taketoday.framework=TRACE");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    this.logger.trace("testattrace");
    assertThat(this.output).contains("testatdebug");
    assertThat(this.output).contains("testattrace");
  }

  @Test
  void parseLevelsCaseInsensitive() {
    addPropertiesToEnvironment(this.context, "logging.level.cn.taketoday.framework=TrAcE");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    this.logger.trace("testattrace");
    assertThat(this.output).contains("testatdebug");
    assertThat(this.output).contains("testattrace");
  }

  @Test
  void parseLevelsTrimsWhitespace() {
    addPropertiesToEnvironment(this.context, "logging.level.cn.taketoday.framework= trace ");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    this.logger.trace("testattrace");
    assertThat(this.output).contains("testatdebug");
    assertThat(this.output).contains("testattrace");
  }

  @Test
  void parseLevelsWithPlaceholder() {
    addPropertiesToEnvironment(this.context, "foo=TRACE", "logging.level.cn.taketoday.framework=${foo}");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    this.logger.trace("testattrace");
    assertThat(this.output).contains("testatdebug");
    assertThat(this.output).contains("testattrace");
  }

  @Test
  void parseLevelsFails() {
    this.logger.setLevel(Level.INFO);
    addPropertiesToEnvironment(this.context, "logging.level.cn.taketoday.framework=GARBAGE");
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader()));
  }

  @Test
  void parseLevelsNone() {
    addPropertiesToEnvironment(this.context, "logging.level.cn.taketoday.framework=OFF");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    this.logger.error("testaterror");
    assertThat(this.output).doesNotContain("testatdebug").doesNotContain("testaterror");
  }

  @Test
  void parseLevelsMapsFalseToOff() {
    addPropertiesToEnvironment(this.context, "logging.level.cn.taketoday.framework=false");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    this.logger.error("testaterror");
    assertThat(this.output).doesNotContain("testatdebug").doesNotContain("testaterror");
  }

  @Test
  void parseArgsDisabled() {
    this.listener.setParseArgs(false);
    addPropertiesToEnvironment(this.context, "debug");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    assertThat(this.output).doesNotContain("testatdebug");
  }

  @Test
  void parseArgsDoesntReplace() {
    this.listener.setInfraLogging(LogLevel.ERROR);
    this.listener.setParseArgs(false);
    multicastEvent(new ApplicationStartingEvent(this.bootstrapContext, this.infraApplication,
            new ApplicationArguments("--debug")));
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    assertThat(this.output).doesNotContain("testatdebug");
  }

  @Test
  void bridgeHandlerLifecycle() {
    assertThat(bridgeHandlerInstalled()).isTrue();
    multicastEvent(new ContextClosedEvent(this.context));
    assertThat(bridgeHandlerInstalled()).isFalse();
  }

  @Test
  void defaultExceptionConversionWord() {
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.info("Hello world", new RuntimeException("Wrapper", new RuntimeException("Expected")));
    assertThat(this.output).contains("Hello world");
    assertThat(this.output).doesNotContain("Wrapped by: java.lang.RuntimeException: Wrapper");
  }

  @Test
  void overrideExceptionConversionWord() {
    addPropertiesToEnvironment(this.context, "logging.exceptionConversionWord=%rEx");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.info("Hello world", new RuntimeException("Wrapper", new RuntimeException("Expected")));
    assertThat(this.output).contains("Hello world");
    assertThat(this.output).contains("Wrapped by: java.lang.RuntimeException: Wrapper");
  }

  @Test
  void shutdownHookIsRegisteredByDefault() throws Exception {
    TestLoggingApplicationListener listener = new TestLoggingApplicationListener();
    Object registered = ReflectionTestUtils.getField(listener, TestLoggingApplicationListener.class,
            "shutdownHookRegistered");
    ((AtomicBoolean) registered).set(false);
    System.setProperty(LoggingSystem.class.getName(), TestShutdownHandlerLoggingSystem.class.getName());
    multicastEvent(listener, new ApplicationStartingEvent(this.bootstrapContext, new Application(), new ApplicationArguments(NO_ARGS)));
    listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    assertThat(listener.shutdownHook).isNotNull();
    listener.shutdownHook.run();
    assertThat(TestShutdownHandlerLoggingSystem.shutdownLatch.await(30, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void shutdownHookRegistrationCanBeDisabled() {
    TestLoggingApplicationListener listener = new TestLoggingApplicationListener();
    Object registered = ReflectionTestUtils.getField(listener, TestLoggingApplicationListener.class,
            "shutdownHookRegistered");
    ((AtomicBoolean) registered).set(false);
    System.setProperty(LoggingSystem.class.getName(), TestShutdownHandlerLoggingSystem.class.getName());
    addPropertiesToEnvironment(this.context, "logging.register_shutdown_hook=false");
    multicastEvent(listener, new ApplicationStartingEvent(this.bootstrapContext, new Application(), new ApplicationArguments(NO_ARGS)));
    listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    assertThat(listener.shutdownHook).isNull();
  }

  @Test
  void closingContextCleansUpLoggingSystem() {
    System.setProperty(LoggingSystem.SYSTEM_PROPERTY, TestCleanupLoggingSystem.class.getName());
    multicastEvent(new ApplicationStartingEvent(this.bootstrapContext, this.infraApplication, new ApplicationArguments(new String[0])));
    TestCleanupLoggingSystem loggingSystem = (TestCleanupLoggingSystem) ReflectionTestUtils.getField(this.listener,
            "loggingSystem");
    assertThat(loggingSystem.cleanedUp).isFalse();
    multicastEvent(new ContextClosedEvent(this.context));
    assertThat(loggingSystem.cleanedUp).isTrue();
  }

  @Test
  void closingChildContextDoesNotCleanUpLoggingSystem() {
    System.setProperty(LoggingSystem.SYSTEM_PROPERTY, TestCleanupLoggingSystem.class.getName());
    multicastEvent(new ApplicationStartingEvent(this.bootstrapContext, this.infraApplication, new ApplicationArguments(new String[0])));
    TestCleanupLoggingSystem loggingSystem = (TestCleanupLoggingSystem) ReflectionTestUtils.getField(this.listener,
            "loggingSystem");
    assertThat(loggingSystem.cleanedUp).isFalse();
    GenericApplicationContext childContext = new GenericApplicationContext();
    childContext.setParent(this.context);
    multicastEvent(new ContextClosedEvent(childContext));
    assertThat(loggingSystem.cleanedUp).isFalse();
    multicastEvent(new ContextClosedEvent(this.context));
    assertThat(loggingSystem.cleanedUp).isTrue();
    childContext.close();
  }

  @Test
  void systemPropertiesAreSetForLoggingConfiguration() {
    addPropertiesToEnvironment(this.context, "logging.exception-conversion-word=conversion",
            "logging.file.name=" + this.logFile, "logging.file.path=path", "logging.pattern.console=console",
            "logging.pattern.file=file", "logging.pattern.level=level", "logging.pattern.correlation=correlation",
            "logging.pattern.rolling-file-name=my.log.%d{yyyyMMdd}.%i.gz");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    assertThat(getSystemProperty(LoggingSystemProperty.CONSOLE_PATTERN)).isEqualTo("console");
    assertThat(getSystemProperty(LoggingSystemProperty.FILE_PATTERN)).isEqualTo("file");
    assertThat(getSystemProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD)).isEqualTo("conversion");
    assertThat(getSystemProperty(LoggingSystemProperty.LOG_FILE)).isEqualTo(this.logFile.getAbsolutePath());
    assertThat(getSystemProperty(LoggingSystemProperty.LEVEL_PATTERN)).isEqualTo("level");
    assertThat(getSystemProperty(LoggingSystemProperty.LOG_PATH)).isEqualTo("path");
    assertThat(getSystemProperty(LoggingSystemProperty.PID)).isNotNull();
  }

  @Test
  void environmentPropertiesIgnoreUnresolvablePlaceholders() {
    // gh-7719
    addPropertiesToEnvironment(this.context, "logging.pattern.console=console ${doesnotexist}");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    assertThat(getSystemProperty(LoggingSystemProperty.CONSOLE_PATTERN)).isEqualTo("console ${doesnotexist}");
  }

  @Test
  void environmentPropertiesResolvePlaceholders() {
    addPropertiesToEnvironment(this.context, "logging.pattern.console=console ${pid}");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    assertThat(getSystemProperty(LoggingSystemProperty.CONSOLE_PATTERN))
            .isEqualTo(this.context.getEnvironment().getProperty("logging.pattern.console"));
  }

  @Test
  void logFilePropertiesCanReferenceSystemProperties() {
    addPropertiesToEnvironment(this.context, "logging.file.name=" + this.tempDir + "${PID}.log");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    assertThat(getSystemProperty(LoggingSystemProperty.LOG_FILE))
            .isEqualTo(this.tempDir + new ApplicationPid().toString() + ".log");
  }

  @Test
  void applicationFailedEventCleansUpLoggingSystem() {
    System.setProperty(LoggingSystem.SYSTEM_PROPERTY, TestCleanupLoggingSystem.class.getName());
    multicastEvent(new ApplicationStartingEvent(this.bootstrapContext, this.infraApplication, new ApplicationArguments()));
    TestCleanupLoggingSystem loggingSystem = (TestCleanupLoggingSystem) ReflectionTestUtils.getField(this.listener,
            "loggingSystem");
    assertThat(loggingSystem.cleanedUp).isFalse();
    multicastEvent(new ApplicationFailedEvent(this.infraApplication, new ApplicationArguments(),
            new GenericApplicationContext(), new Exception()));
    assertThat(loggingSystem.cleanedUp).isTrue();
  }

  @Test
  void cleanupOccursAfterWebServerShutdown() {
    System.setProperty(LoggingSystem.SYSTEM_PROPERTY, TestCleanupLoggingSystem.class.getName());
    this.infraApplication.setApplicationType(ApplicationType.NORMAL);
    ConfigurableApplicationContext context = this.infraApplication.run();
    ApplicationListener<?> listener = this.infraApplication.getListeners()
            .stream()
            .filter(LoggingApplicationListener.class::isInstance)
            .findFirst()
            .get();
    TestCleanupLoggingSystem loggingSystem = (TestCleanupLoggingSystem) ReflectionTestUtils.getField(listener,
            "loggingSystem");
    assertThat(loggingSystem.cleanedUp).isFalse();
    WebServerStyleLifecycle lifecycle = context.getBean(WebServerStyleLifecycle.class);
    AtomicBoolean called = new AtomicBoolean();
    AtomicBoolean cleanupOnStop = new AtomicBoolean();
    lifecycle.onStop = () -> {
      called.set(true);
      cleanupOnStop.set(loggingSystem.cleanedUp);
    };
    context.close();
    assertThat(called).isTrue();
    assertThat(cleanupOnStop).isFalse();
    assertThat(loggingSystem.cleanedUp).isTrue();
  }

  @Test
  void lowPriorityPropertySourceShouldNotOverrideRootLoggerConfig() {
    PropertySources propertySources = this.context.getEnvironment().getPropertySources();
    propertySources
            .addFirst(new MapPropertySource("test1", Collections.singletonMap("logging.level.ROOT", "DEBUG")));
    propertySources.addLast(new MapPropertySource("test2", Collections.singletonMap("logging.level.root", "WARN")));
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    this.logger.debug("testatdebug");
    assertThat(this.output).contains("testatdebug");
  }

  @Test
  void loggingGroupsDefaultsAreApplied() {
    addPropertiesToEnvironment(this.context, "logging.level.web=TRACE");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    assertTraceEnabled("cn.taketoday.core", false);
    assertTraceEnabled("cn.taketoday.core.codec", true);
    assertTraceEnabled("cn.taketoday.http", true);
    assertTraceEnabled("cn.taketoday.web", true);
    assertTraceEnabled("cn.taketoday.actuate.endpoint.web", true);
  }

  @Test
  void loggingGroupsCanBeDefined() {
    addPropertiesToEnvironment(this.context, "logging.group.foo=com.foo.bar,com.foo.baz",
            "logging.level.foo=TRACE");
    this.listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
    assertTraceEnabled("com.foo", false);
    assertTraceEnabled("com.foo.bar", true);
    assertTraceEnabled("com.foo.baz", true);
  }

  private String getSystemProperty(LoggingSystemProperty property) {
    return System.getProperty(property.getEnvironmentVariableName());
  }

  private void assertTraceEnabled(String name, boolean expected) {
    assertThat(this.loggerContext.getLogger(name).isTraceEnabled()).isEqualTo(expected);
  }

  private void multicastEvent(ApplicationEvent event) {
    multicastEvent(this.listener, event);
  }

  private void multicastEvent(ApplicationListener<?> listener, ApplicationEvent event) {
    SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
    multicaster.addApplicationListener(listener);
    multicaster.multicastEvent(event);
  }

  private boolean bridgeHandlerInstalled() {
    return SLF4JBridgeHandler.isInstalled();
  }

  private void addPropertiesToEnvironment(ConfigurableApplicationContext context, String... pairs) {
    ConfigurableEnvironment environment = context.getEnvironment();
    Map<String, Object> properties = new HashMap<>();
    for (String pair : pairs) {
      String[] split = pair.split("=", 2);
      properties.put(split[0], (split.length == 2) ? split[1] : "");
    }
    MapPropertySource propertySource = new MapPropertySource("logging-config", properties);
    environment.getPropertySources().addFirst(propertySource);
  }

  static class TestShutdownHandlerLoggingSystem extends AbstractLoggingSystem {

    private static CountDownLatch shutdownLatch;

    TestShutdownHandlerLoggingSystem(ClassLoader classLoader) {
      super(classLoader);
      TestShutdownHandlerLoggingSystem.shutdownLatch = new CountDownLatch(1);
    }

    @Override
    protected String[] getStandardConfigLocations() {
      return new String[] { "foo.bar" };
    }

    @Override
    protected void loadDefaults(LoggingStartupContext startupContext, LogFile logFile) {
    }

    @Override
    protected void loadConfiguration(LoggingStartupContext initializationContext, String location,
            LogFile logFile) {
    }

    @Override
    public void setLogLevel(String loggerName, LogLevel level) {
    }

    @Override
    public List<LoggerConfiguration> getLoggerConfigurations() {
      return null;
    }

    @Override
    public LoggerConfiguration getLoggerConfiguration(String loggerName) {
      return null;
    }

    @Override
    public Runnable getShutdownHandler() {
      return () -> TestShutdownHandlerLoggingSystem.shutdownLatch.countDown();
    }

  }

  static class TestLoggingApplicationListener extends LoggingApplicationListener {

    private Runnable shutdownHook;

    @Override
    void registerShutdownHook(Runnable shutdownHook) {
      this.shutdownHook = shutdownHook;
    }

  }

  static final class TestCleanupLoggingSystem extends LoggingSystem {

    private boolean cleanedUp = false;

    TestCleanupLoggingSystem(ClassLoader classLoader) {
    }

    @Override
    public void beforeInitialize() {
    }

    @Override
    public void setLogLevel(String loggerName, LogLevel level) {
    }

    @Override
    public List<LoggerConfiguration> getLoggerConfigurations() {
      return null;
    }

    @Override
    public LoggerConfiguration getLoggerConfiguration(String loggerName) {
      return null;
    }

    @Override
    public void cleanUp() {
      this.cleanedUp = true;
    }

  }

  @Configuration
  @Import(WebServerStyleLifecycle.class)
  static class TestConfiguration {

  }

  static class WebServerStyleLifecycle implements SmartLifecycle {

    private volatile boolean running;

    Runnable onStop;

    @Override
    public void start() {
      this.running = true;
    }

    @Override
    public void stop() {
      this.running = false;
      this.onStop.run();
    }

    @Override
    public boolean isRunning() {
      return this.running;
    }

    @Override
    public int getPhase() {
      return Integer.MAX_VALUE - 1;
    }

  }

}
