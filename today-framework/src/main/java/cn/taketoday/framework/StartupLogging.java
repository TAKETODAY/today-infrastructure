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

package cn.taketoday.framework;

import java.net.InetAddress;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.core.ApplicationHome;
import cn.taketoday.core.ApplicationPid;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Version;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Logs application information on startup.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 11:09
 */
final class StartupLogging {

  private static final long HOST_NAME_RESOLVE_THRESHOLD = 200;

  @Nullable
  private final Class<?> sourceClass;

  StartupLogging(@Nullable Class<?> sourceClass) {
    this.sourceClass = sourceClass;
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
    appendVersion(message, this.sourceClass);
    appendJavaVersion(message);
    appendOn(message);
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

  private void appendVersion(StringBuilder message, @Nullable Class<?> source) {
    if (source != null) {
      Package sourcePkg = source.getPackage();
      if (sourcePkg != null) {
        append(message, "v", sourcePkg.getImplementationVersion());
      }
    }
  }

  private void appendOn(StringBuilder message) {
    long startTime = System.currentTimeMillis();
    try {
      String hostName = InetAddress.getLocalHost().getHostName();
      append(message, "on ", hostName);
    }
    catch (Throwable ignored) { }

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
    append(message, "with PID ", new ApplicationPid().toString());
  }

  private void appendContext(StringBuilder message) {
    StringBuilder context = new StringBuilder();
    ApplicationHome home = new ApplicationHome(this.sourceClass);
    if (home.getSource() != null) {
      context.append(home.getSource().getAbsolutePath());
    }

    append(context, "started by ", System.getProperty("user.name"));
    append(context, "in ", System.getProperty("user.dir"));
    if (context.length() > 0) {
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
      message.append((message.length() > 0) ? " " : "");
      if (prefix != null) {
        message.append(prefix);
      }
      message.append(value);
    }
  }

}
