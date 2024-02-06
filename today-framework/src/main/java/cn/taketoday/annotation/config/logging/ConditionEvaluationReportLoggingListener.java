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

import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.condition.ConditionEvaluationReport;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.context.event.SmartApplicationListener;
import cn.taketoday.framework.context.event.ApplicationFailedEvent;
import cn.taketoday.framework.logging.LogLevel;
import cn.taketoday.lang.Assert;

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
