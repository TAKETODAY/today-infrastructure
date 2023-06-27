/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.condition;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.annotation.config.AutoConfigurationImportEvent;
import cn.taketoday.context.annotation.config.AutoConfigurationImportListener;

/**
 * {@link AutoConfigurationImportListener} to record results with the
 * {@link ConditionEvaluationReport}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 23:24
 */
class ConditionEvaluationReportAutoConfigurationImportListener
        implements AutoConfigurationImportListener {

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
