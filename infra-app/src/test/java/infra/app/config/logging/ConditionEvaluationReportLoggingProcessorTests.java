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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.Condition;
import infra.context.condition.ConditionEvaluationReport;
import infra.context.condition.ConditionOutcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionEvaluationReportLoggingProcessor}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/5 22:16
 */
@ExtendWith(OutputCaptureExtension.class)
class ConditionEvaluationReportLoggingProcessorTests {

  @Test
  void logsDebugOnProcessAheadOfTime(CapturedOutput output) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    ConditionEvaluationReport.get(beanFactory)
            .recordConditionEvaluation("test", mock(Condition.class), ConditionOutcome.match());
    ConditionEvaluationReportLoggingProcessor processor = new ConditionEvaluationReportLoggingProcessor();
    processor.processAheadOfTime(beanFactory);
    assertThat(output).doesNotContain("CONDITIONS EVALUATION REPORT");
    withDebugLogging(() -> processor.processAheadOfTime(beanFactory));
    assertThat(output).contains("CONDITIONS EVALUATION REPORT");
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