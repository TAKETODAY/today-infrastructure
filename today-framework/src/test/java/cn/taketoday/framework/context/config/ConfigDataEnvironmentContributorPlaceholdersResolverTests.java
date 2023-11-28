/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginLookup;
import cn.taketoday.origin.PropertySourceOrigin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ConfigDataEnvironmentContributorPlaceholdersResolver}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataEnvironmentContributorPlaceholdersResolverTests {

  @Test
  void resolvePlaceholdersWhenNotStringReturnsResolved() {
    ConfigDataEnvironmentContributorPlaceholdersResolver resolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
            Collections.emptyList(), null, null, false);
    assertThat(resolver.resolvePlaceholders(123)).isEqualTo(123);
  }

  @Test
  void resolvePlaceholdersWhenNotFoundReturnsOriginal() {
    ConfigDataEnvironmentContributorPlaceholdersResolver resolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
            Collections.emptyList(), null, null, false);
    assertThat(resolver.resolvePlaceholders("${test}")).isEqualTo("${test}");
  }

  @Test
  void resolvePlaceholdersWhenFoundReturnsFirstMatch() {
    List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>();
    contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s1", "nope", "t1"), true));
    contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s2", "test", "t2"), true));
    contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s3", "test", "t3"), true));
    ConfigDataEnvironmentContributorPlaceholdersResolver resolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
            contributors, null, null, true);
    assertThat(resolver.resolvePlaceholders("${test}")).isEqualTo("t2");
  }

  @Test
  void resolvePlaceholdersWhenFoundInInactiveThrowsException() {
    List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>();
    contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s1", "nope", "t1"), true));
    contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s2", "test", "t2"), true));
    contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s3", "test", "t3"), false));
    ConfigDataEnvironmentContributorPlaceholdersResolver resolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
            contributors, null, null, true);
    assertThatExceptionOfType(InactiveConfigDataAccessException.class)
            .isThrownBy(() -> resolver.resolvePlaceholders("${test}"))
            .satisfies(propertyNameAndOriginOf("test", "s3"));
  }

  @Test
  void resolvePlaceholderWhenFoundInInactiveAndIgnoringReturnsResolved() {
    List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>();
    contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s1", "nope", "t1"), true));
    contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s2", "test", "t2"), true));
    contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s3", "test", "t3"), false));
    ConfigDataEnvironmentContributorPlaceholdersResolver resolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
            contributors, null, null, false);
    assertThat(resolver.resolvePlaceholders("${test}")).isEqualTo("t2");
  }

  private Consumer<InactiveConfigDataAccessException> propertyNameAndOriginOf(String propertyName, String origin) {
    return (ex) -> {
      assertThat(ex.getPropertyName()).isEqualTo(propertyName);
      assertThat(((PropertySourceOrigin) (ex.getOrigin())).getPropertySource().getName()).isEqualTo(origin);
    };
  }

  static class TestPropertySource extends MapPropertySource implements OriginLookup<String> {

    TestPropertySource(String name, String key, String value) {
      this(name, Collections.singletonMap(key, value));
    }

    TestPropertySource(String name, Map<String, Object> source) {
      super(name, source);
    }

    @Override
    public Origin getOrigin(String key) {
      if (getSource().containsKey(key)) {
        return new PropertySourceOrigin(this, key);
      }
      return null;
    }

  }

  static class TestConfigDataEnvironmentContributor extends ConfigDataEnvironmentContributor {

    private final boolean active;

    protected TestConfigDataEnvironmentContributor(PropertySource<?> propertySource, boolean active) {
      super(Kind.ROOT, null, null, false, propertySource, null, null, null, null);
      this.active = active;
    }

    @Override
    boolean isActive(ConfigDataActivationContext activationContext) {
      return this.active;
    }

  }

}
