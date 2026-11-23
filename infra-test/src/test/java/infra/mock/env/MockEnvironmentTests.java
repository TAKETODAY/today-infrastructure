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

package infra.mock.env;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class MockEnvironmentTests {

  @Test
  void environmentUsesOnlyItsMockPropertySource() {
    MockEnvironment environment = new MockEnvironment();
    assertThat(environment.getPropertySources()).hasSize(1);
    assertThat(environment.getPropertySources().iterator().next()).isInstanceOf(MockPropertySource.class);
    assertThat(environment.getProperty("java.version")).isNull();
    assertThat(environment.withProperty("name", "test")).isSameAs(environment);
    environment.setProperty("number", "42");
    environment.setProperty("message", "Hello ${name}");
    assertThat(environment.getRequiredProperty("name")).isEqualTo("test");
    assertThat(environment.getProperty("number", Integer.class)).isEqualTo(42);
    assertThat(environment.getProperty("message")).isEqualTo("Hello test");
    environment.setProperty("name", "updated");
    assertThat(environment.getProperty("message")).isEqualTo("Hello updated");
  }

  @Test
  void propertySourceConstructorsAndMutatorsPreserveBackingProperties() {
    MockPropertySource defaults = new MockPropertySource();
    assertThat(defaults.getName()).isEqualTo(MockPropertySource.MOCK_PROPERTIES_PROPERTY_SOURCE_NAME);
    assertThat(defaults.getPropertyNames()).isEmpty();
    assertThat(new MockPropertySource("named").getName()).isEqualTo("named");

    Properties properties = new Properties();
    properties.put("existing", "value");
    MockPropertySource source = new MockPropertySource(properties);
    assertThat(source.getName()).isEqualTo("mockProperties");
    assertThat(source.getProperty("existing")).isEqualTo("value");
    assertThat(source.withProperty("number", 42)).isSameAs(source);
    source.setProperty("existing", "updated");
    assertThat(properties).containsEntry("number", 42).containsEntry("existing", "updated");

    MockPropertySource named = new MockPropertySource("custom", properties);
    properties.put("external", true);
    assertThat(named.getName()).isEqualTo("custom");
    assertThat(named.getProperty("external")).isEqualTo(true);
    assertThat(named.getPropertyNames()).containsExactlyInAnyOrder("existing", "number", "external");
  }

}
