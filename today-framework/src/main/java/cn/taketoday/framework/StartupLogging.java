/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.framework;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.Duration;
import java.util.concurrent.Callable;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Version;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 11:09
 */
final class StartupLogging {

  private static final long HOST_NAME_RESOLVE_THRESHOLD = 200;

  private final Class<?> sourceClass;

  StartupLogging(Class<?> sourceClass) {
    this.sourceClass = sourceClass;
  }

  void logStarting(Logger applicationLog) {
    Assert.notNull(applicationLog, "Logger is required");
    applicationLog.info(LogMessage.from(this::getStartingMessage));
    applicationLog.debug(LogMessage.from(this::getRunningMessage));
  }

  void logStarted(Logger applicationLog, Duration timeTakenToStartup) {
    if (applicationLog.isInfoEnabled()) {
      applicationLog.info(getStartedMessage(timeTakenToStartup));
    }
  }

  private CharSequence getStartingMessage() {
    StringBuilder message = new StringBuilder();
    message.append("Starting ");
    appendApplicationName(message);
    appendVersion(message, this.sourceClass);
    appendJavaVersion(message);
    appendOn(message);
    appendPid(message);
    appendContext(message);
    return message;
  }

  private CharSequence getRunningMessage() {
    StringBuilder message = new StringBuilder();
    message.append("Running with today-framework");
    append(message, null, Version::get);
    return message;
  }

  private CharSequence getStartedMessage(Duration timeTakenToStartup) {
    StringBuilder message = new StringBuilder();
    message.append("Started ");
    appendApplicationName(message);
    message.append(" in ");
    message.append(timeTakenToStartup.toMillis() / 1000.0);
    message.append(" seconds");
    try {
      double uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0;
      message.append(" (JVM running for ").append(uptime).append(")");
    }
    catch (Throwable ex) {
      // No JVM time available
    }
    return message;
  }

  private void appendApplicationName(StringBuilder message) {
    String name = (this.sourceClass != null) ? ClassUtils.getShortName(this.sourceClass) : "application";
    message.append(name);
  }

  private void appendVersion(StringBuilder message, Class<?> source) {
    append(message, "v", () -> source.getPackage().getImplementationVersion());
  }

  private void appendOn(StringBuilder message) {
    long startTime = System.currentTimeMillis();
    append(message, "on ", () -> InetAddress.getLocalHost().getHostName());
    long resolveTime = System.currentTimeMillis() - startTime;
    if (resolveTime > HOST_NAME_RESOLVE_THRESHOLD) {
      StringBuilder warning = new StringBuilder();
      warning.append("InetAddress.getLocalHost().getHostName() took ");
      warning.append(resolveTime);
      warning.append(" milliseconds to respond.");
      warning.append(" Please verify your network configuration");
      if (System.getProperty("os.name").toLowerCase().contains("mac")) {
        warning.append(" (macOS machines may need to add entries to /etc/hosts)");
      }
      warning.append(".");
      LoggerFactory.getLogger(StartupLogging.class).warn(warning);
    }
  }

  private void appendPid(StringBuilder message) {
    append(message, "with PID ", ApplicationPid::new);
  }

  private void appendContext(StringBuilder message) {
    StringBuilder context = new StringBuilder();
    ApplicationHome home = new ApplicationHome(this.sourceClass);
    if (home.getSource() != null) {
      context.append(home.getSource().getAbsolutePath());
    }
    append(context, "started by ", () -> System.getProperty("user.name"));
    append(context, "in ", () -> System.getProperty("user.dir"));
    if (context.length() > 0) {
      message.append(" (");
      message.append(context);
      message.append(")");
    }
  }

  private void appendJavaVersion(StringBuilder message) {
    append(message, "using Java ", () -> System.getProperty("java.version"));
  }

  private void append(StringBuilder message, String prefix, Callable<Object> call) {
    append(message, prefix, call, "");
  }

  private void append(
          StringBuilder message, @Nullable String prefix, Callable<Object> call, String defaultValue) {
    Object result = callIfPossible(call);
    String value = (result != null) ? result.toString() : null;
    if (StringUtils.isEmpty(value)) {
      value = defaultValue;
    }
    if (StringUtils.isNotEmpty(value)) {
      message.append((message.length() > 0) ? " " : "");
      if (prefix != null) {
        message.append(prefix);
      }
      message.append(value);
    }
  }

  private Object callIfPossible(Callable<Object> call) {
    try {
      return call.call();
    }
    catch (Exception ex) {
      return null;
    }
  }

}
