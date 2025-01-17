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

package infra.app.test.json;

import com.jayway.jsonpath.Configuration;

import org.junit.jupiter.api.Test;

import infra.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JsonContent}.
 *
 * @author Phillip Webb
 */
class JsonContentTests {

  private static final String JSON = "{\"name\":\"spring\", \"age\":100}";

  private static final ResolvableType TYPE = ResolvableType.forClass(ExampleObject.class);

  @Test
  void createWhenResourceLoadClassIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new JsonContent<ExampleObject>(null, TYPE, JSON, Configuration.defaultConfiguration()))
            .withMessageContaining("ResourceLoadClass is required");
  }

  @Test
  void createWhenJsonIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(
                    () -> new JsonContent<ExampleObject>(getClass(), TYPE, null, Configuration.defaultConfiguration()))
            .withMessageContaining("JSON is required");
  }

  @Test
  void createWhenConfigurationIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new JsonContent<ExampleObject>(getClass(), TYPE, JSON, null))
            .withMessageContaining("Configuration is required");
  }

  @Test
  void createWhenTypeIsNullShouldCreateContent() {
    JsonContent<ExampleObject> content = new JsonContent<>(getClass(), null, JSON,
            Configuration.defaultConfiguration());
    assertThat(content).isNotNull();
  }

  @Test
  @SuppressWarnings("deprecation")
  void assertThatShouldReturnJsonContentAssert() {
    JsonContent<ExampleObject> content = new JsonContent<>(getClass(), TYPE, JSON,
            Configuration.defaultConfiguration());
    assertThat(content.assertThat()).isInstanceOf(JsonContentAssert.class);
  }

  @Test
  void getJsonShouldReturnJson() {
    JsonContent<ExampleObject> content = new JsonContent<>(getClass(), TYPE, JSON,
            Configuration.defaultConfiguration());
    assertThat(content.getJson()).isEqualTo(JSON);

  }

  @Test
  void toStringWhenHasTypeShouldReturnString() {
    JsonContent<ExampleObject> content = new JsonContent<>(getClass(), TYPE, JSON,
            Configuration.defaultConfiguration());
    assertThat(content.toString()).isEqualTo("JsonContent " + JSON + " created from " + TYPE);
  }

  @Test
  void toStringWhenHasNoTypeShouldReturnString() {
    JsonContent<ExampleObject> content = new JsonContent<>(getClass(), null, JSON,
            Configuration.defaultConfiguration());
    assertThat(content.toString()).isEqualTo("JsonContent " + JSON);
  }

}
