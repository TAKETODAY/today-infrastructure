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

package cn.taketoday.framework.env;

import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.framework.json.JsonParseException;
import cn.taketoday.mock.env.MockPropertySource;
import cn.taketoday.origin.PropertySourceOrigin;
import cn.taketoday.test.context.support.TestPropertySourceUtils;

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
