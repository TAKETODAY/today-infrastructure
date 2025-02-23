/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.stream.Collectors;

import infra.app.ApplicationType;
import infra.app.builder.ApplicationBuilder;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.StandardEnvironment;
import infra.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConditionalOnBooleanProperty @ConditionalOnBooleanProperty}.
 *
 * @author Phillip Webb
 */
class ConditionalOnBooleanPropertyTests {

  private ConfigurableApplicationContext context;

  private final ConfigurableEnvironment environment = new StandardEnvironment();

  @AfterEach
  void tearDown() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void defaultsWhenTrue() {
    load(Defaults.class, "test=true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void defaultsWhenFalse() {
    load(Defaults.class, "test=false");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void defaultsWhenMissing() {
    load(Defaults.class);
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void havingValueTrueMatchIfMissingFalseWhenTrue() {
    load(HavingValueTrueMatchIfMissingFalse.class, "test=true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void havingValueTrueMatchIfMissingFalseWhenFalse() {
    load(HavingValueTrueMatchIfMissingFalse.class, "test=false");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void havingValueTrueMatchIfMissingFalseWhenMissing() {
    load(HavingValueTrueMatchIfMissingFalse.class);
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void havingValueTrueMatchIfMissingTrueWhenTrue() {
    load(HavingValueTrueMatchIfMissingTrue.class, "test=true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void havingValueTrueMatchIfMissingTrueWhenFalse() {
    load(HavingValueTrueMatchIfMissingTrue.class, "test=false");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void havingValueTrueMatchIfMissingTrueWhenMissing() {
    load(HavingValueTrueMatchIfMissingTrue.class);
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void havingValueFalseMatchIfMissingFalseWhenTrue() {
    load(HavingValueFalseMatchIfMissingFalse.class, "test=true");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void havingValueFalseMatchIfMissingFalseWhenFalse() {
    load(HavingValueFalseMatchIfMissingFalse.class, "test=false");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void havingValueFalseMatchIfMissingFalseWhenMissing() {
    load(HavingValueFalseMatchIfMissingFalse.class);
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void havingValueFalseMatchIfMissingTrueWhenTrue() {
    load(HavingValueFalseMatchIfMissingTrue.class, "test=true");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void havingValueFalseMatchIfMissingTrueWhenFalse() {
    load(HavingValueFalseMatchIfMissingTrue.class, "test=false");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void havingValueFalseMatchIfMissingTrueWhenMissing() {
    load(HavingValueFalseMatchIfMissingTrue.class);
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void withPrefix() {
    load(HavingValueFalseMatchIfMissingTrue.class, "foo.test=true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void nameOrValueMustBeSpecified() {
    assertThatIllegalStateException().isThrownBy(() -> load(NoNameOrValueAttribute.class, "some.property"))
            .satisfies(causeMessageContaining(
                    "The name or value attribute of @ConditionalOnBooleanProperty must be specified"));
  }

  @Test
  void nameAndValueMustNotBeSpecified() {
    assertThatIllegalStateException().isThrownBy(() -> load(NameAndValueAttribute.class, "some.property"))
            .satisfies(causeMessageContaining(
                    "The name and value attributes of @ConditionalOnBooleanProperty are exclusive"));
  }

  @Test
  void conditionReportWhenMatched() {
    load(Defaults.class, "test=true");
    assertThat(this.context.containsBean("foo")).isTrue();
    assertThat(getConditionEvaluationReport()).contains("@ConditionalOnBooleanProperty (test=true) matched");
  }

  @Test
  void conditionReportWhenDoesNotMatch() {
    load(Defaults.class, "test=false");
    assertThat(this.context.containsBean("foo")).isFalse();
    assertThat(getConditionEvaluationReport())
            .contains("@ConditionalOnBooleanProperty (test=true) found different value in property 'test'");
  }

  @Test
  void repeatablePropertiesConditionReportWhenMatched() {
    load(RepeatablePropertiesRequiredConfiguration.class, "property1=true", "property2=true");
    assertThat(this.context.containsBean("foo")).isTrue();
    String report = getConditionEvaluationReport();
    assertThat(report).contains("@ConditionalOnBooleanProperty (property1=true) matched");
    assertThat(report).contains("@ConditionalOnBooleanProperty (property2=true) matched");
  }

  @Test
  void repeatablePropertiesConditionReportWhenDoesNotMatch() {
    load(RepeatablePropertiesRequiredConfiguration.class, "property1=true");
    assertThat(getConditionEvaluationReport())
            .contains("@ConditionalOnBooleanProperty (property2=true) did not find property 'property2'");
  }

  private <T extends Exception> Consumer<T> causeMessageContaining(String message) {
    return (ex) -> assertThat(ex.getCause()).hasMessageContaining(message);
  }

  private String getConditionEvaluationReport() {
    return ConditionEvaluationReport.get(this.context.getBeanFactory())
            .getConditionAndOutcomesBySource()
            .values()
            .stream()
            .flatMap(ConditionAndOutcomes::stream)
            .map(Object::toString)
            .collect(Collectors.joining("\n"));
  }

  private void load(Class<?> config, String... environment) {
    TestPropertyValues.of(environment).applyTo(this.environment);
    this.context = new ApplicationBuilder(config)
            .environment(this.environment)
            .type(ApplicationType.NORMAL)
            .run();
  }

  abstract static class BeanConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBooleanProperty("test")
  static class Defaults extends BeanConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBooleanProperty(name = "test", havingValue = true, matchIfMissing = false)
  static class HavingValueTrueMatchIfMissingFalse extends BeanConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBooleanProperty(name = "test", havingValue = true, matchIfMissing = true)
  static class HavingValueTrueMatchIfMissingTrue extends BeanConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBooleanProperty(name = "test", havingValue = false, matchIfMissing = false)
  static class HavingValueFalseMatchIfMissingFalse extends BeanConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBooleanProperty(name = "test", havingValue = false, matchIfMissing = true)
  static class HavingValueFalseMatchIfMissingTrue extends BeanConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBooleanProperty(prefix = "foo", name = "test")
  static class WithPrefix extends BeanConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBooleanProperty
  static class NoNameOrValueAttribute {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBooleanProperty(value = "x", name = "y")
  static class NameAndValueAttribute {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBooleanProperty("property1")
  @ConditionalOnBooleanProperty("property2")
  static class RepeatablePropertiesRequiredConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

}
