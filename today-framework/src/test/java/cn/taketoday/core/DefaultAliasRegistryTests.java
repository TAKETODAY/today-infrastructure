/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/9/30 22:58
 */
class DefaultAliasRegistryTests {

  private final DefaultAliasRegistry registry = new DefaultAliasRegistry();

  @Test
  void aliasChaining() {
    registry.registerAlias("test", "testAlias");
    registry.registerAlias("testAlias", "testAlias2");
    registry.registerAlias("testAlias2", "testAlias3");

    assertThat(registry.hasAlias("test", "testAlias")).isTrue();
    assertThat(registry.hasAlias("test", "testAlias2")).isTrue();
    assertThat(registry.hasAlias("test", "testAlias3")).isTrue();
    assertThat(registry.canonicalName("testAlias")).isEqualTo("test");
    assertThat(registry.canonicalName("testAlias2")).isEqualTo("test");
    assertThat(registry.canonicalName("testAlias3")).isEqualTo("test");
  }

  @Test
    // SPR-17191
  void aliasChainingWithMultipleAliases() {
    registry.registerAlias("name", "alias_a");
    registry.registerAlias("name", "alias_b");
    assertThat(registry.hasAlias("name", "alias_a")).isTrue();
    assertThat(registry.hasAlias("name", "alias_b")).isTrue();

    registry.registerAlias("real_name", "name");
    assertThat(registry.hasAlias("real_name", "name")).isTrue();
    assertThat(registry.hasAlias("real_name", "alias_a")).isTrue();
    assertThat(registry.hasAlias("real_name", "alias_b")).isTrue();

    registry.registerAlias("name", "alias_c");
    assertThat(registry.hasAlias("real_name", "name")).isTrue();
    assertThat(registry.hasAlias("real_name", "alias_a")).isTrue();
    assertThat(registry.hasAlias("real_name", "alias_b")).isTrue();
    assertThat(registry.hasAlias("real_name", "alias_c")).isTrue();
  }

  @Test
  void removeAlias() {
    registry.registerAlias("real_name", "nickname");
    assertThat(registry.hasAlias("real_name", "nickname")).isTrue();

    registry.removeAlias("nickname");
    assertThat(registry.hasAlias("real_name", "nickname")).isFalse();
  }

  @Test
  void isAlias() {
    registry.registerAlias("real_name", "nickname");
    assertThat(registry.isAlias("nickname")).isTrue();
    assertThat(registry.isAlias("real_name")).isFalse();
    assertThat(registry.isAlias("fake")).isFalse();
  }

  @Test
  void getAliases() {
    registry.registerAlias("test", "testAlias1");
    assertThat(registry.getAliases("test")).containsExactly("testAlias1");

    registry.registerAlias("testAlias1", "testAlias2");
    registry.registerAlias("testAlias2", "testAlias3");
    assertThat(registry.getAliases("test")).containsExactlyInAnyOrder("testAlias1", "testAlias2", "testAlias3");
    assertThat(registry.getAliases("testAlias1")).containsExactlyInAnyOrder("testAlias2", "testAlias3");
    assertThat(registry.getAliases("testAlias2")).containsExactly("testAlias3");

    assertThat(registry.getAliases("testAlias3")).isEmpty();
  }

}
