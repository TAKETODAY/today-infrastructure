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

package infra.app.logging.logback;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ch.qos.logback.core.util.FileSize;
import infra.app.logging.LogFile;
import infra.app.logging.LoggingSystemProperties;
import infra.core.conversion.ConversionFailedException;
import infra.core.conversion.ConverterNotFoundException;
import infra.core.env.Environment;
import infra.core.env.PropertyResolver;
import infra.util.ClassUtils;
import infra.util.DataSize;

/**
 * {@link LoggingSystemProperties} for Logback.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RollingPolicySystemProperty
 * @since 4.0
 */
@SuppressWarnings("NullAway")
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
