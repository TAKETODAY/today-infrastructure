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

package cn.taketoday.framework.logging.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.pattern.PropertyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import cn.taketoday.framework.logging.LoggingSystemProperty;

/**
 * Logback {@link ClassicConverter} to convert the
 * {@link LoggingSystemProperty#APPLICATION_NAME APPLICATION_NAME} into a value suitable
 * for logging. Similar to Logback's {@link PropertyConverter} but a non-existent property
 * is logged as an empty string rather than {@code null}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ApplicationNameConverter extends ClassicConverter {

  @Override
  public String convert(ILoggingEvent event) {
    String applicationName = event.getLoggerContextVO()
            .getPropertyMap()
            .get(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName());
    if (applicationName == null) {
      applicationName = System.getProperty(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName());
      if (applicationName == null) {
        applicationName = "";
      }
    }
    return applicationName;
  }

}
