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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.config.AutoConfigurationImportEvent;
import infra.context.annotation.config.AutoConfigurationImportListener;
import infra.lang.TodayStrategies;

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
    this.listener = new ConditionEvaluationReportAutoConfigurationImportListener(beanFactory);
  }

  @Test
  void shouldBeInInfraFactories() {
    List<AutoConfigurationImportListener> factories = TodayStrategies.find(AutoConfigurationImportListener.class);
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
