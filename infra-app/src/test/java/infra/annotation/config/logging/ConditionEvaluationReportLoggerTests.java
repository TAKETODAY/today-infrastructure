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

package infra.annotation.config.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import infra.annotation.config.logging.ConditionEvaluationReportLoggingListenerTests.Config;
import infra.app.logging.LogLevel;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.condition.ConditionEvaluationReport;

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