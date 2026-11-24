/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.test.context.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.env.Environment;
import infra.mock.env.MockEnvironment;
import infra.test.context.DynamicPropertyRegistrar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

class DynamicPropertyRegistrarInitializerTests {

  @Test
  void requiresConfigurableEnvironmentBeforeInitialization() {
    var initializer = new DynamicPropertyRegistrarBeanInitializer();
    assertThatIllegalArgumentException().isThrownBy(() -> initializer.setEnvironment(mock(Environment.class)))
            .withMessage("Environment must be a ConfigurableEnvironment");
    assertThatIllegalStateException().isThrownBy(() -> initializer.initialize(new StandardBeanFactory()))
            .withMessage("Environment is required");
  }

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "DEBUG", "OFF" })
  void eagerlyRegistersLazyPropertySuppliersWithOrWithoutDiagnostics(LogLevel level) {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = DynamicPropertyRegistrarBeanInitializer.class.getName();
    var configuration = logging.getLoggerConfiguration(name);
    LogLevel previous = (configuration != null ? configuration.getConfiguredLevel() : null);
    logging.setLogLevel(name, level);
    try {
      var environment = new MockEnvironment();
      var factory = new StandardBeanFactory();
      factory.registerSingleton("registrar", (DynamicPropertyRegistrar) registry -> registry.add("dynamic", () -> "registered"));
      var initializer = new DynamicPropertyRegistrarBeanInitializer();
      initializer.setEnvironment(environment);
      initializer.initialize(factory);
      assertThat(environment.getProperty("dynamic")).isEqualTo("registered");
      initializer.initialize(new StandardBeanFactory());
      assertThat(environment.getProperty("dynamic")).isEqualTo("registered");
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

}
