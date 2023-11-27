/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.bind;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.util.PropertyPlaceholderHandler;

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
