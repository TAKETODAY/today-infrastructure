/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Iterator;
import java.util.Map;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ConfigurationCondition;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.condition.ConditionEvaluationReport.ConditionAndOutcome;
import cn.taketoday.context.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.config.WebMvcAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 15:53
 */
@ExtendWith(MockitoExtension.class)
class ConditionEvaluationReportTests {

  private StandardBeanFactory beanFactory;

  private ConditionEvaluationReport report;

  @Mock
  private Condition condition1;

  @Mock
  private Condition condition2;

  @Mock
  private Condition condition3;

  private ConditionOutcome outcome1;

  private ConditionOutcome outcome2;

  private ConditionOutcome outcome3;

  @BeforeEach
  void setup() {
    this.beanFactory = new StandardBeanFactory();
    this.report = ConditionEvaluationReport.get(this.beanFactory);
  }

  @Test
  void get() {
    assertThat(this.report).isNotNull();
    assertThat(this.report).isSameAs(ConditionEvaluationReport.get(this.beanFactory));
  }

  @Test
  void parent() {
    this.beanFactory.setParentBeanFactory(new StandardBeanFactory());
    ConditionEvaluationReport.get((StandardBeanFactory) this.beanFactory.getParentBeanFactory());
    assertThat(this.report).isSameAs(ConditionEvaluationReport.get(this.beanFactory));
    assertThat(this.report).isNotNull();
    assertThat(this.report.getParent()).isNotNull();
    ConditionEvaluationReport.get((StandardBeanFactory) this.beanFactory.getParentBeanFactory());
    assertThat(this.report).isSameAs(ConditionEvaluationReport.get(this.beanFactory));
    assertThat(this.report.getParent()).isSameAs(ConditionEvaluationReport
            .get((StandardBeanFactory) this.beanFactory.getParentBeanFactory()));
  }

  @Test
  void parentBottomUp() {
    this.beanFactory = new StandardBeanFactory(); // NB: overrides setup
    this.beanFactory.setParentBeanFactory(new StandardBeanFactory());
    ConditionEvaluationReport.get((StandardBeanFactory) this.beanFactory.getParentBeanFactory());
    this.report = ConditionEvaluationReport.get(this.beanFactory);
    assertThat(this.report).isNotNull();
    assertThat(this.report).isNotSameAs(this.report.getParent());
    assertThat(this.report.getParent()).isNotNull();
    assertThat(this.report.getParent().getParent()).isNull();
  }

