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

package cn.taketoday.framework.logging.java;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import cn.taketoday.framework.logging.LoggingSystemProperties;

/**
 * Simple 'Java Logging' {@link Formatter}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class SimpleFormatter extends Formatter {

  private static final String DEFAULT_FORMAT = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL] - %8$s %4$s [%7$s] --- %3$s: %5$s%6$s%n";

  private final String format = getOrUseDefault("LOG_FORMAT", DEFAULT_FORMAT);

  private final String pid = getOrUseDefault(LoggingSystemProperties.PID_KEY, "????");

  private final Date date = new Date();

  @Override
  public synchronized String format(LogRecord record) {
    this.date.setTime(record.getMillis());
    String source = record.getLoggerName();
    String message = formatMessage(record);
    String throwable = getThrowable(record);
    String thread = getThreadName();
    return String.format(this.format, this.date, source, record.getLoggerName(),
            record.getLevel().getLocalizedName(), message, throwable, thread, this.pid);
  }

  private String getThrowable(LogRecord record) {
    if (record.getThrown() == null) {
      return "";
    }
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    printWriter.println();
    record.getThrown().printStackTrace(printWriter);
    printWriter.close();
    return stringWriter.toString();
  }

  private String getThreadName() {
    String name = Thread.currentThread().getName();
    return (name != null) ? name : "";
  }

  private static String getOrUseDefault(String key, String defaultValue) {
    String value = null;
    try {
      value = System.getenv(key);
    }
    catch (Exception ex) {
      // ignore
    }
    if (value == null) {
      value = defaultValue;
    }
    return System.getProperty(key, value);
  }

}
