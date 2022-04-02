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

package cn.taketoday.context.properties.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigurationPropertyNameAliases}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigurationPropertyNameAliasesTests {

  @Test
  void createWithStringWhenNullNameShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigurationPropertyNameAliases((String) null))
            .withMessageContaining("Name must not be null");
  }

  @Test
  void createWithStringShouldAddMapping() {
    ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases("foo", "bar", "baz");
    assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
            .containsExactly(ConfigurationPropertyName.of("bar"), ConfigurationPropertyName.of("baz"));
  }

  @Test
  void createWithNameShouldAddMapping() {
    ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases(
            ConfigurationPropertyName.of("foo"), ConfigurationPropertyName.of("bar"),
            ConfigurationPropertyName.of("baz"));
    assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
            .containsExactly(ConfigurationPropertyName.of("bar"), ConfigurationPropertyName.of("baz"));
  }

  @Test
  void addAliasesFromStringShouldAddMapping() {
    ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
    aliases.addAliases("foo", "bar", "baz");
    assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
            .containsExactly(ConfigurationPropertyName.of("bar"), ConfigurationPropertyName.of("baz"));
  }

  @Test
  void addAliasesFromNameShouldAddMapping() {
    ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
    aliases.addAliases(ConfigurationPropertyName.of("foo"), ConfigurationPropertyName.of("bar"),
            ConfigurationPropertyName.of("baz"));
    assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
            .containsExactly(ConfigurationPropertyName.of("bar"), ConfigurationPropertyName.of("baz"));
  }

  @Test
  void addWhenHasExistingShouldAddAdditionalMappings() {
    ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
    aliases.addAliases("foo", "bar");
    aliases.addAliases("foo", "baz");
    assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
            .containsExactly(ConfigurationPropertyName.of("bar"), ConfigurationPropertyName.of("baz"));
  }

  @Test
  void getAliasesWhenNotMappedShouldReturnEmptyList() {
    ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
    assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo"))).isEmpty();
  }

  @Test
  void getAliasesWhenMappedShouldReturnMapping() {
    ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
    aliases.addAliases("foo", "bar");
    assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
            .containsExactly(ConfigurationPropertyName.of("bar"));
  }

  @Test
  void getNameForAliasWhenHasMappingShouldReturnName() {
    ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
    aliases.addAliases("foo", "bar");
    aliases.addAliases("foo", "baz");
    assertThat((Object) aliases.getNameForAlias(ConfigurationPropertyName.of("bar")))
            .isEqualTo(ConfigurationPropertyName.of("foo"));
    assertThat((Object) aliases.getNameForAlias(ConfigurationPropertyName.of("baz")))
            .isEqualTo(ConfigurationPropertyName.of("foo"));
  }

  @Test
  void getNameForAliasWhenNotMappedShouldReturnNull() {
    ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
    aliases.addAliases("foo", "bar");
    assertThat((Object) aliases.getNameForAlias(ConfigurationPropertyName.of("baz"))).isNull();
  }

}
