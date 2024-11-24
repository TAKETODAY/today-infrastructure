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

package infra.app.env;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import infra.app.Application;
import infra.app.env.SystemEnvironmentPropertySourceEnvironmentPostProcessor.OriginAwareSystemEnvironmentPropertySource;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.PropertySource;
import infra.core.env.StandardEnvironment;
import infra.core.env.SystemEnvironmentPropertySource;
import infra.origin.SystemEnvironmentOrigin;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemEnvironmentPropertySourceEnvironmentPostProcessor}.
 *
 * @author Madhura Bhave
 */
class SystemEnvironmentPropertySourceEnvironmentPostProcessorTests {

  private final ConfigurableEnvironment environment = new StandardEnvironment();

  private final Application application = new Application();

  @Test
  void postProcessShouldReplaceSystemEnvironmentPropertySource() {
    SystemEnvironmentPropertySourceEnvironmentPostProcessor postProcessor = new SystemEnvironmentPropertySourceEnvironmentPostProcessor();
    postProcessor.postProcessEnvironment(this.environment, this.application);
    PropertySource<?> replaced = this.environment.getPropertySources().get("systemEnvironment");
    assertThat(replaced).isInstanceOf(OriginAwareSystemEnvironmentPropertySource.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void replacedPropertySourceShouldBeOriginAware() {
    SystemEnvironmentPropertySourceEnvironmentPostProcessor postProcessor = new SystemEnvironmentPropertySourceEnvironmentPostProcessor();
    PropertySource<?> original = this.environment.getPropertySources().get("systemEnvironment");
    postProcessor.postProcessEnvironment(this.environment, this.application);
    OriginAwareSystemEnvironmentPropertySource replaced = (OriginAwareSystemEnvironmentPropertySource) this.environment
            .getPropertySources()
            .get("systemEnvironment");
    Map<String, Object> originalMap = (Map<String, Object>) original.getSource();
    Map<String, Object> replacedMap = replaced.getSource();
    originalMap.forEach((key, value) -> {
      Object actual = replacedMap.get(key);
      assertThat(actual).isEqualTo(value);
      assertThat(replaced.getOrigin(key)).isInstanceOf(SystemEnvironmentOrigin.class);
    });
  }

  @Test
  void replacedPropertySourceWhenPropertyAbsentShouldReturnNullOrigin() {
    SystemEnvironmentPropertySourceEnvironmentPostProcessor postProcessor = new SystemEnvironmentPropertySourceEnvironmentPostProcessor();
    postProcessor.postProcessEnvironment(this.environment, this.application);
    OriginAwareSystemEnvironmentPropertySource replaced = (OriginAwareSystemEnvironmentPropertySource) this.environment
            .getPropertySources()
            .get("systemEnvironment");
    assertThat(replaced.getOrigin("NON_EXISTENT")).isNull();
  }

  @Test
  void replacedPropertySourceShouldResolveProperty() {
    SystemEnvironmentPropertySourceEnvironmentPostProcessor postProcessor = new SystemEnvironmentPropertySourceEnvironmentPostProcessor();
    Map<String, Object> source = Collections.singletonMap("FOO_BAR_BAZ", "hello");
    this.environment.getPropertySources()
            .replace("systemEnvironment", new SystemEnvironmentPropertySource("systemEnvironment", source));
    postProcessor.postProcessEnvironment(this.environment, this.application);
    OriginAwareSystemEnvironmentPropertySource replaced = (OriginAwareSystemEnvironmentPropertySource) this.environment
            .getPropertySources()
            .get("systemEnvironment");
    SystemEnvironmentOrigin origin = (SystemEnvironmentOrigin) replaced.getOrigin("foo.bar.baz");
    assertThat(origin.getProperty()).isEqualTo("FOO_BAR_BAZ");
    assertThat(replaced.getProperty("foo.bar.baz")).isEqualTo("hello");
  }

  @Test
  void propertySourceShouldBePrefixed() {
    SystemEnvironmentPropertySourceEnvironmentPostProcessor postProcessor = new SystemEnvironmentPropertySourceEnvironmentPostProcessor();
    Application application = new Application();
    application.setEnvironmentPrefix("my");
    postProcessor.postProcessEnvironment(this.environment, application);
    OriginAwareSystemEnvironmentPropertySource replaced = (OriginAwareSystemEnvironmentPropertySource) this.environment
            .getPropertySources()
            .get("systemEnvironment");
    assertThat(replaced.getPrefix()).isEqualTo("my");
  }

}