  @Test
  void recordConditionEvaluations() {
    this.outcome1 = new ConditionOutcome(false, "m1");
    this.outcome2 = new ConditionOutcome(false, "m2");
    this.outcome3 = new ConditionOutcome(false, "m3");
    this.report.recordConditionEvaluation("a", this.condition1, this.outcome1);
    this.report.recordConditionEvaluation("a", this.condition2, this.outcome2);
    this.report.recordConditionEvaluation("b", this.condition3, this.outcome3);
    Map<String, ConditionAndOutcomes> map = this.report.getConditionAndOutcomesBySource();
    assertThat(map.size()).isEqualTo(2);
    Iterator<ConditionAndOutcome> iterator = map.get("a").iterator();
    ConditionAndOutcome conditionAndOutcome = iterator.next();
    assertThat(conditionAndOutcome.getCondition()).isEqualTo(this.condition1);
    assertThat(conditionAndOutcome.getOutcome()).isEqualTo(this.outcome1);
    conditionAndOutcome = iterator.next();
    assertThat(conditionAndOutcome.getCondition()).isEqualTo(this.condition2);
    assertThat(conditionAndOutcome.getOutcome()).isEqualTo(this.outcome2);
    assertThat(iterator.hasNext()).isFalse();
    iterator = map.get("b").iterator();
    conditionAndOutcome = iterator.next();
    assertThat(conditionAndOutcome.getCondition()).isEqualTo(this.condition3);
    assertThat(conditionAndOutcome.getOutcome()).isEqualTo(this.outcome3);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void fullMatch() {
    prepareMatches(true, true, true);
    assertThat(this.report.getConditionAndOutcomesBySource().get("a").isFullMatch()).isTrue();
  }

  @Test
  void notFullMatch() {
    prepareMatches(true, false, true);
    assertThat(this.report.getConditionAndOutcomesBySource().get("a").isFullMatch()).isFalse();
  }

  private void prepareMatches(boolean m1, boolean m2, boolean m3) {
    this.outcome1 = new ConditionOutcome(m1, "m1");
    this.outcome2 = new ConditionOutcome(m2, "m2");
    this.outcome3 = new ConditionOutcome(m3, "m3");
    this.report.recordConditionEvaluation("a", this.condition1, this.outcome1);
    this.report.recordConditionEvaluation("a", this.condition2, this.outcome2);
    this.report.recordConditionEvaluation("a", this.condition3, this.outcome3);
  }

  @Test
  @SuppressWarnings("resource")
  void conditionPopulatesReport() {
    ConditionEvaluationReport report = ConditionEvaluationReport.get(
            new AnnotationConfigApplicationContext(Config.class).getBeanFactory());
    assertThat(report.getConditionAndOutcomesBySource().size()).isNotEqualTo(0);
  }

  @Test
  void testDuplicateConditionAndOutcomes() {
    ConditionAndOutcome outcome1 = new ConditionAndOutcome(this.condition1,
            new ConditionOutcome(true, "Message 1"));
    ConditionAndOutcome outcome2 = new ConditionAndOutcome(this.condition2,
            new ConditionOutcome(true, "Message 2"));
    ConditionAndOutcome outcome3 = new ConditionAndOutcome(this.condition3,
            new ConditionOutcome(true, "Message 2"));
    assertThat(outcome1).isEqualTo(outcome1);
    assertThat(outcome1).isNotEqualTo(outcome2);
    assertThat(outcome2).isEqualTo(outcome3);
    ConditionAndOutcomes outcomes = new ConditionAndOutcomes();
    outcomes.add(this.condition1, new ConditionOutcome(true, "Message 1"));
    outcomes.add(this.condition2, new ConditionOutcome(true, "Message 2"));
    outcomes.add(this.condition3, new ConditionOutcome(true, "Message 2"));
    assertThat(outcomes).hasSize(2);
  }

  @Configuration(proxyBeanMethods = false)
  @Import(WebMvcAutoConfiguration.class)
  static class Config {

  }

  @Configuration(proxyBeanMethods = false)
  @Conditional({ ConditionEvaluationReportTests.MatchParseCondition.class,
          ConditionEvaluationReportTests.NoMatchBeanCondition.class })
  static class NegativeOuterConfig {

    @Configuration(proxyBeanMethods = false)
    @Conditional({ ConditionEvaluationReportTests.MatchParseCondition.class })
    static class PositiveInnerConfig {

      @Bean
      String negativeOuterPositiveInnerBean() {
        return "negativeOuterPositiveInnerBean";
      }

    }

  }

  static class TestMatchCondition extends ContextCondition implements ConfigurationCondition {

    private final ConfigurationPhase phase;

    private final boolean match;

    TestMatchCondition(ConfigurationPhase phase, boolean match) {
      this.phase = phase;
      this.match = match;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return this.phase;
    }

    @Override
    public ConditionOutcome getMatchOutcome(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
      return new ConditionOutcome(this.match, ClassUtils.getShortName(getClass()));
    }

  }

  static class MatchParseCondition extends TestMatchCondition {

    MatchParseCondition() {
      super(ConfigurationPhase.PARSE_CONFIGURATION, true);
    }

  }

  static class MatchBeanCondition extends TestMatchCondition {

    MatchBeanCondition() {
      super(ConfigurationPhase.REGISTER_BEAN, true);
    }

  }

  static class NoMatchParseCondition extends TestMatchCondition {

    NoMatchParseCondition() {
      super(ConfigurationPhase.PARSE_CONFIGURATION, false);
    }

  }

  static class NoMatchBeanCondition extends TestMatchCondition {

    NoMatchBeanCondition() {
      super(ConfigurationPhase.REGISTER_BEAN, false);
    }

  }

}
