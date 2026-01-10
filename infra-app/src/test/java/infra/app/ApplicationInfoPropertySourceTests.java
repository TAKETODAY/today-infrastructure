/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import infra.core.ApplicationPid;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/8/20 14:28
 */
class ApplicationInfoPropertySourceTests {

  @Test
  void shouldAddVersion() {
    MockEnvironment environment = new MockEnvironment();
    environment.getPropertySources().addLast(new ApplicationInfoPropertySource("1.2.3"));
    assertThat(environment.getProperty("app.version")).isEqualTo("1.2.3");
  }

  @Test
  void shouldNotAddVersionIfVersionIsNotAvailable() {
    MockEnvironment environment = new MockEnvironment();
    environment.getPropertySources().addLast(new ApplicationInfoPropertySource((String) null));
    assertThat(environment.containsProperty("app.version")).isFalse();
  }

  @Test
  void shouldAddPid() {
    MockEnvironment environment = new MockEnvironment();
    environment.getPropertySources().addLast(new ApplicationInfoPropertySource("1.2.3"));
    assertThat(environment.getProperty("app.pid", Long.class))
            .isEqualTo(new ApplicationPid().toLong());
  }

  @Test
  void shouldMoveToEnd() {
    MockEnvironment environment = new MockEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("first", Collections.emptyMap()));
    environment.getPropertySources().addAfter("first", new MapPropertySource("second", Collections.emptyMap()));
    environment.getPropertySources().addFirst(new ApplicationInfoPropertySource("1.2.3"));
    List<String> propertySources = environment.getPropertySources().stream().map(PropertySource::getName).toList();
    assertThat(propertySources).containsExactly("applicationInfo", "first", "second", "mockProperties");
    ApplicationInfoPropertySource.moveToEnd(environment);
    List<String> propertySourcesAfterMove = environment.getPropertySources()
            .stream()
            .map(PropertySource::getName)
            .toList();
    assertThat(propertySourcesAfterMove).containsExactly("first", "second", "mockProperties", "applicationInfo");
  }

}