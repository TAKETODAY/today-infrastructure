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

package cn.taketoday.framework.logging.logback;

import java.nio.charset.Charset;
import java.util.function.BiConsumer;

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
 * @since 4.0
 */
public class LogbackLoggingSystemProperties extends LoggingSystemProperties {

  private static final boolean JBOSS_LOGGING_PRESENT = ClassUtils.isPresent(
          "org.jboss.logging.Logger", LogbackLoggingSystemProperties.class.getClassLoader());

  /**
   * The name of the System property that contains the rolled-over log file name
   * pattern.
   */
  public static final String ROLLINGPOLICY_FILE_NAME_PATTERN = "LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN";

  /**
   * The name of the System property that contains the clean history on start flag.
   */
  public static final String ROLLINGPOLICY_CLEAN_HISTORY_ON_START = "LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START";

  /**
   * The name of the System property that contains the file log max size.
   */
  public static final String ROLLINGPOLICY_MAX_FILE_SIZE = "LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE";

  /**
   * The name of the System property that contains the file total size cap.
   */
  public static final String ROLLINGPOLICY_TOTAL_SIZE_CAP = "LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP";

  /**
   * The name of the System property that contains the file log max history.
   */
  public static final String ROLLINGPOLICY_MAX_HISTORY = "LOGBACK_ROLLINGPOLICY_MAX_HISTORY";

  public LogbackLoggingSystemProperties(Environment environment) {
    super(environment);
  }

  /**
   * Create a new {@link LogbackLoggingSystemProperties} instance.
   *
   * @param environment the source environment
   * @param setter setter used to apply the property
   */
  public LogbackLoggingSystemProperties(Environment environment, BiConsumer<String, String> setter) {
    super(environment, setter);
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
    applyRollingPolicy(resolver, ROLLINGPOLICY_FILE_NAME_PATTERN, "logging.logback.rollingpolicy.file-name-pattern", "logging.pattern.rolling-file-name");
    applyRollingPolicy(resolver, ROLLINGPOLICY_CLEAN_HISTORY_ON_START, "logging.logback.rollingpolicy.clean-history-on-start", "logging.file.clean-history-on-start");
    applyRollingPolicy(resolver, ROLLINGPOLICY_MAX_FILE_SIZE, "logging.logback.rollingpolicy.max-file-size", "logging.file.max-size", DataSize.class);
    applyRollingPolicy(resolver, ROLLINGPOLICY_TOTAL_SIZE_CAP, "logging.logback.rollingpolicy.total-size-cap", "logging.file.total-size-cap", DataSize.class);
    applyRollingPolicy(resolver, ROLLINGPOLICY_MAX_HISTORY, "logging.logback.rollingpolicy.max-history", "logging.file.max-history");
  }

  private void applyRollingPolicy(PropertyResolver resolver,
          String systemPropertyName, String propertyName, String deprecatedPropertyName) {
    applyRollingPolicy(resolver, systemPropertyName, propertyName, deprecatedPropertyName, String.class);
  }

  private <T> void applyRollingPolicy(PropertyResolver resolver,
          String systemPropertyName, String propertyName, String deprecatedPropertyName, Class<T> type) {
    T value = getProperty(resolver, propertyName, type);
    if (value == null) {
      value = getProperty(resolver, deprecatedPropertyName, type);
    }
    if (value != null) {
      String stringValue = String.valueOf((value instanceof DataSize dataSize) ? dataSize.toBytes() : value);
      setSystemProperty(systemPropertyName, stringValue);
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
