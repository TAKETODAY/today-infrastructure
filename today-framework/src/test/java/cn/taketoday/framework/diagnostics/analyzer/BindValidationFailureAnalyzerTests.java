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

package cn.taketoday.framework.diagnostics.analyzer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.FieldError;
import cn.taketoday.validation.Validator;
import cn.taketoday.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BindValidationFailureAnalyzer}.
 *
 * @author Madhura Bhave
 */
class BindValidationFailureAnalyzerTests {

  @BeforeEach
  void setup() {
    LocaleContextHolder.setLocale(Locale.US);
  }

  @AfterEach
  void cleanup() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void bindExceptionWithFieldErrorsDueToValidationFailure() {
    FailureAnalysis analysis = performAnalysis(FieldValidationFailureConfiguration.class);
    assertThat(analysis.getDescription()).contains(failure("test.foo.foo", "null", "must not be null"))
            .contains(failure("test.foo.value", "0", "at least five"))
            .contains(failure("test.foo.nested.bar", "null", "must not be null"))
            .contains(
                    "Binding to target cn.taketoday.framework.diagnostics.analyzer.BindValidationFailureAnalyzerTests$FieldValidationFailureProperties failed:");
  }

  @Test
  void bindExceptionWithOriginDueToValidationFailure() {
    FailureAnalysis analysis = performAnalysis(FieldValidationFailureConfiguration.class, "test.foo.value=4");
    assertThat(analysis.getDescription()).contains("Origin: \"test.foo.value\" from property source \"test\"");
  }

  @Test
  void bindExceptionWithObjectErrorsDueToValidationFailure() {
    FailureAnalysis analysis = performAnalysis(ObjectValidationFailureConfiguration.class);
    assertThat(analysis.getDescription()).contains("Reason: This object could not be bound.");
  }

  @Test
  void otherBindExceptionShouldReturnAnalysis() {
    BindException cause = new BindException(new FieldValidationFailureProperties(),
            "fieldValidationFailureProperties");
    cause.addError(new FieldError("test", "value", "must not be null"));
    BeanCreationException rootFailure = new BeanCreationException("bean creation failure", cause);
    FailureAnalysis analysis = new BindValidationFailureAnalyzer().analyze(rootFailure, rootFailure);
    assertThat(analysis.getDescription()).contains(failure("test.value", "null", "must not be null"));
  }

  private static String failure(String property, String value, String reason) {
    return String.format("Property: %s%n    Value: %s%n    Reason: %s", property, value, reason);
  }

  private FailureAnalysis performAnalysis(Class<?> configuration, String... environment) {
    BeanCreationException failure = createFailure(configuration, environment);
    assertThat(failure).isNotNull();
    return new BindValidationFailureAnalyzer().analyze(failure);
  }

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

  @EnableConfigurationProperties(FieldValidationFailureProperties.class)
  static class FieldValidationFailureConfiguration {

  }

  @EnableConfigurationProperties(ObjectErrorFailureProperties.class)
  static class ObjectValidationFailureConfiguration {

  }

  @ConfigurationProperties("test.foo")
  @Validated
  static class FieldValidationFailureProperties {

    @NotNull
    private String foo;

    @Min(value = 5, message = "at least five")
    private int value;

    @Valid
    private FieldValidationFailureProperties.Nested nested = new Nested();

    String getFoo() {
      return this.foo;
    }

    void setFoo(String foo) {
      this.foo = foo;
    }

    int getValue() {
      return this.value;
    }

    void setValue(int value) {
      this.value = value;
    }

    Nested getNested() {
      return this.nested;
    }

    void setNested(Nested nested) {
      this.nested = nested;
    }

    static class Nested {

      @NotNull
      private String bar;

      String getBar() {
        return this.bar;
      }

      void setBar(String bar) {
        this.bar = bar;
      }

    }

  }

  @ConfigurationProperties("foo.bar")
  @Validated
  static class ObjectErrorFailureProperties implements Validator {

    @Override
    public void validate(Object target, Errors errors) {
      errors.reject("my.objectError", "This object could not be bound.");
    }

    @Override
    public boolean supports(Class<?> clazz) {
      return true;
    }

  }

}
