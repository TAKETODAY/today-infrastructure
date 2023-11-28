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

import java.nio.charset.Charset;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ch.qos.logback.core.util.FileSize;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.framework.logging.LogFile;
import cn.taketoday.framework.logging.LoggingSystemProperties;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.DataSize;

/**
 * {@link LoggingSystemProperties} for Logback.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RollingPolicySystemProperty
 * @since 4.0
 */
public class LogbackLoggingSystemProperties extends LoggingSystemProperties {

  private static final boolean JBOSS_LOGGING_PRESENT = ClassUtils.isPresent(
          "org.jboss.logging.Logger", LogbackLoggingSystemProperties.class.getClassLoader());

  public LogbackLoggingSystemProperties(Environment environment) {
    super(environment);
  }

  /**
   * Create a new {@link LoggingSystemProperties} instance.
   *
   * @param environment the source environment
   * @param defaultValueResolver function used to resolve default values or {@code null}
   * @param setter setter used to apply the property or {@code null} for system
   * properties
   */
  public LogbackLoggingSystemProperties(Environment environment,
          Function<String, String> defaultValueResolver, @Nullable BiConsumer<String, String> setter) {
    super(environment, defaultValueResolver, setter);
  }

  @Override
  protected Charset getDefaultCharset() {
    return Charset.defaultCharset();
  }

  @Override
  protected void apply(@Nullable LogFile logFile, PropertyResolver resolver) {
    super.apply(logFile, resolver);
    applyJBossLoggingProperties();
    applyRollingPolicyProperties(resolver);
  }

  private void applyJBossLoggingProperties() {
    if (JBOSS_LOGGING_PRESENT) {
      setSystemProperty("org.jboss.logging.provider", "slf4j");
    }
  }

  private void applyRollingPolicyProperties(PropertyResolver resolver) {
    applyRollingPolicy(RollingPolicySystemProperty.FILE_NAME_PATTERN, resolver);
    applyRollingPolicy(RollingPolicySystemProperty.CLEAN_HISTORY_ON_START, resolver);
    applyRollingPolicy(RollingPolicySystemProperty.MAX_FILE_SIZE, resolver, DataSize.class);
    applyRollingPolicy(RollingPolicySystemProperty.TOTAL_SIZE_CAP, resolver, DataSize.class);
    applyRollingPolicy(RollingPolicySystemProperty.MAX_HISTORY, resolver);
  }

  private void applyRollingPolicy(RollingPolicySystemProperty property, PropertyResolver resolver) {
    applyRollingPolicy(property, resolver, String.class);
  }

  private <T> void applyRollingPolicy(RollingPolicySystemProperty property, PropertyResolver resolver, Class<T> type) {
    T value = getProperty(resolver, property.getApplicationPropertyName(), type);
    if (value == null) {
      value = getProperty(resolver, property.getDeprecatedApplicationPropertyName(), type);
    }
    if (value != null) {
      String stringValue = String.valueOf((value instanceof DataSize dataSize) ? dataSize.toBytes() : value);
      setSystemProperty(property.getEnvironmentVariableName(), stringValue);
    }
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> T getProperty(PropertyResolver resolver, String key, Class<T> type) {
    try {
      return resolver.getProperty(key, type);
    }
    catch (ConversionFailedException | ConverterNotFoundException ex) {
      if (type != DataSize.class) {
        throw ex;
      }
      String value = resolver.getProperty(key);
      return (T) DataSize.ofBytes(FileSize.valueOf(value).getSize());
    }
  }

}
