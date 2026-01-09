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

import infra.app.context.event.ApplicationFailedEvent;
import infra.app.logging.LogLevel;
import infra.context.ApplicationContextInitializer;
import infra.context.ApplicationEvent;
import infra.context.ConfigurableApplicationContext;
import infra.context.condition.ConditionEvaluationReport;
import infra.context.event.ContextRefreshedEvent;
import infra.context.event.SmartApplicationListener;
import infra.lang.Assert;

/**
 * {@link ApplicationContextInitializer} that writes the {@link ConditionEvaluationReport}
 * to the log. Reports are logged at the {@link LogLevel#DEBUG DEBUG} level. A crash
 * report triggers an info output suggesting the user runs again with debug enabled to
 * display the report.
 * <p>
 * This initializer is not intended to be shared across multiple application context
 * instances.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ConditionEvaluationReportLoggingListener implements ApplicationContextInitializer {

  private final LogLevel logLevelForReport;

  public ConditionEvaluationReportLoggingListener() {
    this(LogLevel.DEBUG);
  }

  private ConditionEvaluationReportLoggingListener(LogLevel logLevelForReport) {
    Assert.isTrue(isInfoOrDebug(logLevelForReport), "LogLevel must be INFO or DEBUG");
    this.logLevelForReport = logLevelForReport;
  }

  private boolean isInfoOrDebug(LogLevel logLevelForReport) {
    return LogLevel.INFO.equals(logLevelForReport) || LogLevel.DEBUG.equals(logLevelForReport);
  }

  /**
   * Static factory method that creates a
   * {@link ConditionEvaluationReportLoggingListener} which logs the report at the
   * specified log level.
   *
   * @param logLevelForReport the log level to log the report at
   * @return a {@link ConditionEvaluationReportLoggingListener} instance.
   */
  public static ConditionEvaluationReportLoggingListener forLoggingLevel(LogLevel logLevelForReport) {
    return new ConditionEvaluationReportLoggingListener(logLevelForReport);
  }

  @Override
  public void initialize(ConfigurableApplicationContext context) {
    context.addApplicationListener(new Listener(context));
  }

  private final class Listener implements SmartApplicationListener {

    private final ConfigurableApplicationContext context;

    private Listener(ConfigurableApplicationContext context) {
      this.context = context;
    }

    private ConditionEvaluationReport getReport() {
      return ConditionEvaluationReport.get(context.getBeanFactory());
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
      return ContextRefreshedEvent.class.isAssignableFrom(eventType)
              || ApplicationFailedEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
      ConditionEvaluationReport report = getReport();
      var logger = new ConditionEvaluationReportLogger(logLevelForReport, report);
      if (event instanceof ContextRefreshedEvent contextRefreshedEvent) {
        if (contextRefreshedEvent.getApplicationContext() == this.context) {
          logger.logReport(false);
        }
      }
      else if (event instanceof ApplicationFailedEvent applicationFailedEvent
              && applicationFailedEvent.getApplicationContext() == this.context) {
        logger.logReport(true);
      }

      context.removeApplicationListener(this);
      context.getBeanFactory().removeSingleton(ConditionEvaluationReport.BEAN_NAME);
    }

  }

}
