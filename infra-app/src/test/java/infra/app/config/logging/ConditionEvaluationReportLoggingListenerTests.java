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
import infra.app.Application;
import infra.app.ApplicationArguments;
import infra.context.config.PropertyPlaceholderAutoConfiguration;
import infra.app.context.event.ApplicationFailedEvent;
import infra.app.logging.LogLevel;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.condition.ConditionEvaluationReport;
import infra.http.converter.config.HttpMessageConvertersAutoConfiguration;
import infra.test.stdio.CapturedOutput;
import infra.test.stdio.OutputCaptureExtension;
import infra.web.config.WebMvcAutoConfiguration;

import static infra.app.config.logging.ConditionEvaluationReportLoggingListener.forLoggingLevel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/5 21:57
 */
@ExtendWith(OutputCaptureExtension.class)
class ConditionEvaluationReportLoggingListenerTests {

  private final ConditionEvaluationReportLoggingListener initializer = forLoggingLevel(LogLevel.INFO);

  @Test
  void logsDebugOnContextRefresh(CapturedOutput output) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    this.initializer.initialize(context);
    context.register(Config.class);
    withDebugLogging(context::refresh);
    assertThat(output).contains("CONDITIONS EVALUATION REPORT");
  }

  @Test
  void logsDebugOnApplicationFailedEvent(CapturedOutput output) {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      forLoggingLevel(LogLevel.DEBUG).initialize(context);
      withDebugLogging(() -> context.publishEvent(failedEvent(context)));
      assertThat(output).contains("CONDITIONS EVALUATION REPORT");
    }
  }

  @Test
  void logsInfoGuidanceToEnableDebugLoggingOnApplicationFailedEvent(CapturedOutput output) {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      forLoggingLevel(LogLevel.DEBUG).initialize(context);
      withInfoLogging(() -> context.publishEvent(failedEvent(context)));
      assertThat(output).doesNotContain("CONDITIONS EVALUATION REPORT")
              .contains("re-run your application with 'debug' enabled");
    }
  }

  @Test
  void doesNotReportChildContextRefresh(CapturedOutput output) {
    try (var parent = new AnnotationConfigApplicationContext(Config.class);
            var child = new AnnotationConfigApplicationContext()) {
      child.setParent(parent);
      this.initializer.initialize(parent);
      withInfoLogging(child::refresh);
      assertThat(output).doesNotContain("CONDITIONS EVALUATION REPORT");
    }
  }

  @Test
  void doesNotReportFailureFromAnotherContext(CapturedOutput output) {
    try (var context = new AnnotationConfigApplicationContext(Config.class);
            var other = new AnnotationConfigApplicationContext()) {
      this.initializer.initialize(context);
      withInfoLogging(() -> context.publishEvent(failedEvent(other)));
      assertThat(output).doesNotContain("CONDITIONS EVALUATION REPORT");
    }
  }

  @Test
  void rejectsUnsupportedLoggingLevel() {
    assertThatIllegalArgumentException().isThrownBy(() -> forLoggingLevel(LogLevel.WARN))
            .withMessage("LogLevel must be INFO or DEBUG");
  }

  @Test
  void canBeUsedInApplicationContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(Config.class);
    new ConditionEvaluationReportLoggingListener(LogLevel.DEBUG, true).initialize(context);
    context.refresh();
    assertThatThrownBy(() -> context.getBean(ConditionEvaluationReport.class))
            .isInstanceOf(NoSuchBeanDefinitionException.class);
  }

  private void withDebugLogging(Runnable runnable) {
    withLoggingLevel(Level.DEBUG, runnable);
  }

  private ApplicationFailedEvent failedEvent(AnnotationConfigApplicationContext context) {
    return new ApplicationFailedEvent(new Application(), new ApplicationArguments(), context,
            new IllegalStateException("Startup failed"));
  }

  private void withInfoLogging(Runnable runnable) {
    withLoggingLevel(Level.INFO, runnable);
  }

  private void withLoggingLevel(Level logLevel, Runnable runnable) {
    Logger logger = ((LoggerContext) LoggerFactory.getILoggerFactory())
            .getLogger(ConditionEvaluationReportLogger.class);
    Level currentLevel = logger.getLevel();
    logger.setLevel(logLevel);
    try {
      runnable.run();
    }
    finally {
      logger.setLevel(currentLevel);
    }
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
          PropertyPlaceholderAutoConfiguration.class })
  static class Config {

  }

}
