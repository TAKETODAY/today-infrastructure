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

package infra.context.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.config.AutoConfigurationImportFilter;
import infra.context.annotation.config.AutoConfigurationMetadata;
import infra.lang.TodayStrategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link AutoConfigurationImportFilter} part of {@link OnClassCondition}.
 *
 * @author Phillip Webb
 */
class OnClassConditionAutoConfigurationImportFilterTests {

  private final OnClassCondition filter = new OnClassCondition();

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @BeforeEach
  void setup() {
    this.filter.setBeanClassLoader(getClass().getClassLoader());
    this.filter.setBeanFactory(this.beanFactory);
  }

  @Test
  void shouldBeRegistered() {
    assertThat(TodayStrategies.find(AutoConfigurationImportFilter.class))
            .hasAtLeastOneElementOfType(OnClassCondition.class);
  }

  @Test
  void matchShouldMatchClasses() {
    String[] autoConfigurationClasses = new String[] { "test.match", "test.nomatch" };
    boolean[] result = this.filter.match(autoConfigurationClasses, getAutoConfigurationMetadata());
    assertThat(result).containsExactly(true, false);
  }

  @Test
  void matchShouldRecordOutcome() {
    String[] autoConfigurationClasses = new String[] { "test.match", "test.nomatch" };
    this.filter.match(autoConfigurationClasses, getAutoConfigurationMetadata());
    ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
    assertThat(report.getConditionAndOutcomesBySource()).hasSize(1).containsKey("test.nomatch");
  }

  private AutoConfigurationMetadata getAutoConfigurationMetadata() {
    AutoConfigurationMetadata metadata = mock(AutoConfigurationMetadata.class);
    given(metadata.wasProcessed("test.match")).willReturn(true);
    given(metadata.get("test.match", "ConditionalOnClass")).willReturn("java.io.InputStream");
    given(metadata.wasProcessed("test.nomatch")).willReturn(true);
    given(metadata.get("test.nomatch", "ConditionalOnClass")).willReturn("java.io.DoesNotExist");
    return metadata;
  }

}
