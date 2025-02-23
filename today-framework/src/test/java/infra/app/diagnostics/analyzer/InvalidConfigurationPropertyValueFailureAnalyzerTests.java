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

package infra.app.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import infra.app.diagnostics.FailureAnalysis;
import infra.context.properties.source.ConfigurationPropertySources;
import infra.context.properties.source.InvalidConfigurationPropertyValueException;
import infra.core.env.EnumerablePropertySource;
import infra.core.env.MapPropertySource;
import infra.core.env.SystemEnvironmentPropertySource;
import infra.mock.env.MockEnvironment;
import infra.origin.Origin;
import infra.origin.OriginLookup;
import infra.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InvalidConfigurationPropertyValueFailureAnalyzer}.
 *
 * @author Stephane Nicoll
 */
class InvalidConfigurationPropertyValueFailureAnalyzerTests {

  private final MockEnvironment environment = new MockEnvironment();

  @Test
  void analysisWithNullEnvironment() {
    InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
            "test.property", "invalid", "This is not valid.");
    FailureAnalysis analysis = new InvalidConfigurationPropertyValueFailureAnalyzer(null).analyze(failure);
    assertThat(analysis.getDescription())
            .contains("Invalid value 'invalid' for configuration property 'test.property'.");
  }

  @Test
  void analysisWithKnownProperty() {
    MapPropertySource source = new MapPropertySource("test", Collections.singletonMap("test.property", "invalid"));
    this.environment.getPropertySources().addFirst(OriginCapablePropertySource.get(source));
    InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
            "test.property", "invalid", "This is not valid.");
    FailureAnalysis analysis = performAnalysis(failure);
    assertCommonParts(failure, analysis);
    assertThat(analysis.getAction()).contains("Review the value of the property with the provided reason.");
    assertThat(analysis.getDescription()).contains("Validation failed for the following reason")
            .contains("This is not valid.")
            .doesNotContain("Additionally, this property is also set");
  }

  @Test
  void analysisWithKnownPropertyFromSystemEnvironment() {
    MapPropertySource source = new SystemEnvironmentPropertySource("systemEnvironment",
            Collections.singletonMap("COM_EXAMPLE_TESTPROPERTY", "invalid"));
    this.environment.getPropertySources().addFirst(source);
    ConfigurationPropertySources.attach(this.environment);
    assertThat(this.environment.getProperty("com.example.test-property")).isEqualTo("invalid");
    InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
            "com.example.test-property", "invalid", "This is not valid.");
    FailureAnalysis analysis = performAnalysis(failure);
    assertThat(analysis.getDescription()).contains("com.example.test-property")
            .contains("invalid")
            .contains("property source \"systemEnvironment\"");
    assertThat(analysis.getCause()).isSameAs(failure);
    assertThat(analysis.getAction()).contains("Review the value of the property with the provided reason.");
    assertThat(analysis.getDescription()).contains("Validation failed for the following reason")
            .contains("This is not valid.")
            .doesNotContain("Additionally, this property is also set");
  }

  @Test
  void analysisWithKnownPropertyAndNoReason() {
    MapPropertySource source = new MapPropertySource("test", Collections.singletonMap("test.property", "invalid"));
    this.environment.getPropertySources().addFirst(OriginCapablePropertySource.get(source));
    InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
            "test.property", "invalid", null);
    FailureAnalysis analysis = performAnalysis(failure);
    assertThat(analysis.getAction()).contains("Review the value of the property.");
    assertThat(analysis.getDescription()).contains("No reason was provided.")
            .doesNotContain("Additionally, this property is also set");
  }

  @Test
  void analysisWithKnownPropertyAndOtherCandidates() {
    MapPropertySource source = new MapPropertySource("test", Collections.singletonMap("test.property", "invalid"));
    MapPropertySource additional = new MapPropertySource("additional",
            Collections.singletonMap("test.property", "valid"));
    MapPropertySource another = new MapPropertySource("another", Collections.singletonMap("test.property", "test"));
    this.environment.getPropertySources().addFirst(OriginCapablePropertySource.get(source));
    this.environment.getPropertySources().addLast(additional);
    this.environment.getPropertySources().addLast(OriginCapablePropertySource.get(another));
    this.environment.getPropertySources().addLast(OriginCapablePropertySource.get("another-again", another));
    ConfigurationPropertySources.attach(this.environment);
    InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
            "test.property", "invalid", "This is not valid.");
    FailureAnalysis analysis = performAnalysis(failure);
    assertCommonParts(failure, analysis);
    assertThat(analysis.getAction()).contains("Review the value of the property with the provided reason.");
    assertThat(analysis.getDescription())
            .contains("Additionally, this property is also set in the following property sources:")
            .contains("In 'additional' with the value 'valid'")
            .contains("In 'another' with the value 'test' (originating from 'TestOrigin test.property')")
            .doesNotContain("another-again");
  }

  @Test
  void analysisWithUnknownKey() {
    InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
            "test.key.not.defined", "invalid", "This is not valid.");
    FailureAnalysis analysis = performAnalysis(failure);
    assertThat(analysis.getDescription())
            .contains("Invalid value 'invalid' for configuration property 'test.key.not.defined'.");
  }

  private void assertCommonParts(InvalidConfigurationPropertyValueException failure, FailureAnalysis analysis) {
    assertThat(analysis.getDescription()).contains("test.property")
            .contains("invalid")
            .contains("TestOrigin test.property");
    assertThat(analysis.getCause()).isSameAs(failure);
  }

  private FailureAnalysis performAnalysis(InvalidConfigurationPropertyValueException failure) {
    InvalidConfigurationPropertyValueFailureAnalyzer analyzer = new InvalidConfigurationPropertyValueFailureAnalyzer(
            this.environment);
    return analyzer.analyze(failure);
  }

  static class OriginCapablePropertySource<T> extends EnumerablePropertySource<T> implements OriginLookup<String> {

    private final EnumerablePropertySource<T> propertySource;

    OriginCapablePropertySource(EnumerablePropertySource<T> propertySource) {
      this(propertySource.getName(), propertySource);
    }

    OriginCapablePropertySource(String name, EnumerablePropertySource<T> propertySource) {
      super(name, propertySource.getSource());
      this.propertySource = propertySource;
    }

    @Override
    public Object getProperty(String name) {
      return this.propertySource.getProperty(name);
    }

    @Override
    public String[] getPropertyNames() {
      return this.propertySource.getPropertyNames();
    }

    @Override
    public Origin getOrigin(String name) {
      return new TestOrigin(name, this.propertySource.getName());
    }

    static <T> OriginCapablePropertySource<T> get(EnumerablePropertySource<T> propertySource) {
      return new OriginCapablePropertySource<>(propertySource);
    }

    static <T> OriginCapablePropertySource<T> get(String name, EnumerablePropertySource<T> propertySource) {
      return new OriginCapablePropertySource<>(name, propertySource);
    }

    static final class TestOrigin implements Origin {

      private final String name;

      private final String sourceName;

      private TestOrigin(String name, String sourceName) {
        this.name = name;
        this.sourceName = sourceName;
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) {
          return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
          return false;
        }
        TestOrigin other = (TestOrigin) obj;
        return ObjectUtils.nullSafeEquals(this.name, other.name)
                && ObjectUtils.nullSafeEquals(this.sourceName, other.sourceName);
      }

      @Override
      public int hashCode() {
        return ObjectUtils.nullSafeHashCode(this.name);
      }

      @Override
      public String toString() {
        return "TestOrigin " + this.name;
      }

    }

  }

}
