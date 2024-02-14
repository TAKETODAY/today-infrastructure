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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.condition.ConditionEvaluationReport;
import cn.taketoday.context.condition.ConditionOutcome;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionEvaluationReportLoggingProcessor}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/5 22:16
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