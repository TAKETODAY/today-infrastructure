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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import infra.aot.hint.ResourcePatternHint;
import infra.aot.hint.ResourcePatternHints;
import infra.aot.hint.RuntimeHints;
import infra.app.env.PropertiesPropertySourceLoader;
import infra.app.env.PropertySourceLoader;
import infra.app.env.YamlPropertySourceLoader;
import infra.core.test.io.support.MockTodayStrategies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/3 22:33
 */
class ConfigDataLocationRuntimeHintsTests {

  @Test
  void registerWithDefaultSettings() {
    RuntimeHints hints = new RuntimeHints();
    new TestConfigDataLocationRuntimeHints().registerHints(hints, null);
    assertThat(hints.resources().resourcePatternHints()).singleElement()
            .satisfies(includes("application*.properties", "application*.xml", "application*.yaml", "application*.yml",
                    "config/application*.properties", "config/application*.xml", "config/application*.yaml",
                    "config/application*.yml"));
  }

  @Test
  void registerWithCustomName() {
    RuntimeHints hints = new RuntimeHints();
    new TestConfigDataLocationRuntimeHints() {
      @Override
      protected List<String> getFileNames(ClassLoader classLoader) {
        return List.of("test");
      }

    }.registerHints(hints, null);
    assertThat(hints.resources().resourcePatternHints()).singleElement()
            .satisfies(includes("test*.properties", "test*.xml", "test*.yaml", "test*.yml", "config/test*.properties",
                    "config/test*.xml", "config/test*.yaml", "config/test*.yml"));
  }

  @Test
  void registerWithCustomLocation() {
    RuntimeHints hints = new RuntimeHints();
    new TestConfigDataLocationRuntimeHints() {
      @Override
      protected List<String> getLocations(ClassLoader classLoader) {
        return List.of("config/");
      }
    }.registerHints(hints, null);
    assertThat(hints.resources().resourcePatternHints()).singleElement()
            .satisfies(includes("config/application*.properties", "config/application*.xml", "config/application*.yaml",
                    "config/application*.yml"));
  }

  @Test
  void registerWithCustomExtension() {
    RuntimeHints hints = new RuntimeHints();
    new ConfigDataLocationRuntimeHints() {
      @Override
      protected List<String> getExtensions(ClassLoader classLoader) {
        return List.of(".conf");
      }
    }.registerHints(hints, null);
    assertThat(hints.resources().resourcePatternHints()).singleElement()
            .satisfies(includes("application*.conf", "config/application*.conf"));
  }

  @Test
  void registerWithUnknownLocationDoesNotAddHint() {
    RuntimeHints hints = new RuntimeHints();
    new ConfigDataLocationRuntimeHints() {
      @Override
      protected List<String> getLocations(ClassLoader classLoader) {
        return List.of(UUID.randomUUID().toString());
      }
    }.registerHints(hints, null);
    assertThat(hints.resources().resourcePatternHints()).isEmpty();
  }

  private Consumer<ResourcePatternHints> includes(String... patterns) {
    return (hint) -> {
      assertThat(hint.getIncludes().stream().map(ResourcePatternHint::getPattern)).contains(patterns);
    };
  }

  static class TestConfigDataLocationRuntimeHints extends ConfigDataLocationRuntimeHints {

    private final MockTodayStrategies strategies;

    TestConfigDataLocationRuntimeHints(MockTodayStrategies strategies) {
      this.strategies = strategies;
    }

    TestConfigDataLocationRuntimeHints() {
      this(loader());
    }

    private static MockTodayStrategies loader() {
      MockTodayStrategies springFactoriesLoader = new MockTodayStrategies();
      springFactoriesLoader.add(PropertySourceLoader.class, PropertiesPropertySourceLoader.class,
              YamlPropertySourceLoader.class);
      return springFactoriesLoader;
    }

    @Override
    protected MockTodayStrategies getLoaderStrategies(ClassLoader classLoader) {
      return this.strategies;
    }

  }

}