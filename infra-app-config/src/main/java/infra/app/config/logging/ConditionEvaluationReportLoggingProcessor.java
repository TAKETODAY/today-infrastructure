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

import org.jspecify.annotations.Nullable;

import infra.app.logging.LogLevel;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.condition.ConditionEvaluationReport;

/**
 * {@link BeanFactoryInitializationAotProcessor} that logs the
 * {@link ConditionEvaluationReport} during ahead-of-time processing.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConditionEvaluationReportLoggingProcessor implements BeanFactoryInitializationAotProcessor {

  @Nullable
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
