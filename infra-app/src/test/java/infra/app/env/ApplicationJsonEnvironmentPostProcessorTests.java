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

package infra.app.env;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.app.json.JsonParseException;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.PropertySource;
import infra.core.env.StandardEnvironment;
import infra.mock.env.MockPropertySource;
import infra.origin.PropertySourceOrigin;
import infra.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ApplicationJsonEnvironmentPostProcessor}.
 *
 * @author Dave Syer
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Artsiom Yudovin
 */
class ApplicationJsonEnvironmentPostProcessorTests {

  private final ApplicationJsonEnvironmentPostProcessor processor = new ApplicationJsonEnvironmentPostProcessor();

  private final ConfigurableEnvironment environment = new StandardEnvironment();

  @Test
  void error() {
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEmpty();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "infra.application.json=foo:bar");
    assertThatExceptionOfType(JsonParseException.class)
            .isThrownBy(() -> this.processor.postProcessEnvironment(this.environment, null))
            .withMessageContaining("Cannot parse JSON");
  }

  @Test
  void missing() {
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEmpty();
    this.processor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEmpty();
  }

  @Test
  void empty() {
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEmpty();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "infra.application.json={}");
    this.processor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEmpty();
  }

  @Test
  void periodSeparated() {
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEmpty();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
            "infra.application.json={\"foo\":\"bar\"}");
    this.processor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEqualTo("bar");
  }

  @Test
  void envVar() {
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEmpty();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
            "INFRA_APPLICATION_JSON={\"foo\":\"bar\"}");
    this.processor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEqualTo("bar");
  }

  @Test
  void nested() {
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEmpty();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
            "INFRA_APPLICATION_JSON={\"foo\":{\"bar\":\"spam\",\"rab\":\"maps\"}}");
    this.processor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.resolvePlaceholders("${foo.bar:}")).isEqualTo("spam");
    assertThat(this.environment.resolvePlaceholders("${foo.rab:}")).isEqualTo("maps");
  }

  @Test
  void prefixed() {
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEmpty();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
            "INFRA_APPLICATION_JSON={\"foo.bar\":\"spam\"}");
    this.processor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.resolvePlaceholders("${foo.bar:}")).isEqualTo("spam");
  }

  @Test
  void list() {
    assertThat(this.environment.resolvePlaceholders("${foo[1]:}")).isEmpty();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
            "INFRA_APPLICATION_JSON={\"foo\":[\"bar\",\"spam\"]}");
    this.processor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.resolvePlaceholders("${foo[1]:}")).isEqualTo("spam");
  }

  @Test
  void listOfObject() {
    assertThat(this.environment.resolvePlaceholders("${foo[0].bar:}")).isEmpty();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
            "INFRA_APPLICATION_JSON={\"foo\":[{\"bar\":\"spam\"}]}");
    this.processor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.resolvePlaceholders("${foo[0].bar:}")).isEqualTo("spam");
  }

  @Test
  void propertySourceShouldTrackOrigin() {
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEmpty();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
            "infra.application.json={\"foo\":\"bar\"}");
    this.processor.postProcessEnvironment(this.environment, null);
    PropertySource<?> propertySource = this.environment.getPropertySources().get("infra.application.json");
    PropertySourceOrigin origin = (PropertySourceOrigin) PropertySourceOrigin.get(propertySource, "foo");
    assertThat(origin.getPropertySource().getName()).isEqualTo("Inlined Test Properties");
    assertThat(origin.getPropertyName()).isEqualTo("infra.application.json");
    assertThat(this.environment.resolvePlaceholders("${foo:}")).isEqualTo("bar");
  }

  @Test
  void nullValuesShouldBeAddedToPropertySource() {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
            "INFRA_APPLICATION_JSON={\"foo\":null}");
    this.processor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.containsProperty("foo")).isTrue();
  }

  @Test
  void emptyValuesForCollectionShouldNotBeIgnored() {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
            "INFRA_APPLICATION_JSON={\"foo\":[]}");
    MockPropertySource source = new MockPropertySource();
    source.setProperty("foo", "bar");
    this.environment.getPropertySources().addLast(source);
    assertThat(this.environment.resolvePlaceholders("${foo}")).isEqualTo("bar");
    this.environment.getPropertySources().addLast(source);
    this.processor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.resolvePlaceholders("${foo}")).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void emptyMapValuesShouldNotBeIgnored() {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
            "INFRA_APPLICATION_JSON={\"foo\":{}}");
    MockPropertySource source = new MockPropertySource();
    source.setProperty("foo.baz", "bar");
    this.environment.getPropertySources().addLast(source);
    this.processor.postProcessEnvironment(this.environment, null);
    assertThat(this.environment.getProperty("foo", Map.class)).isEmpty();
  }

}
