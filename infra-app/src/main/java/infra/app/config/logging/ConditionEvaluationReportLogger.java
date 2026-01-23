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

package infra.app.config.logging;

import org.jspecify.annotations.Nullable;

import infra.app.logging.LogLevel;
import infra.context.condition.ConditionEvaluationReport;
import infra.context.condition.ConditionEvaluationReportMessage;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Logs the {@link ConditionEvaluationReport}.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class ConditionEvaluationReportLogger {

  private final LogLevel logLevel;

  @Nullable
  private final ConditionEvaluationReport report;

  ConditionEvaluationReportLogger(LogLevel logLevel, @Nullable ConditionEvaluationReport report) {
    Assert.isTrue(isInfoOrDebug(logLevel), "LogLevel must be INFO or DEBUG");
    this.logLevel = logLevel;
    this.report = report;
  }

  private boolean isInfoOrDebug(LogLevel logLevelForReport) {
    return LogLevel.INFO.equals(logLevelForReport) || LogLevel.DEBUG.equals(logLevelForReport);
  }

  void logReport(boolean isCrashReport) {
    Logger logger = LoggerFactory.getLogger(getClass());
    if (report == null) {
      logger.info("Unable to provide the condition evaluation report");
      return;
    }
    if (!report.getConditionAndOutcomesBySource().isEmpty()) {
      if (this.logLevel.equals(LogLevel.INFO)) {
        if (logger.isInfoEnabled()) {
          logger.info(new ConditionEvaluationReportMessage(report));
          report.clear();
        }
        else if (isCrashReport) {
          logMessage(logger, "info");
        }
      }
      else {
        if (logger.isDebugEnabled()) {
          logger.debug(new ConditionEvaluationReportMessage(report));
          report.clear();
        }
        else if (isCrashReport) {
          logMessage(logger, "debug");
        }
      }
    }
  }

  private void logMessage(Logger logger, String logLevel) {
    logger.info(String.format("%n%nError starting ApplicationContext. To display the "
            + "condition evaluation report re-run your application with '%s' enabled.", logLevel));
  }

}
