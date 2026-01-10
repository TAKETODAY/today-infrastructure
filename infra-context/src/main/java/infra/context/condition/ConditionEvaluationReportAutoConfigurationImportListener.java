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

package infra.context.condition;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.annotation.config.AutoConfigurationImportEvent;
import infra.context.annotation.config.AutoConfigurationImportListener;

/**
 * {@link AutoConfigurationImportListener} to record results with the
 * {@link ConditionEvaluationReport}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 23:24
 */
class ConditionEvaluationReportAutoConfigurationImportListener implements AutoConfigurationImportListener {

  private final ConfigurableBeanFactory beanFactory;

  ConditionEvaluationReportAutoConfigurationImportListener(ConfigurableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void onAutoConfigurationImportEvent(AutoConfigurationImportEvent event) {
    ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
    report.recordEvaluationCandidates(event.getCandidateConfigurations());
    report.recordExclusions(event.getExclusions());
  }

}
