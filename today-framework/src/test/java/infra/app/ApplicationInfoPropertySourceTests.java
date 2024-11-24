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

package infra.app;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import infra.core.ApplicationPid;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.*;

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