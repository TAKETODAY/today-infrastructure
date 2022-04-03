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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import cn.taketoday.context.support.MockEnvironment;
import cn.taketoday.core.env.EnumerablePropertySource;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginLookup;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MutuallyExclusiveConfigurationPropertiesFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class MutuallyExclusiveConfigurationPropertiesFailureAnalyzerTests {

  private final MockEnvironment environment = new MockEnvironment();

  @Test
  void analyzeWhenEnvironmentIsNullShouldReturnNull() {
    MutuallyExclusiveConfigurationPropertiesException failure = new MutuallyExclusiveConfigurationPropertiesException(
            new HashSet<>(Arrays.asList("com.example.a", "com.example.b")),
            new HashSet<>(Arrays.asList("com.example.a", "com.example.b")));
    FailureAnalysis failureAnalysis = new MutuallyExclusiveConfigurationPropertiesFailureAnalyzer()
            .analyze(failure);
    assertThat(failureAnalysis).isNull();
  }

  @Test
  void analyzeWhenNotAllPropertiesAreInTheEnvironmentShouldReturnNull() {
    MapPropertySource source = new MapPropertySource("test", Collections.singletonMap("com.example.a", "alpha"));
    this.environment.getPropertySources().addFirst(OriginCapablePropertySource.get(source));
    MutuallyExclusiveConfigurationPropertiesException failure = new MutuallyExclusiveConfigurationPropertiesException(
            new HashSet<>(Arrays.asList("com.example.a", "com.example.b")),
            new HashSet<>(Arrays.asList("com.example.a", "com.example.b")));
    FailureAnalysis analysis = performAnalysis(failure);
    assertThat(analysis).isNull();
  }

  @Test
  void analyzeWhenAllConfiguredPropertiesAreInTheEnvironmentShouldReturnAnalysis() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("com.example.a", "alpha");
    properties.put("com.example.b", "bravo");
    MapPropertySource source = new MapPropertySource("test", properties);
    this.environment.getPropertySources().addFirst(OriginCapablePropertySource.get(source));
    MutuallyExclusiveConfigurationPropertiesException failure = new MutuallyExclusiveConfigurationPropertiesException(
            new HashSet<>(Arrays.asList("com.example.a", "com.example.b")),
            new HashSet<>(Arrays.asList("com.example.a", "com.example.b")));
    FailureAnalysis analysis = performAnalysis(failure);
    assertThat(analysis.getAction()).isEqualTo(
            "Update your configuration so that only one of the mutually exclusive properties is configured.");
    assertThat(analysis.getDescription()).contains(String.format(
                    "The following configuration properties are mutually exclusive:%n%n\tcom.example.a%n\tcom.example.b%n"))
            .contains(String
                    .format("However, more than one of those properties has been configured at the same time:%n%n"
                            + "\tcom.example.a (originating from 'TestOrigin test')%n"
                            + "\tcom.example.b (originating from 'TestOrigin test')%n"));
  }

  @Test
  void analyzeWhenPropertyIsInMultiplePropertySourcesShouldListEachSourceInAnalysis() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("com.example.a", "alpha");
    properties.put("com.example.b", "bravo");
    this.environment.getPropertySources()
            .addFirst(OriginCapablePropertySource.get(new MapPropertySource("test-one", properties)));
    this.environment.getPropertySources()
            .addLast(OriginCapablePropertySource.get(new MapPropertySource("test-two", properties)));
    MutuallyExclusiveConfigurationPropertiesException failure = new MutuallyExclusiveConfigurationPropertiesException(
            new HashSet<>(Arrays.asList("com.example.a", "com.example.b")),
            new HashSet<>(Arrays.asList("com.example.a", "com.example.b")));
    FailureAnalysis analysis = performAnalysis(failure);
    assertThat(analysis.getAction()).isEqualTo(
            "Update your configuration so that only one of the mutually exclusive properties is configured.");
    assertThat(analysis.getDescription()).contains(String.format(
                    "The following configuration properties are mutually exclusive:%n%n\tcom.example.a%n\tcom.example.b%n"))
            .contains(String
                    .format("However, more than one of those properties has been configured at the same time:%n%n"
                            + "\tcom.example.a (originating from 'TestOrigin test-one')%n"
                            + "\tcom.example.a (originating from 'TestOrigin test-two')%n"
                            + "\tcom.example.b (originating from 'TestOrigin test-one')%n"
                            + "\tcom.example.b (originating from 'TestOrigin test-two')%n"));
  }

  private FailureAnalysis performAnalysis(MutuallyExclusiveConfigurationPropertiesException failure) {
    MutuallyExclusiveConfigurationPropertiesFailureAnalyzer analyzer = new MutuallyExclusiveConfigurationPropertiesFailureAnalyzer();
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
          return "TestOrigin " + getName();
        }

      };
    }

    static <T> OriginCapablePropertySource<T> get(EnumerablePropertySource<T> propertySource) {
      return new OriginCapablePropertySource<>(propertySource);
    }

  }

}
