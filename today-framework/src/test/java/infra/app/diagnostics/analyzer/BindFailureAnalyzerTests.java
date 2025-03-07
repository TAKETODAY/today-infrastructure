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

package infra.app.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.app.diagnostics.FailureAnalysis;
import infra.app.logging.LogLevel;
import infra.beans.factory.BeanCreationException;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.properties.ConfigurationProperties;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySources;
import infra.lang.Nullable;
import infra.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BindFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class BindFailureAnalyzerTests {

  @Test
  void analysisForUnboundElementsIsNull() {
    FailureAnalysis analysis = performAnalysis(UnboundElementsFailureConfiguration.class,
            "test.foo.listValue[0]=hello", "test.foo.listValue[2]=world");
    assertThat(analysis).isNull();
  }

  @Test
  void analysisForValidationExceptionIsNull() {
    FailureAnalysis analysis = performAnalysis(FieldValidationFailureConfiguration.class, "test.foo.value=1");
    assertThat(analysis).isNull();
  }

  @Test
  void bindExceptionDueToOtherFailure() {
    FailureAnalysis analysis = performAnalysis(GenericFailureConfiguration.class, "test.foo.value=alpha");
    assertThat(analysis.getDescription()).contains(failure("test.foo.value", "alpha",
            "\"test.foo.value\" from property source \"test\"", "failed to convert java.lang.String to int"));
  }

  @Test
  void bindExceptionForUnknownValueInEnumListsValidValuesInAction() {
    FailureAnalysis analysis = performAnalysis(EnumFailureConfiguration.class, "test.foo.fruit=apple,strawberry");
    for (Fruit fruit : Fruit.values()) {
      assertThat(analysis.getAction()).contains(fruit.name());
    }
  }

  @Test
  void bindExceptionWithNestedFailureShouldDisplayNestedMessage() {
    FailureAnalysis analysis = performAnalysis(NestedFailureConfiguration.class, "test.foo.value=hello");
    assertThat(analysis.getDescription()).contains(failure("test.foo.value", "hello",
            "\"test.foo.value\" from property source \"test\"", "java.lang.RuntimeException: This is a failure"));
  }

  @Test
  void bindExceptionDueToClassNotFoundConversionFailure() {
    FailureAnalysis analysis = performAnalysis(GenericFailureConfiguration.class,
            "test.foo.type=com.example.Missing");
    assertThat(analysis.getDescription()).contains(failure("test.foo.type", "com.example.Missing",
            "\"test.foo.type\" from property source \"test\"",
            "failed to convert java.lang.String to java.lang.Class<?> (caused by java.lang.ClassNotFoundException: com.example.Missing"));
  }

  @Test
  void bindExceptionDueToMapConversionFailure() {
    FailureAnalysis analysis = performAnalysis(LoggingLevelFailureConfiguration.class, "logging.level=debug");
    assertThat(analysis.getDescription()).contains(failure("logging.level", "debug",
            "\"logging.level\" from property source \"test\"",
            "infra.core.conversion.ConverterNotFoundException: No converter found capable of converting "
                    + "from type [java.lang.String] to type [java.util.Map<java.lang.String, "
                    + "infra.app.logging.LogLevel>]"));
  }

  @Test
  void bindExceptionWithSupressedMissingParametersException() {
    BeanCreationException failure = createFailure(GenericFailureConfiguration.class, "test.foo.value=alpha");
    failure.addSuppressed(new IllegalStateException(
            "Missing parameter names. Ensure that the compiler uses the '-parameters' flag"));
    FailureAnalysis analysis = new BindFailureAnalyzer().analyze(failure);
    assertThat(analysis.getDescription())
            .contains(failure("test.foo.value", "alpha", "\"test.foo.value\" from property source \"test\"",
                    "failed to convert java.lang.String to int"))
            .contains(MissingParameterNamesFailureAnalyzer.POSSIBILITY);
    assertThat(analysis.getAction()).contains(MissingParameterNamesFailureAnalyzer.ACTION);
  }

  private static String failure(String property, String value, String origin, String reason) {
    return String.format("Property: %s%n    Value: \"%s\"%n    Origin: %s%n    Reason: %s", property, value, origin,
            reason);
  }

  private FailureAnalysis performAnalysis(Class<?> configuration, String... environment) {
    BeanCreationException failure = createFailure(configuration, environment);
    assertThat(failure).isNotNull();
    return new BindFailureAnalyzer().analyze(failure);
  }

  @Nullable
  private BeanCreationException createFailure(Class<?> configuration, String... environment) {
    try {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      addEnvironment(context, environment);
      context.register(configuration);
      context.refresh();
      context.close();
      return null;
    }
    catch (BeanCreationException ex) {
      return ex;
    }
  }

  private void addEnvironment(AnnotationConfigApplicationContext context, String[] environment) {
    PropertySources sources = context.getEnvironment().getPropertySources();
    Map<String, Object> map = new HashMap<>();
    for (String pair : environment) {
      int index = pair.indexOf('=');
      String key = (index > 0) ? pair.substring(0, index) : pair;
      String value = (index > 0) ? pair.substring(index + 1) : "";
      map.put(key.trim(), value.trim());
    }
    sources.addFirst(new MapPropertySource("test", map));
  }

  @EnableConfigurationProperties(BindValidationFailureAnalyzerTests.FieldValidationFailureProperties.class)
  static class FieldValidationFailureConfiguration {

  }

  @EnableConfigurationProperties(UnboundElementsFailureProperties.class)
  static class UnboundElementsFailureConfiguration {

  }

  @EnableConfigurationProperties(GenericFailureProperties.class)
  static class GenericFailureConfiguration {

  }

  @EnableConfigurationProperties(EnumFailureProperties.class)
  static class EnumFailureConfiguration {

  }

  @EnableConfigurationProperties(NestedFailureProperties.class)
  static class NestedFailureConfiguration {

  }

  @EnableConfigurationProperties(LoggingProperties.class)
  static class LoggingLevelFailureConfiguration {

  }

  @ConfigurationProperties("test.foo")
  @Validated
  static class FieldValidationFailureProperties {

    @Min(value = 5, message = "at least five")
    private int value;

    int getValue() {
      return this.value;
    }

    void setValue(int value) {
      this.value = value;
    }

  }

  @ConfigurationProperties("test.foo")
  static class UnboundElementsFailureProperties {

    private List<String> listValue;

    List<String> getListValue() {
      return this.listValue;
    }

    void setListValue(List<String> listValue) {
      this.listValue = listValue;
    }

  }

  @ConfigurationProperties("test.foo")
  static class GenericFailureProperties {

    private int value;

    private Class<?> type;

    int getValue() {
      return this.value;
    }

    void setValue(int value) {
      this.value = value;
    }

    Class<?> getType() {
      return this.type;
    }

    void setType(Class<?> type) {
      this.type = type;
    }

  }

  @ConfigurationProperties("test.foo")
  static class EnumFailureProperties {

    private Set<Fruit> fruit;

    Set<Fruit> getFruit() {
      return this.fruit;
    }

    void setFruit(Set<Fruit> fruit) {
      this.fruit = fruit;
    }

  }

  @ConfigurationProperties("test.foo")
  static class NestedFailureProperties {

    private String value;

    String getValue() {
      return this.value;
    }

    void setValue(String value) {
      throw new RuntimeException("This is a failure");
    }

  }

  @ConfigurationProperties("logging")
  static class LoggingProperties {

    private Map<String, LogLevel> level = new HashMap<>();

    Map<String, LogLevel> getLevel() {
      return this.level;
    }

    void setLevel(Map<String, LogLevel> level) {
      this.level = level;
    }

  }

  enum Fruit {

    APPLE, BANANA, ORANGE

  }

}
