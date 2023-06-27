/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.mock.env.MockEnvironment;
import cn.taketoday.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link InfraTestRandomPortEnvironmentPostProcessor}.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
class InfraTestRandomPortEnvironmentPostProcessorTests {

  private final InfraTestRandomPortEnvironmentPostProcessor postProcessor = new InfraTestRandomPortEnvironmentPostProcessor();

  private MockEnvironment environment;

  private PropertySources propertySources;

  @BeforeEach
  void setup() {
    this.environment = new MockEnvironment();
    this.propertySources = this.environment.getPropertySources();
  }

  @Test
  void postProcessWhenServerAndManagementPortIsZeroInTestPropertySource() {
    addTestPropertySource("0", "0");
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("0");
    assertThat(this.environment.getProperty("management.server.port")).isEqualTo("0");
  }

  @Test
  void postProcessWhenServerPortAndManagementPortIsZeroInDifferentPropertySources() {
    addTestPropertySource("0", null);
    Map<String, Object> source = new HashMap<>();
    source.put("management.server.port", "0");
    this.propertySources.addLast(new MapPropertySource("other", source));
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("0");
    assertThat(this.environment.getProperty("management.server.port")).isEqualTo("0");
  }

  @Test
  void postProcessWhenTestServerAndTestManagementPortAreNonZero() {
    addTestPropertySource("8080", "8081");
    this.environment.setProperty("server.port", "8080");
    this.environment.setProperty("management.server.port", "8081");
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("8080");
    assertThat(this.environment.getProperty("management.server.port")).isEqualTo("8081");
  }

  @Test
  void postProcessWhenTestServerPortIsZeroAndTestManagementPortIsNotNull() {
    addTestPropertySource("0", "8080");
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("0");
    assertThat(this.environment.getProperty("management.server.port")).isEqualTo("8080");
  }

  @Test
  void postProcessWhenTestServerPortIsZeroAndManagementPortIsNull() {
    addTestPropertySource("0", null);
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("0");
    assertThat(this.environment.getProperty("management.server.port")).isNull();
  }

  @Test
  void postProcessWhenTestServerPortIsZeroAndManagementPortIsNotNullAndSameInProduction() {
    addTestPropertySource("0", null);
    Map<String, Object> other = new HashMap<>();
    other.put("server.port", "8081");
    other.put("management.server.port", "8081");
    MapPropertySource otherSource = new MapPropertySource("other", other);
    this.propertySources.addLast(otherSource);
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("0");
    assertThat(this.environment.getProperty("management.server.port")).isEmpty();
  }

  @Test
  void postProcessWhenTestServerPortIsZeroAndManagementPortIsNotNullAndDefaultSameInProduction() {
    // mgmt port is 8080 which means it's on the same port as main server since that
    // is null in app properties
    addTestPropertySource("0", null);
    this.propertySources
            .addLast(new MapPropertySource("other", Collections.singletonMap("management.server.port", "8080")));
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("0");
    assertThat(this.environment.getProperty("management.server.port")).isEmpty();
  }

  @Test
  void postProcessWhenTestServerPortIsZeroAndManagementPortIsNotNullAndDifferentInProduction() {
    addTestPropertySource("0", null);
    this.propertySources
            .addLast(new MapPropertySource("other", Collections.singletonMap("management.server.port", "8081")));
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("0");
    assertThat(this.environment.getProperty("management.server.port")).isEqualTo("0");
  }

  @Test
  void postProcessWhenTestServerPortIsZeroAndManagementPortMinusOne() {
    addTestPropertySource("0", null);
    this.propertySources
            .addLast(new MapPropertySource("other", Collections.singletonMap("management.server.port", "-1")));
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("0");
    assertThat(this.environment.getProperty("management.server.port")).isEqualTo("-1");
  }

  @Test
  void postProcessWhenTestServerPortIsZeroAndManagementPortIsAnInteger() {
    addTestPropertySource("0", null);
    this.propertySources
            .addLast(new MapPropertySource("other", Collections.singletonMap("management.server.port", 8081)));
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("0");
    assertThat(this.environment.getProperty("management.server.port")).isEqualTo("0");
  }

  @Test
  void postProcessWhenManagementServerPortPlaceholderPresentShouldResolvePlaceholder() {
    addTestPropertySource("0", null);
    MapPropertySource testPropertySource = (MapPropertySource) this.propertySources
            .get(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
    testPropertySource.getSource().put("port", "9090");
    this.propertySources
            .addLast(new MapPropertySource("other", Collections.singletonMap("management.server.port", "${port}")));
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("0");
    assertThat(this.environment.getProperty("management.server.port")).isEqualTo("0");
  }

  @Test
  void postProcessWhenManagementServerPortPlaceholderAbsentShouldFail() {
    addTestPropertySource("0", null);
    this.propertySources
            .addLast(new MapPropertySource("other", Collections.singletonMap("management.server.port", "${port}")));
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.postProcessor.postProcessEnvironment(this.environment, null))
            .withMessage("Could not resolve placeholder 'port' in value \"${port}\"");
  }

  @Test
  void postProcessWhenServerPortPlaceholderPresentShouldResolvePlaceholder() {
    addTestPropertySource("0", null);
    MapPropertySource testPropertySource = (MapPropertySource) this.propertySources
            .get(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
    testPropertySource.getSource().put("port", "8080");
    Map<String, Object> source = new HashMap<>();
    source.put("server.port", "${port}");
    source.put("management.server.port", "9090");
    this.propertySources.addLast(new MapPropertySource("other", source));
    this.postProcessor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("server.port")).isEqualTo("0");
    assertThat(this.environment.getProperty("management.server.port")).isEqualTo("0");
  }

  @Test
  void postProcessWhenServerPortPlaceholderAbsentShouldFail() {
    addTestPropertySource("0", null);
    Map<String, Object> source = new HashMap<>();
    source.put("server.port", "${port}");
    source.put("management.server.port", "9090");
    this.propertySources.addLast(new MapPropertySource("other", source));
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.postProcessor.postProcessEnvironment(this.environment, null))
            .withMessage("Could not resolve placeholder 'port' in value \"${port}\"");
  }

  private void addTestPropertySource(String serverPort, String managementPort) {
    Map<String, Object> source = new HashMap<>();
    source.put("server.port", serverPort);
    source.put("management.server.port", managementPort);
    MapPropertySource inlineTestSource = new MapPropertySource(
            TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME, source);
    this.propertySources.addFirst(inlineTestSource);
  }

}
