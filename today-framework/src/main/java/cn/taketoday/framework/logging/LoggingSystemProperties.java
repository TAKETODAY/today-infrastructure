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

package cn.taketoday.framework.logging;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

import cn.taketoday.core.ApplicationPid;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertySourcesPropertyResolver;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Utility to set system properties that can later be used by log configuration files.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Vedran Pavic
 * @author Robert Thornton
 * @author Eddú Meléndez
 * @since 4.0
 */
public class LoggingSystemProperties {

  /**
   * The name of the System property that contains the process ID.
   */
  public static final String PID_KEY = "PID";

  /**
   * The name of the System property that contains the exception conversion word.
   */
  public static final String EXCEPTION_CONVERSION_WORD = "LOG_EXCEPTION_CONVERSION_WORD";

  /**
   * The name of the System property that contains the log file.
   */
  public static final String LOG_FILE = "LOG_FILE";

  /**
   * The name of the System property that contains the log path.
   */
  public static final String LOG_PATH = "LOG_PATH";

  /**
   * The name of the System property that contains the console log pattern.
   */
  public static final String CONSOLE_LOG_PATTERN = "CONSOLE_LOG_PATTERN";

  /**
   * The name of the System property that contains the console log charset.
   */
  public static final String CONSOLE_LOG_CHARSET = "CONSOLE_LOG_CHARSET";

  /**
   * The log level threshold for console log.
   */
  public static final String CONSOLE_LOG_THRESHOLD = "CONSOLE_LOG_THRESHOLD";

  /**
   * The name of the System property that contains the file log pattern.
   */
  public static final String FILE_LOG_PATTERN = "FILE_LOG_PATTERN";

  /**
   * The name of the System property that contains the file log charset.
   */
  public static final String FILE_LOG_CHARSET = "FILE_LOG_CHARSET";

  /**
   * The log level threshold for file log.
   */
  public static final String FILE_LOG_THRESHOLD = "FILE_LOG_THRESHOLD";

  /**
   * The name of the System property that contains the log level pattern.
   */
  public static final String LOG_LEVEL_PATTERN = "LOG_LEVEL_PATTERN";

  /**
   * The name of the System property that contains the log date-format pattern.
   */
  public static final String LOG_DATEFORMAT_PATTERN = "LOG_DATEFORMAT_PATTERN";

  private static final BiConsumer<String, String> systemPropertySetter = (name, value) -> {
    if (System.getProperty(name) == null && value != null) {
      System.setProperty(name, value);
    }
  };

  private final Environment environment;

  private final BiConsumer<String, String> setter;

  /**
   * Create a new {@link LoggingSystemProperties} instance.
   *
   * @param environment the source environment
   */
  public LoggingSystemProperties(Environment environment) {
    this(environment, systemPropertySetter);
  }

  /**
   * Create a new {@link LoggingSystemProperties} instance.
   *
   * @param environment the source environment
   * @param setter setter used to apply the property
   */
  public LoggingSystemProperties(Environment environment, BiConsumer<String, String> setter) {
    Assert.notNull(environment, "Environment must not be null");
    Assert.notNull(setter, "Setter must not be null");
    this.environment = environment;
    this.setter = setter;
  }

  protected Charset getDefaultCharset() {
    return StandardCharsets.UTF_8;
  }

  public final void apply() {
    apply(null);
  }

  public final void apply(@Nullable LogFile logFile) {
    PropertyResolver resolver = getPropertyResolver();
    apply(logFile, resolver);
  }

  protected void apply(@Nullable LogFile logFile, PropertyResolver resolver) {
    setSystemProperty(resolver, EXCEPTION_CONVERSION_WORD, "logging.exception-conversion-word");
    setSystemProperty(PID_KEY, new ApplicationPid().toString());
    setSystemProperty(resolver, CONSOLE_LOG_PATTERN, "logging.pattern.console");
    setSystemProperty(resolver, CONSOLE_LOG_CHARSET, "logging.charset.console", getDefaultCharset().name());
    setSystemProperty(resolver, CONSOLE_LOG_THRESHOLD, "logging.threshold.console");
    setSystemProperty(resolver, LOG_DATEFORMAT_PATTERN, "logging.pattern.dateformat");
    setSystemProperty(resolver, FILE_LOG_PATTERN, "logging.pattern.file");
    setSystemProperty(resolver, FILE_LOG_CHARSET, "logging.charset.file", getDefaultCharset().name());
    setSystemProperty(resolver, FILE_LOG_THRESHOLD, "logging.threshold.file");
    setSystemProperty(resolver, LOG_LEVEL_PATTERN, "logging.pattern.level");

    if (logFile != null) {
      logFile.applyToSystemProperties();
    }
  }

  private PropertyResolver getPropertyResolver() {
    if (environment instanceof ConfigurableEnvironment configurableEnv) {
      var resolver = new PropertySourcesPropertyResolver(configurableEnv.getPropertySources());
      resolver.setConversionService(configurableEnv.getConversionService());
      resolver.setIgnoreUnresolvableNestedPlaceholders(true);
      return resolver;
    }
    return environment;
  }

  protected final void setSystemProperty(PropertyResolver resolver,
          String systemPropertyName, String propertyName) {
    setSystemProperty(resolver, systemPropertyName, propertyName, null);
  }

  protected final void setSystemProperty(PropertyResolver resolver,
          String systemPropertyName, String propertyName, @Nullable String defaultValue) {
    String value = resolver.getProperty(propertyName);
    if (value == null) {
      value = defaultValue;
    }
    setSystemProperty(systemPropertyName, value);
  }

  protected final void setSystemProperty(String name, @Nullable String value) {
    setter.accept(name, value);
  }

}
