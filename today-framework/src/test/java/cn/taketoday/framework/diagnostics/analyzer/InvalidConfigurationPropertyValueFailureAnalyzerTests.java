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

package cn.taketoday.framework.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import cn.taketoday.context.properties.source.InvalidConfigurationPropertyValueException;
import cn.taketoday.context.support.MockEnvironment;
import cn.taketoday.core.env.EnumerablePropertySource;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginLookup;

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
    FailureAnalysis analysis = new InvalidConfigurationPropertyValueFailureAnalyzer().analyze(failure);
    assertThat(analysis).isNull();
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
            .contains("This is not valid.").doesNotContain("Additionally, this property is also set");
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
    InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
            "test.property", "invalid", "This is not valid.");
    FailureAnalysis analysis = performAnalysis(failure);
    assertCommonParts(failure, analysis);
    assertThat(analysis.getAction()).contains("Review the value of the property with the provided reason.");
    assertThat(analysis.getDescription())
            .contains("Additionally, this property is also set in the following property sources:")
            .contains("In 'additional' with the value 'valid'")
            .contains("In 'another' with the value 'test' (originating from 'TestOrigin test.property')");
  }

  @Test
  void analysisWithUnknownKey() {
    InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
            "test.key.not.defined", "invalid", "This is not valid.");
    assertThat(performAnalysis(failure)).isNull();
  }

  private void assertCommonParts(InvalidConfigurationPropertyValueException failure, FailureAnalysis analysis) {
    assertThat(analysis.getDescription()).contains("test.property").contains("invalid")
            .contains("TestOrigin test.property");
    assertThat(analysis.getCause()).isSameAs(failure);
  }

  private FailureAnalysis performAnalysis(InvalidConfigurationPropertyValueException failure) {
    InvalidConfigurationPropertyValueFailureAnalyzer analyzer = new InvalidConfigurationPropertyValueFailureAnalyzer();
    analyzer.setEnvironment(this.environment);
    return analyzer.analyze(failure);
  }

  static class OriginCapablePropertySource<T> extends EnumerablePropertySource<T> implements OriginLookup<String> {

    private final EnumerablePropertySource<T> propertySource;

    OriginCapablePropertySource(EnumerablePropertySource<T> propertySource) {
      super(propertySource.getName(), propertySource.getSource());
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
      return new Origin() {

        @Override
        public String toString() {
          return "TestOrigin " + name;
        }

      };
    }

    static <T> OriginCapablePropertySource<T> get(EnumerablePropertySource<T> propertySource) {
      return new OriginCapablePropertySource<>(propertySource);
    }

  }

}
