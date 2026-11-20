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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import infra.app.env.PropertiesPropertySourceLoader;
import infra.app.env.PropertySourceLoader;
import infra.app.env.YamlPropertySourceLoader;
import infra.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemEnvironmentConfigDataResource}.
 * @author Moritz Halbritter
 */
class SystemEnvironmentConfigDataResourceTests {

  private final Map<String, String> environment = new HashMap<>();

  private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();

  private final PropertiesPropertySourceLoader propertiesLoader = new PropertiesPropertySourceLoader();

  @Test
  void loadLoadsPropertySources() throws IOException {
    this.environment.put("VAR1", "key1=value1");
    List<PropertySource<?>> loaded = createResource("VAR1").load();
    assertThat(loaded).hasSize(1);
    assertThat(loaded.get(0).getProperty("key1")).isEqualTo("value1");
  }

  @Test
  void loadUsesDefaultCharsetOfNoEncodingSpecified() throws IOException {
    this.environment.put("VAR1", "smiley=😃");
    List<PropertySource<?>> loaded = createResource("VAR1", this.propertiesLoader, StandardCharsets.UTF_8).load();
    assertThat(loaded).hasSize(1);
    assertThat(loaded.get(0).getProperty("smiley")).isEqualTo("😃");
  }

  @Test
  void loadWhenNoContentReturnsNull() throws IOException {
    assertThat(createResource("VAR1").load()).isNull();
  }

  @Test
  void equalsAndHashcode() {
    var var1 = createResource("VAR1");
    var var2 = createResource("VAR2");
    var var3 = createResource("VAR1", this.yamlLoader);
    var var4 = createResource("VAR1");
    assertThat(var1).isNotEqualTo(var2);
    assertThat(var1).isNotEqualTo(var3);
    assertThat(var1).isEqualTo(var4);
    assertThat(var1).hasSameHashCodeAs(var4);
  }

  @Test
  void toStringReturnsString() {
    assertThat(createResource("VAR1"))
            .hasToString("system environment variable [VAR1] content loaded using PropertiesPropertySourceLoader");
  }

  @Test
  void loadSupportsYamlDocumentsAndUnicode() throws IOException {
    environment.put("VAR1", "name: 中文\n---\nname: 第二份\n");
    var loaded = createResource("VAR1", yamlLoader).load();
    assertThat(loaded).hasSize(2);
    assertThat(loaded.get(0).getProperty("name")).isEqualTo("中文");
    assertThat(loaded.get(1).getProperty("name")).isEqualTo("第二份");
  }

  private SystemEnvironmentConfigDataResource createResource(String variableName) {
    return createResource(variableName, this.propertiesLoader);
  }

  private SystemEnvironmentConfigDataResource createResource(String variableName, PropertySourceLoader loader) {
    return new SystemEnvironmentConfigDataResource(variableName, loader, environment::get, null);
  }

  private SystemEnvironmentConfigDataResource createResource(String variableName,
          PropertySourceLoader loader, Charset charset) {
    return new SystemEnvironmentConfigDataResource(variableName, loader, environment::get, null, charset);
  }

}
