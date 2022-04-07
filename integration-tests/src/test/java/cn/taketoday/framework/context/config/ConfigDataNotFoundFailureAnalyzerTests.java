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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.Test;

import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.origin.Origin;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigDataNotFoundFailureAnalyzer}.
 *
 * @author Michal Mlak
 * @author Phillip Webb
 */
class ConfigDataNotFoundFailureAnalyzerTests {

  private final ConfigDataNotFoundFailureAnalyzer analyzer = new ConfigDataNotFoundFailureAnalyzer();

  @Test
  void analyzeWhenConfigDataLocationNotFoundException() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("test");
    ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(location);
    FailureAnalysis result = this.analyzer.analyze(exception);
    assertThat(result.getDescription()).isEqualTo("Config data location 'test' does not exist");
    assertThat(result.getAction())
            .isEqualTo("Check that the value 'test' is correct, or prefix it with 'optional:'");
  }

  @Test
  void analyzeWhenOptionalConfigDataLocationNotFoundException() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("optional:test");
    ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(location);
    FailureAnalysis result = this.analyzer.analyze(exception);
    assertThat(result.getDescription()).isEqualTo("Config data location 'optional:test' does not exist");
    assertThat(result.getAction()).isEqualTo("Check that the value 'optional:test' is correct");
  }

  @Test
  void analyzeWhenConfigDataLocationWithOriginNotFoundException() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("test").withOrigin(new TestOrigin("origin"));
    ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(location);
    FailureAnalysis result = this.analyzer.analyze(exception);
    assertThat(result.getDescription()).isEqualTo("Config data location 'test' does not exist");
    assertThat(result.getAction())
            .isEqualTo("Check that the value 'test' at origin is correct, or prefix it with 'optional:'");
  }

  @Test
  void analyzeWhenConfigDataResourceNotFoundException() {
    ConfigDataResource resource = new TestConfigDataResource("myresource");
    ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(resource);
    FailureAnalysis result = this.analyzer.analyze(exception);
    assertThat(result.getDescription()).isEqualTo("Config data resource 'myresource' does not exist");
    assertThat(result.getAction()).isEqualTo("Check that the value is correct");
  }

  @Test
  void analyzeWhenConfigDataResourceWithLocationNotFoundException() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("test");
    ConfigDataResource resource = new TestConfigDataResource("myresource");
    ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(resource)
            .withLocation(location);
    FailureAnalysis result = this.analyzer.analyze(exception);
    assertThat(result.getDescription())
            .isEqualTo("Config data resource 'myresource' via location 'test' does not exist");
    assertThat(result.getAction())
            .isEqualTo("Check that the value 'test' is correct, or prefix it with 'optional:'");
  }

  static class TestOrigin implements Origin {

    private final String string;

    TestOrigin(String string) {
      this.string = string;
    }

    @Override
    public String toString() {
      return this.string;
    }

  }

  static class TestConfigDataResource extends ConfigDataResource {

    private final String string;

    TestConfigDataResource(String string) {
      this.string = string;
    }

    @Override
    public String toString() {
      return this.string;
    }

  }

}
