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

import java.io.File;
import java.util.Properties;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * A reference to a log output file. Log output files are specified using
 * {@code logging.file.name} or {@code logging.file.path} {@link Environment} properties.
 * If the {@code logging.file.name} property is not specified {@code "infra-app.log"} will be
 * written in the {@code logging.file.path} directory.
 *
 * @author Phillip Webb
 * @author Christian Carriere-Tisseur
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #get(PropertyResolver)
 * @since 4.0
 */
public class LogFile {

  public static final String DEFAULT_LOG_FILE_NAME = "infra-app.log";
  /**
   * The name of the Infra property that contains the name of the log file. Names can
   * be an exact location or relative to the current directory.
   */
  public static final String FILE_NAME_PROPERTY = "logging.file.name";

  /**
   * The name of the Infra property that contains the directory where log files are
   * written.
   */
  public static final String FILE_PATH_PROPERTY = "logging.file.path";

  @Nullable
  private final String file;

  @Nullable
  private final String path;

  /**
   * Create a new {@link LogFile} instance.
   *
   * @param file a reference to the file to write
   */
  LogFile(String file) {
    this(file, null);
  }

  /**
   * Create a new {@link LogFile} instance.
   *
   * @param file a reference to the file to write
   * @param path a reference to the logging path to use if {@code file} is not specified
   */
  LogFile(@Nullable String file, @Nullable String path) {
    Assert.isTrue(StringUtils.isNotEmpty(file) || StringUtils.isNotEmpty(path),
            "File or Path must not be empty");
    this.file = file;
    this.path = path;
  }

  /**
   * Apply log file details to {@code LOG_PATH} and {@code LOG_FILE} system properties.
   */
  public void applyToSystemProperties() {
    applyTo(System.getProperties());
  }

  /**
   * Apply log file details to {@code LOG_PATH} and {@code LOG_FILE} map entries.
   *
   * @param properties the properties to apply to
   */
  public void applyTo(Properties properties) {
    put(properties, LoggingSystemProperty.LOG_PATH, this.path);
    put(properties, LoggingSystemProperty.LOG_FILE, toString());
  }

  private void put(Properties properties, LoggingSystemProperty property, @Nullable String value) {
    if (StringUtils.isNotEmpty(value)) {
      properties.put(property.getEnvironmentVariableName(), value);
    }
  }

  @Override
  public String toString() {
    if (StringUtils.isNotEmpty(this.file)) {
      return this.file;
    }
    return new File(this.path, DEFAULT_LOG_FILE_NAME).getPath();
  }

  /**
   * Get a {@link LogFile} from the given Infra {@link Environment}.
   *
   * @param propertyResolver the {@link PropertyResolver} used to obtain the logging
   * properties
   * @return a {@link LogFile} or {@code null} if the environment didn't contain any
   * suitable properties
   */
  @Nullable
  public static LogFile get(PropertyResolver propertyResolver) {
    String file = propertyResolver.getProperty(FILE_NAME_PROPERTY);
    String path = propertyResolver.getProperty(FILE_PATH_PROPERTY);
    if (StringUtils.isNotEmpty(file) || StringUtils.isNotEmpty(path)) {
      return new LogFile(file, path);
    }
    return null;
  }

}
