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

package cn.taketoday.annotation.config.logging;

import java.util.function.Supplier;

import cn.taketoday.context.condition.ConditionEvaluationReport;
import cn.taketoday.context.condition.ConditionEvaluationReportMessage;
import cn.taketoday.framework.logging.LogLevel;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

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

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Supplier<ConditionEvaluationReport> reportSupplier;

  private final LogLevel logLevel;

  ConditionEvaluationReportLogger(LogLevel logLevel, Supplier<ConditionEvaluationReport> reportSupplier) {
    Assert.isTrue(isInfoOrDebug(logLevel), "LogLevel must be INFO or DEBUG");
    this.logLevel = logLevel;
    this.reportSupplier = reportSupplier;
  }

  private boolean isInfoOrDebug(LogLevel logLevelForReport) {
    return LogLevel.INFO.equals(logLevelForReport) || LogLevel.DEBUG.equals(logLevelForReport);
  }

  void logReport(boolean isCrashReport) {
    ConditionEvaluationReport report = this.reportSupplier.get();
    if (report == null) {
      this.logger.info("Unable to provide the condition evaluation report");
      return;
    }
    if (!report.getConditionAndOutcomesBySource().isEmpty()) {
      if (this.logLevel.equals(LogLevel.INFO)) {
        if (this.logger.isInfoEnabled()) {
          this.logger.info(new ConditionEvaluationReportMessage(report));
          report.clear();
        }
        else if (isCrashReport) {
          logMessage("info");
        }
      }
      else {
        if (this.logger.isDebugEnabled()) {
          this.logger.debug(new ConditionEvaluationReportMessage(report));
          report.clear();
        }
        else if (isCrashReport) {
          logMessage("debug");
        }
      }
    }
  }

  private void logMessage(String logLevel) {
    this.logger.info(String.format("%n%nError starting ApplicationContext. To display the "
            + "condition evaluation report re-run your application with '%s' enabled.", logLevel));
  }

}
