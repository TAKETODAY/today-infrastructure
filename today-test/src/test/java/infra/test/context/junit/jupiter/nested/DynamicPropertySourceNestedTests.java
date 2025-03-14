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

package infra.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.test.context.DynamicPropertyRegistry;
import infra.test.context.DynamicPropertySource;
import infra.test.context.NestedTestConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link DynamicPropertySource @DynamicPropertySource} in conjunction with the
 * {@link InfraExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
class DynamicPropertySourceNestedTests {

  private static final String TEST_CONTAINER_IP = "DynamicPropertySourceNestedTests.test.container.ip";

  private static final String TEST_CONTAINER_PORT = "DynamicPropertySourceNestedTests.test.container.port";

  static DemoContainer container = new DemoContainer();

  @DynamicPropertySource
  static void containerProperties(DynamicPropertyRegistry registry) {
    registry.add(TEST_CONTAINER_IP, container::getIpAddress);
    registry.add(TEST_CONTAINER_PORT, container::getPort);
  }

  @Test
  @DisplayName("@Service has values injected from @DynamicPropertySource")
  void serviceHasInjectedValues(@Autowired Service service) {
    assertServiceHasInjectedValues(service);
  }

  private static void assertServiceHasInjectedValues(Service service) {
    assertThat(service.getIp()).isEqualTo("127.0.0.1");
    assertThat(service.getPort()).isEqualTo(4242);
  }

  @Nested
  @NestedTestConfiguration(OVERRIDE)
  @JUnitConfig(Config.class)
  class DynamicPropertySourceFromSuperclassTests extends DynamicPropertySourceSuperclass {

    @Test
    @DisplayName("@Service has values injected from @DynamicPropertySource in superclass")
    void serviceHasInjectedValues(@Autowired Service service) {
      assertServiceHasInjectedValues(service);
    }
  }

  @Nested
  @NestedTestConfiguration(OVERRIDE)
  @JUnitConfig(Config.class)
  class DynamicPropertySourceFromInterfaceTests implements DynamicPropertySourceInterface {

    @Test
    @DisplayName("@Service has values injected from @DynamicPropertySource in interface")
    void serviceHasInjectedValues(@Autowired Service service) {
      assertServiceHasInjectedValues(service);
    }
  }

  @Nested
  @NestedTestConfiguration(OVERRIDE)
  @JUnitConfig(Config.class)
  class OverriddenConfigTests {

    @Test
    @DisplayName("@Service does not have values injected from @DynamicPropertySource in enclosing class")
    void serviceHasDefaultInjectedValues(@Autowired Service service) {
      assertThat(service.getIp()).isEqualTo("10.0.0.1");
      assertThat(service.getPort()).isEqualTo(-999);
    }
  }

  @Nested
  class DynamicPropertySourceFromEnclosingClassTests {

    @Test
    @DisplayName("@Service has values injected from @DynamicPropertySource in enclosing class")
    void serviceHasInjectedValues(@Autowired Service service) {
      assertServiceHasInjectedValues(service);
    }

    @Nested
    class DoubleNestedDynamicPropertySourceFromEnclosingClassTests {

      @Test
      @DisplayName("@Service has values injected from @DynamicPropertySource in enclosing class")
      void serviceHasInjectedValues(@Autowired Service service) {
        assertServiceHasInjectedValues(service);
      }
    }
  }

  @Nested
  class DynamicPropertySourceOverridesEnclosingClassTests {

    @DynamicPropertySource
    static void overrideDynamicPropertyFromEnclosingClass(DynamicPropertyRegistry registry) {
      registry.add(TEST_CONTAINER_PORT, () -> -999);
    }

    @Test
    @DisplayName("@Service has values injected from @DynamicPropertySource in enclosing class and nested class")
    void serviceHasInjectedValues(@Autowired Service service) {
      assertThat(service.getIp()).isEqualTo("127.0.0.1");
      assertThat(service.getPort()).isEqualTo(-999);
    }

  }

  static abstract class DynamicPropertySourceSuperclass {

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
      registry.add(TEST_CONTAINER_IP, container::getIpAddress);
      registry.add(TEST_CONTAINER_PORT, container::getPort);
    }
  }

  interface DynamicPropertySourceInterface {

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
      registry.add(TEST_CONTAINER_IP, container::getIpAddress);
      registry.add(TEST_CONTAINER_PORT, container::getPort);
    }
  }

  @Configuration
  @Import(Service.class)
  static class Config {
  }

  static class Service {

    private final String ip;

    private final int port;

    Service(@Value("${" + TEST_CONTAINER_IP + ":10.0.0.1}") String ip, @Value("${" + TEST_CONTAINER_PORT + ":-999}") int port) {
      this.ip = ip;
      this.port = port;
    }

    String getIp() {
      return this.ip;
    }

    int getPort() {
      return this.port;
    }
  }

  static class DemoContainer {

    String getIpAddress() {
      return "127.0.0.1";
    }

    int getPort() {
      return 4242;
    }
  }

}
