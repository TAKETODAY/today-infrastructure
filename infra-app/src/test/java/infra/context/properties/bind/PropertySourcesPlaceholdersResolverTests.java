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

package infra.context.properties.bind;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import infra.core.env.ConfigurableEnvironment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySources;
import infra.util.PropertyPlaceholderHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PropertySourcesPlaceholdersResolver}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class PropertySourcesPlaceholdersResolverTests {

  private PropertySourcesPlaceholdersResolver resolver;

  @Test
  void placeholderResolverIfEnvironmentNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new PropertySourcesPlaceholdersResolver((ConfigurableEnvironment) null))
            .withMessageContaining("Environment is required");
  }

  @Test
  void resolveIfPlaceholderPresentResolvesProperty() {
    PropertySources sources = getPropertySources();
    this.resolver = new PropertySourcesPlaceholdersResolver(sources);
    Object resolved = this.resolver.resolvePlaceholders("${FOO}");
    assertThat(resolved).isEqualTo("hello world");
  }

  @Test
  void resolveIfPlaceholderAbsentUsesDefault() {
    this.resolver = new PropertySourcesPlaceholdersResolver((PropertySources) null);
    Object resolved = this.resolver.resolvePlaceholders("${FOO:bar}");
    assertThat(resolved).isEqualTo("bar");
  }

  @Test
  void resolveIfPlaceholderAbsentAndNoDefaultUsesPlaceholder() {
    this.resolver = new PropertySourcesPlaceholdersResolver((PropertySources) null);
    Object resolved = this.resolver.resolvePlaceholders("${FOO}");
    assertThat(resolved).isEqualTo("${FOO}");
  }

  @Test
  void resolveIfHelperPresentShouldUseIt() {
    PropertySources sources = getPropertySources();
    TestPropertyPlaceholderHelper helper = new TestPropertyPlaceholderHelper("$<", ">");
    this.resolver = new PropertySourcesPlaceholdersResolver(sources, helper);
    Object resolved = this.resolver.resolvePlaceholders("$<FOO>");
    assertThat(resolved).isEqualTo("hello world");
  }

  private PropertySources getPropertySources() {
    PropertySources sources = new PropertySources();
    Map<String, Object> source = new HashMap<>();
    source.put("FOO", "hello world");
    sources.addFirst(new MapPropertySource("test", source));
    return sources;
  }

  static class TestPropertyPlaceholderHelper extends PropertyPlaceholderHandler {

    TestPropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
      super(placeholderPrefix, placeholderSuffix);
    }

  }

}
