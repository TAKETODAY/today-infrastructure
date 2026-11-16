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

package infra.app.context.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import infra.app.env.PropertiesPropertySourceLoader;
import infra.app.env.YamlPropertySourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SystemEnvironmentConfigDataLocationResolver}.
 *
 * @author Moritz Halbritter
 */
class SystemEnvironmentConfigDataLocationResolverTests {

  private final Map<String, String> environment = new HashMap<>();

  private final ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);

  private final SystemEnvironmentConfigDataLocationResolver resolver = new SystemEnvironmentConfigDataLocationResolver(
          List.of(new PropertiesPropertySourceLoader(), new YamlPropertySourceLoader()), environment::get);

  @Test
  void isResolvable() {
    assertThat(resolver.isResolvable(context, ConfigDataLocation.valueOf("env:VAR1"))).isTrue();
    assertThat(resolver.isResolvable(context, ConfigDataLocation.valueOf("dummy:VAR1"))).isFalse();
    assertThat(resolver.isResolvable(context, ConfigDataLocation.valueOf("VAR1"))).isFalse();
  }

  @Test
  void resolveResolves() {
    assertResolves("env:VAR1", PropertiesPropertySourceLoader.class);
  }

  @Test
  void resolveWhenHasNoVariableThrowsException() {
    assertThatExceptionOfType(ConfigDataLocationNotFoundException.class)
            .isThrownBy(() -> resolver.resolve(context, ConfigDataLocation.valueOf("env:VAR1")))
            .withMessage("Environment variable 'VAR1' is not set");
  }

  @Test
  void resolveWhenOptionalAndHasVariableResolves() {
    assertResolves("optional:env:VAR1", PropertiesPropertySourceLoader.class);
  }

  @Test
  void resolveWhenOptionalAndHasNoVariableResolvesEmpty() {
    assertThat(resolver.resolve(context, ConfigDataLocation.valueOf("optional:env:VAR1"))).isEmpty();
  }

  @Test
  void resolveWhenHasPropertiesExtensionHintResolves() {
    assertResolves("env:VAR1[.properties]", PropertiesPropertySourceLoader.class);
  }

  @Test
  void resolveWhenHasYamlExtensionHintResolves() {
    assertResolves("env:VAR1[.yaml]", YamlPropertySourceLoader.class);
  }

  @Test
  void resolveWhenHasUnknownExtensionHintThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolve(context, ConfigDataLocation.valueOf("env:VAR1[.dummy]")))
            .withMessage("File extension 'dummy' is not known to any PropertySourceLoader");
  }

  private void assertResolves(String location, Class<?> loaderClass) {
    environment.put("VAR1", "VALUE1");
    var resolved = resolver.resolve(context, ConfigDataLocation.valueOf(location));
    assertThat(resolved).hasSize(1);
    assertThat(resolved.get(0).getVariableName()).isEqualTo("VAR1");
    assertThat(resolved.get(0).getLoader()).isInstanceOf(loaderClass);
  }

}
