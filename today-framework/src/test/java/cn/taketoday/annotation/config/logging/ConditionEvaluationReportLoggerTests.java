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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import cn.taketoday.annotation.config.logging.ConditionEvaluationReportLoggingListenerTests.Config;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.condition.ConditionEvaluationReport;
import cn.taketoday.framework.logging.LogLevel;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/5 21:56
 */
@ExtendWith(OutputCaptureExtension.class)
class ConditionEvaluationReportLoggerTests {

  @Test
  void noErrorIfNotInitialized(CapturedOutput output) {
    new ConditionEvaluationReportLogger(LogLevel.INFO, null).logReport(true);
    assertThat(output).contains("Unable to provide the condition evaluation report");
  }

  @Test
  void supportsOnlyInfoAndDebugLogLevels() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ConditionEvaluationReportLogger(LogLevel.TRACE, null))
            .withMessageContaining("LogLevel must be INFO or DEBUG");
  }

  @Test
  void loggerWithInfoLevelShouldLogAtInfo(CapturedOutput output) {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      ConditionEvaluationReportLogger logger = new ConditionEvaluationReportLogger(LogLevel.INFO,
              ConditionEvaluationReport.get(context.getBeanFactory()));
      context.register(Config.class);
      context.refresh();
      logger.logReport(false);
      assertThat(output).contains("CONDITIONS EVALUATION REPORT");
    }
  }

  @Test
  void loggerWithDebugLevelShouldLogAtDebug(CapturedOutput output) {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      ConditionEvaluationReportLogger logger = new ConditionEvaluationReportLogger(LogLevel.DEBUG,
              ConditionEvaluationReport.get(context.getBeanFactory()));
      context.register(Config.class);
      context.refresh();
      logger.logReport(false);
      assertThat(output).doesNotContain("CONDITIONS EVALUATION REPORT");
      withDebugLogging(() -> logger.logReport(false));
      assertThat(output).contains("CONDITIONS EVALUATION REPORT");
    }
  }

  @Test
  void logsInfoOnErrorIfDebugDisabled(CapturedOutput output) {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      ConditionEvaluationReportLogger logger = new ConditionEvaluationReportLogger(LogLevel.DEBUG,
              ConditionEvaluationReport.get(context.getBeanFactory()));
      context.register(Config.class);
      context.refresh();
      logger.logReport(true);
      assertThat(output).contains("Error starting ApplicationContext. To display the condition "
              + "evaluation report re-run your application with 'debug' enabled.");
    }
  }

  @Test
  void logsOutput(CapturedOutput output) {
    withDebugLogging(() -> {
      try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
        ConditionEvaluationReportLogger logger = new ConditionEvaluationReportLogger(LogLevel.DEBUG,
                ConditionEvaluationReport.get(context.getBeanFactory()));
        context.register(Config.class);
        ConditionEvaluationReport.get(context.getBeanFactory()).recordExclusions(List.of("com.foo.Bar"));
        context.refresh();

        logger.logReport(false);
        assertThat(output).contains("not a netty web application and not a reactive web application");
      }
    });
  }

  private void withDebugLogging(Runnable runnable) {
    Logger logger = ((LoggerContext) LoggerFactory.getILoggerFactory())
            .getLogger(ConditionEvaluationReportLogger.class);
    Level currentLevel = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    try {
      runnable.run();
    }
    finally {
      logger.setLevel(currentLevel);
    }
  }

}