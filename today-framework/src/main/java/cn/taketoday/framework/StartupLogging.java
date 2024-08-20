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

package cn.taketoday.framework;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.core.ApplicationHome;
import cn.taketoday.core.env.Environment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Version;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Logs application information on startup.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 11:09
 */
final class StartupLogging {

  @Nullable
  private final Class<?> sourceClass;

  private final Environment environment;

  StartupLogging(@Nullable Class<?> sourceClass, Environment environment) {
    this.sourceClass = sourceClass;
    this.environment = environment;
  }

  void logStarting(Logger applicationLog) {
    Assert.notNull(applicationLog, "Logger is required");

    if (applicationLog.isInfoEnabled()) {
      applicationLog.info(getStartingMessage());
    }
    if (applicationLog.isDebugEnabled()) {
      applicationLog.debug("Running with today-framework {}", Version.instance);
    }
  }

  void logStarted(Logger applicationLog, Application.Startup startup) {
    if (applicationLog.isInfoEnabled()) {
      applicationLog.info(getStartedMessage(startup));
    }
  }

  private CharSequence getStartingMessage() {
    StringBuilder message = new StringBuilder();
    message.append("Starting");
    appendAotMode(message);
    appendApplicationName(message);
    appendApplicationVersion(message);
    appendJavaVersion(message);
    appendPid(message);
    appendContext(message);
    return message;
  }

  private CharSequence getStartedMessage(Application.Startup startup) {
    StringBuilder message = new StringBuilder();
    message.append(startup.action());
    appendApplicationName(message);
    message.append(" in ");
    message.append(startup.timeTakenToStarted().toMillis() / 1000.0);
    message.append(" seconds");
    Long uptimeMs = startup.processUptime();
    if (uptimeMs != null) {
      double uptime = uptimeMs / 1000.0;
      message.append(" (process running for ").append(uptime).append(")");
    }
    return message;
  }

  private void appendAotMode(StringBuilder message) {
    if (AotDetector.useGeneratedArtifacts()) {
      append(message, null, "AOT-processed");
    }
  }

  private void appendApplicationName(StringBuilder message) {
    String name = sourceClass != null ? ClassUtils.getShortName(this.sourceClass) : "application";
    append(message, null, name);
  }

  private void appendApplicationVersion(StringBuilder message) {
    append(message, "v", environment.getProperty("app.version"));
  }

  private void appendPid(StringBuilder message) {
    append(message, "with PID ", environment.getProperty("app.pid"));
  }

  private void appendContext(StringBuilder message) {
    StringBuilder context = new StringBuilder();
    ApplicationHome home = new ApplicationHome(this.sourceClass);
    if (home.getSource() != null) {
      context.append(home.getSource().getAbsolutePath());
    }

    append(context, "started by ", System.getProperty("user.name"));
    append(context, "in ", System.getProperty("user.dir"));
    if (!context.isEmpty()) {
      message.append(" (");
      message.append(context);
      message.append(")");
    }
  }

  private void appendJavaVersion(StringBuilder message) {
    append(message, "using Java ", System.getProperty("java.version"));
  }

  private void append(StringBuilder message, @Nullable String prefix, @Nullable String value) {
    if (StringUtils.hasText(value)) {
      message.append((!message.isEmpty()) ? " " : "");
      if (prefix != null) {
        message.append(prefix);
      }
      message.append(value);
    }
  }

}
