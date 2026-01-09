/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app;

import org.jspecify.annotations.Nullable;

import infra.aot.AotDetector;
import infra.core.ApplicationHome;
import infra.core.env.Environment;
import infra.lang.Assert;
import infra.lang.Version;
import infra.logging.Logger;
import infra.util.ClassUtils;
import infra.util.StringUtils;

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
