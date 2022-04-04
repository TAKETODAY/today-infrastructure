/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.annotation.config.AutoConfigurationImportEvent;
import cn.taketoday.context.annotation.config.AutoConfigurationImportListener;
import cn.taketoday.lang.TodayStrategies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionEvaluationReportAutoConfigurationImportListener}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class ConditionEvaluationReportAutoConfigurationImportListenerTests {

  private ConditionEvaluationReportAutoConfigurationImportListener listener;

  private final ConfigurableBeanFactory beanFactory = new StandardBeanFactory();

  @BeforeEach
  void setup() {
    this.listener = new ConditionEvaluationReportAutoConfigurationImportListener();
    this.listener.setBeanFactory(this.beanFactory);
  }

  @Test
  void shouldBeInSpringFactories() {
    List<AutoConfigurationImportListener> factories = TodayStrategies.get(AutoConfigurationImportListener.class);
    assertThat(factories)
            .hasAtLeastOneElementOfType(ConditionEvaluationReportAutoConfigurationImportListener.class);
  }

  @Test
  void onAutoConfigurationImportEventShouldRecordCandidates() {
    List<String> candidateConfigurations = Collections.singletonList("Test");
    Set<String> exclusions = Collections.emptySet();
    AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, candidateConfigurations,
            exclusions);
    this.listener.onAutoConfigurationImportEvent(event);
    ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
    assertThat(report.getUnconditionalClasses()).containsExactlyElementsOf(candidateConfigurations);
  }

  @Test
  void onAutoConfigurationImportEventShouldRecordExclusions() {
    List<String> candidateConfigurations = Collections.emptyList();
    Set<String> exclusions = Collections.singleton("Test");
    AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, candidateConfigurations,
            exclusions);
    this.listener.onAutoConfigurationImportEvent(event);
    ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
    assertThat(report.getExclusions()).containsExactlyElementsOf(exclusions);
  }

  @Test
  void onAutoConfigurationImportEventShouldApplyExclusionsGlobally() {
    AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, Arrays.asList("First", "Second"),
            Collections.emptySet());
    this.listener.onAutoConfigurationImportEvent(event);
    AutoConfigurationImportEvent anotherEvent = new AutoConfigurationImportEvent(this, Collections.emptyList(),
            Collections.singleton("First"));
    this.listener.onAutoConfigurationImportEvent(anotherEvent);
    ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
    assertThat(report.getUnconditionalClasses()).containsExactly("Second");
    assertThat(report.getExclusions()).containsExactly("First");
  }

  @Test
  void onAutoConfigurationImportEventShouldApplyExclusionsGloballyWhenExclusionIsAlreadyApplied() {
    AutoConfigurationImportEvent excludeEvent = new AutoConfigurationImportEvent(this, Collections.emptyList(),
            Collections.singleton("First"));
    this.listener.onAutoConfigurationImportEvent(excludeEvent);
    AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, Arrays.asList("First", "Second"),
            Collections.emptySet());
    this.listener.onAutoConfigurationImportEvent(event);
    ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
    assertThat(report.getUnconditionalClasses()).containsExactly("Second");
    assertThat(report.getExclusions()).containsExactly("First");
  }

}
