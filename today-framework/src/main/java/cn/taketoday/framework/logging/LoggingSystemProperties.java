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
import java.util.function.Function;

import cn.taketoday.core.ApplicationPid;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertySourcesPropertyResolver;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Utility to set system properties that can later be used by log configuration files.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Vedran Pavic
 * @author Robert Thornton
 * @author Eddú Meléndez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LoggingSystemProperty
 * @since 4.0
 */
public class LoggingSystemProperties {

  private static final BiConsumer<String, String> systemPropertySetter = (name, value) -> {
    if (System.getProperty(name) == null && value != null) {
      System.setProperty(name, value);
    }
  };

  private final Environment environment;

  private final Function<String, String> defaultValueResolver;

  private final BiConsumer<String, String> setter;

  /**
   * Create a new {@link LoggingSystemProperties} instance.
   *
   * @param environment the source environment
   */
  public LoggingSystemProperties(Environment environment) {
    this(environment, null);
  }

  /**
   * Create a new {@link LoggingSystemProperties} instance.
   *
   * @param environment the source environment
   * @param setter setter used to apply the property or {@code null} for system
   * properties
   */
  public LoggingSystemProperties(Environment environment, @Nullable BiConsumer<String, String> setter) {
    this(environment, null, setter);
  }

  /**
   * Create a new {@link LoggingSystemProperties} instance.
   *
   * @param environment the source environment
   * @param defaultValueResolver function used to resolve default values or {@code null}
   * @param setter setter used to apply the property or {@code null} for system
   * properties
   */
  public LoggingSystemProperties(Environment environment, @Nullable Function<String, String> defaultValueResolver,
          @Nullable BiConsumer<String, String> setter) {
    Assert.notNull(environment, "Environment must not be null");
    this.environment = environment;
    this.defaultValueResolver = (defaultValueResolver != null) ? defaultValueResolver : (name) -> null;
    this.setter = (setter != null) ? setter : systemPropertySetter;
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

  private PropertyResolver getPropertyResolver() {
    if (this.environment instanceof ConfigurableEnvironment configurableEnvironment) {
      PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
              configurableEnvironment.getPropertySources());
      resolver.setConversionService(((ConfigurableEnvironment) this.environment).getConversionService());
      resolver.setIgnoreUnresolvableNestedPlaceholders(true);
      return resolver;
    }
    return this.environment;
  }

  protected void apply(@Nullable LogFile logFile, PropertyResolver resolver) {
    String defaultCharsetName = getDefaultCharset().name();
    setApplicationNameSystemProperty(resolver);
    setSystemProperty(LoggingSystemProperty.PID, new ApplicationPid().toString());
    setSystemProperty(LoggingSystemProperty.CONSOLE_CHARSET, resolver, defaultCharsetName);
    setSystemProperty(LoggingSystemProperty.FILE_CHARSET, resolver, defaultCharsetName);
    setSystemProperty(LoggingSystemProperty.CONSOLE_THRESHOLD, resolver);
    setSystemProperty(LoggingSystemProperty.FILE_THRESHOLD, resolver);
    setSystemProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD, resolver);
    setSystemProperty(LoggingSystemProperty.CONSOLE_PATTERN, resolver);
    setSystemProperty(LoggingSystemProperty.FILE_PATTERN, resolver);
    setSystemProperty(LoggingSystemProperty.LEVEL_PATTERN, resolver);
    setSystemProperty(LoggingSystemProperty.DATEFORMAT_PATTERN, resolver);
    setSystemProperty(LoggingSystemProperty.CORRELATION_PATTERN, resolver);
    if (logFile != null) {
      logFile.applyToSystemProperties();
    }
  }

  private void setApplicationNameSystemProperty(PropertyResolver resolver) {
    if (resolver.getProperty("logging.include-application-name", Boolean.class, Boolean.TRUE)) {
      String applicationName = resolver.getProperty("app.name");
      if (StringUtils.hasText(applicationName)) {
        setSystemProperty(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName(),
                "[%s] ".formatted(applicationName));
      }
    }
  }

  private void setSystemProperty(LoggingSystemProperty property, PropertyResolver resolver) {
    setSystemProperty(property, resolver, null);
  }

  private void setSystemProperty(LoggingSystemProperty property, PropertyResolver resolver, @Nullable String defaultValue) {
    String value = (property.getApplicationPropertyName() != null)
                   ? resolver.getProperty(property.getApplicationPropertyName()) : null;
    value = (value != null) ? value : this.defaultValueResolver.apply(property.getApplicationPropertyName());
    value = (value != null) ? value : defaultValue;
    setSystemProperty(property.getEnvironmentVariableName(), value);
  }

  private void setSystemProperty(LoggingSystemProperty property, @Nullable String value) {
    setSystemProperty(property.getEnvironmentVariableName(), value);
  }

  /**
   * Set a system property.
   *
   * @param name the property name
   * @param value the value
   */
  protected final void setSystemProperty(String name, @Nullable String value) {
    this.setter.accept(name, value);
  }

}
