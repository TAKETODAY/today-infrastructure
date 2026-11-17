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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import infra.app.env.PropertiesPropertySourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SystemEnvironmentConfigDataLoader}.
 *
 * @author Moritz Halbritter
 */
class SystemEnvironmentConfigDataLoaderTests {

  private final ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class);

  private final Map<String, String> environment = new HashMap<>();

  private final SystemEnvironmentConfigDataLoader loader = new SystemEnvironmentConfigDataLoader();

  @Test
  void loadLoadsConfigData() throws IOException {
    environment.put("VAR1", "key1=value1");
    ConfigData data = loader.load(context, createResource("VAR1"));
    assertThat(data.getPropertySources()).hasSize(1);
    assertThat(data.getPropertySources().get(0).getProperty("key1")).isEqualTo("value1");
  }

  @Test
  void loadWhenNoContentThrowsException() {
    assertThatExceptionOfType(ConfigDataResourceNotFoundException.class)
            .isThrownBy(() -> loader.load(context, createResource("VAR1")))
            .withMessage("Config data resource 'system environment variable [VAR1] content "
                    + "loaded using PropertiesPropertySourceLoader' cannot be found");
  }

  private SystemEnvironmentConfigDataResource createResource(String variableName) {
    return new SystemEnvironmentConfigDataResource(variableName, new PropertiesPropertySourceLoader(),
            environment::get, null);
  }

}
