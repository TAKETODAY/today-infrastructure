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

import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.condition.ConditionEvaluationReport;
import cn.taketoday.framework.logging.LogLevel;

/**
 * {@link BeanFactoryInitializationAotProcessor} that logs the
 * {@link ConditionEvaluationReport} during ahead-of-time processing.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConditionEvaluationReportLoggingProcessor implements BeanFactoryInitializationAotProcessor {

  @Override
  public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
    logConditionEvaluationReport(beanFactory);
    return null;
  }

  private void logConditionEvaluationReport(ConfigurableBeanFactory beanFactory) {
    new ConditionEvaluationReportLogger(LogLevel.DEBUG, ConditionEvaluationReport.get(beanFactory))
            .logReport(false);
  }

}
