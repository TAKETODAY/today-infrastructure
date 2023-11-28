/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import cn.taketoday.aot.hint.ResourcePatternHint;
import cn.taketoday.aot.hint.ResourcePatternHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.core.test.io.support.MockTodayStrategies;
import cn.taketoday.framework.env.PropertiesPropertySourceLoader;
import cn.taketoday.framework.env.PropertySourceLoader;
import cn.taketoday.framework.env.YamlPropertySourceLoader;

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
      assertThat(hint.getExcludes()).isEmpty();
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