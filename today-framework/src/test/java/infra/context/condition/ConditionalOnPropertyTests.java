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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Consumer;

import infra.app.ApplicationType;
import infra.app.builder.ApplicationBuilder;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.annotation.AliasFor;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.StandardEnvironment;
import infra.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConditionalOnProperty @ConditionalOnProperty}.
 *
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ConditionalOnPropertyTests {

  private ConfigurableApplicationContext context;

  private ConfigurableEnvironment environment = new StandardEnvironment();

  @AfterEach
  void tearDown() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void allPropertiesAreDefined() {
    load(MultiplePropertiesRequiredConfiguration.class, "property1=value1", "property2=value2");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void notAllPropertiesAreDefined() {
    load(MultiplePropertiesRequiredConfiguration.class, "property1=value1");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void propertyValueEqualsFalse() {
    load(MultiplePropertiesRequiredConfiguration.class, "property1=false", "property2=value2");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void propertyValueEqualsFALSE() {
    load(MultiplePropertiesRequiredConfiguration.class, "property1=FALSE", "property2=value2");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void relaxedName() {
    load(RelaxedPropertiesRequiredConfiguration.class, "spring.theRelaxedProperty=value1");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void prefixWithoutPeriod() {
    load(RelaxedPropertiesRequiredConfigurationWithShortPrefix.class, "spring.property=value1");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
    // Enabled by default
  void enabledIfNotConfiguredOtherwise() {
    load(EnabledIfNotConfiguredOtherwiseConfig.class);
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void enabledIfNotConfiguredOtherwiseWithConfig() {
    load(EnabledIfNotConfiguredOtherwiseConfig.class, "simple.myProperty:false");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void enabledIfNotConfiguredOtherwiseWithConfigDifferentCase() {
    load(EnabledIfNotConfiguredOtherwiseConfig.class, "simple.my-property:FALSE");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
    // Disabled by default
  void disableIfNotConfiguredOtherwise() {
    load(DisabledIfNotConfiguredOtherwiseConfig.class);
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void disableIfNotConfiguredOtherwiseWithConfig() {
    load(DisabledIfNotConfiguredOtherwiseConfig.class, "simple.myProperty:true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void disableIfNotConfiguredOtherwiseWithConfigDifferentCase() {
    load(DisabledIfNotConfiguredOtherwiseConfig.class, "simple.myproperty:TrUe");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void simpleValueIsSet() {
    load(SimpleValueConfig.class, "simple.myProperty:bar");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void caseInsensitive() {
    load(SimpleValueConfig.class, "simple.myProperty:BaR");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void defaultValueIsSet() {
    load(DefaultValueConfig.class, "simple.myProperty:bar");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void defaultValueIsNotSet() {
    load(DefaultValueConfig.class);
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void defaultValueIsSetDifferentValue() {
    load(DefaultValueConfig.class, "simple.myProperty:another");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void prefix() {
    load(PrefixValueConfig.class, "simple.myProperty:bar");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void relaxedEnabledByDefault() {
    load(PrefixValueConfig.class, "simple.myProperty:bar");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void multiValuesAllSet() {
    load(MultiValuesConfig.class, "simple.my-property:bar", "simple.my-another-property:bar");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void multiValuesOnlyOneSet() {
    load(MultiValuesConfig.class, "simple.my-property:bar");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void usingValueAttribute() {
    load(ValueAttribute.class, "some.property");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void nameOrValueMustBeSpecified() {
    assertThatIllegalStateException().isThrownBy(() -> load(NoNameOrValueAttribute.class, "some.property"))
            .satisfies(causeMessageContaining(
                    "The name or value attribute of @ConditionalOnProperty must be specified"));
  }

  @Test
  void nameAndValueMustNotBeSpecified() {
    assertThatIllegalStateException().isThrownBy(() -> load(NameAndValueAttribute.class, "some.property"))
            .satisfies(causeMessageContaining(
                    "The name and value attributes of @ConditionalOnProperty are exclusive"));
  }

  private <T extends Exception> Consumer<T> causeMessageContaining(String message) {
    return (ex) -> assertThat(ex.getCause()).hasMessageContaining(message);
  }

  @Test
  void metaAnnotationConditionMatchesWhenPropertyIsSet() {
    load(MetaAnnotation.class, "my.feature.enabled=true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void metaAnnotationConditionDoesNotMatchWhenPropertyIsNotSet() {
    load(MetaAnnotation.class);
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void metaAndDirectAnnotationConditionDoesNotMatchWhenOnlyDirectPropertyIsSet() {
    load(MetaAnnotationAndDirectAnnotation.class, "my.other.feature.enabled=true");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void metaAndDirectAnnotationConditionDoesNotMatchWhenOnlyMetaPropertyIsSet() {
    load(MetaAnnotationAndDirectAnnotation.class, "my.feature.enabled=true");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void metaAndDirectAnnotationConditionDoesNotMatchWhenNeitherPropertyIsSet() {
    load(MetaAnnotationAndDirectAnnotation.class);
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void metaAndDirectAnnotationConditionMatchesWhenBothPropertiesAreSet() {
    load(MetaAnnotationAndDirectAnnotation.class, "my.feature.enabled=true", "my.other.feature.enabled=true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void metaAnnotationWithAliasConditionMatchesWhenPropertyIsSet() {
    load(MetaAnnotationWithAlias.class, "my.feature.enabled=true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void metaAndDirectAnnotationWithAliasConditionDoesNotMatchWhenOnlyMetaPropertyIsSet() {
    load(MetaAnnotationAndDirectAnnotationWithAlias.class, "my.feature.enabled=true");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void metaAndDirectAnnotationWithAliasConditionDoesNotMatchWhenOnlyDirectPropertyIsSet() {
    load(MetaAnnotationAndDirectAnnotationWithAlias.class, "my.other.feature.enabled=true");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void metaAndDirectAnnotationWithAliasConditionMatchesWhenBothPropertiesAreSet() {
    load(MetaAnnotationAndDirectAnnotationWithAlias.class, "my.feature.enabled=true",
            "my.other.feature.enabled=true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  private void load(Class<?> config, String... environment) {
    TestPropertyValues.of(environment).applyTo(this.environment);
    this.context = new ApplicationBuilder(config).environment(this.environment).type(ApplicationType.NORMAL)
            .run();
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(name = { "property1", "property2" })
  static class MultiplePropertiesRequiredConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = "spring.", name = "the-relaxed-property")
  static class RelaxedPropertiesRequiredConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = "spring", name = "property")
  static class RelaxedPropertiesRequiredConfigurationWithShortPrefix {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  // i.e ${simple.myProperty:true}
  @ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "true", matchIfMissing = true)
  static class EnabledIfNotConfiguredOtherwiseConfig {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  // i.e ${simple.myProperty:false}
  @ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "true")
  static class DisabledIfNotConfiguredOtherwiseConfig {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "bar")
  static class SimpleValueConfig {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(name = "simple.myProperty", havingValue = "bar", matchIfMissing = true)
  static class DefaultValueConfig {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "bar")
  static class PrefixValueConfig {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = "simple", name = { "my-property", "my-another-property" }, havingValue = "bar")
  static class MultiValuesConfig {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty("some.property")
  static class ValueAttribute {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty
  static class NoNameOrValueAttribute {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(value = "x", name = "y")
  static class NameAndValueAttribute {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMyFeature
  static class MetaAnnotation {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMyFeature
  @ConditionalOnProperty(prefix = "my.other.feature", name = "enabled", havingValue = "true")
  static class MetaAnnotationAndDirectAnnotation {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @ConditionalOnProperty(prefix = "my.feature", name = "enabled", havingValue = "true")
  @interface ConditionalOnMyFeature {

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMyFeatureWithAlias("my.feature")
  static class MetaAnnotationWithAlias {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMyFeatureWithAlias("my.feature")
  @ConditionalOnProperty(prefix = "my.other.feature", name = "enabled", havingValue = "true")
  static class MetaAnnotationAndDirectAnnotationWithAlias {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @ConditionalOnProperty(name = "enabled", havingValue = "true")
  @interface ConditionalOnMyFeatureWithAlias {

    @AliasFor(annotation = ConditionalOnProperty.class, attribute = "prefix")
    String value();

  }

}
