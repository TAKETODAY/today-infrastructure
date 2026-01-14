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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import infra.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import infra.app.Application;
import infra.app.ApplicationArguments;
import infra.app.context.event.ApplicationFailedEvent;
import infra.app.logging.LogLevel;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.condition.ConditionEvaluationReport;
import infra.http.converter.config.HttpMessageConvertersAutoConfiguration;
import infra.webmvc.config.WebMvcAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/5 21:57
 */
@ExtendWith(OutputCaptureExtension.class)
class ConditionEvaluationReportLoggingListenerTests {

  private final ConditionEvaluationReportLoggingListener initializer = ConditionEvaluationReportLoggingListener.forLoggingLevel(LogLevel.INFO);

  @Test
  void logsDebugOnContextRefresh(CapturedOutput output) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    this.initializer.initialize(context);
    context.register(Config.class);
    withDebugLogging(context::refresh);
    assertThat(output).contains("CONDITIONS EVALUATION REPORT");
  }

  @Test
  @Disabled
  void logsDebugOnApplicationFailedEvent(CapturedOutput output) {
    withDebugLogging(() -> {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      var initializer = ConditionEvaluationReportLoggingListener.forLoggingLevel(LogLevel.DEBUG);
      initializer.initialize(context);
      context.register(ErrorConfig.class);
      assertThatException().isThrownBy(context::refresh)
              .satisfies((ex) -> context.publishEvent(new ApplicationFailedEvent(new Application(), new ApplicationArguments(), context, ex)));

      assertThat(output).contains("CONDITIONS EVALUATION REPORT");
    });
  }

  @Test
  @Disabled
  void logsInfoGuidanceToEnableDebugLoggingOnApplicationFailedEvent(CapturedOutput output) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    var initializer = ConditionEvaluationReportLoggingListener.forLoggingLevel(LogLevel.INFO);

    initializer.initialize(context);
    context.register(ErrorConfig.class);
    assertThatException().isThrownBy(context::refresh)
            .satisfies((ex) -> withInfoLogging(() ->
                    context.publishEvent(new ApplicationFailedEvent(new Application(), new ApplicationArguments(), context, ex))));
    assertThat(output).doesNotContain("CONDITIONS EVALUATION REPORT")
            .contains("re-run your application with 'debug' enabled");
  }

  @Test
  void canBeUsedInApplicationContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(Config.class);
    new ConditionEvaluationReportLoggingListener().initialize(context);
    context.refresh();
    assertThatThrownBy(() -> context.getBean(ConditionEvaluationReport.class))
            .isInstanceOf(NoSuchBeanDefinitionException.class);
  }

  private void withDebugLogging(Runnable runnable) {
    withLoggingLevel(Level.DEBUG, runnable);
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

  @Configuration(proxyBeanMethods = false)
  @Import(WebMvcAutoConfiguration.class)
  static class ErrorConfig {

    @Bean
    String iBreak() {
      throw new RuntimeException();
    }

  }

}