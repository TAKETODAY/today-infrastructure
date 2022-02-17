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

package cn.taketoday.beans;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 22:01
 */
class PropertyAccessorUtilsTests {

  @Test
  public void getPropertyName() {
    assertThat(PropertyAccessorUtils.getPropertyName("")).isEqualTo("");
    assertThat(PropertyAccessorUtils.getPropertyName("[user]")).isEqualTo("");
    assertThat(PropertyAccessorUtils.getPropertyName("user")).isEqualTo("user");
  }

  @Test
  public void isNestedOrIndexedProperty() {
    assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty(null)).isFalse();
    assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty("")).isFalse();
    assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty("user")).isFalse();

    assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty("[user]")).isTrue();
    assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty("user.name")).isTrue();
  }

  @Test
  public void getFirstNestedPropertySeparatorIndex() {
    assertThat(PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex("[user]")).isEqualTo(-1);
    assertThat(PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex("user.name")).isEqualTo(4);
  }

  @Test
  public void getLastNestedPropertySeparatorIndex() {
    assertThat(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex("[user]")).isEqualTo(-1);
    assertThat(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex("user.address.street")).isEqualTo(12);
  }

  @Test
  public void matchesProperty() {
    assertThat(PropertyAccessorUtils.matchesProperty("user", "email")).isFalse();
    assertThat(PropertyAccessorUtils.matchesProperty("username", "user")).isFalse();
    assertThat(PropertyAccessorUtils.matchesProperty("admin[user]", "user")).isFalse();

    assertThat(PropertyAccessorUtils.matchesProperty("user", "user")).isTrue();
    assertThat(PropertyAccessorUtils.matchesProperty("user[name]", "user")).isTrue();
  }

  @Test
  public void canonicalPropertyName() {
    assertThat(PropertyAccessorUtils.canonicalPropertyName(null)).isEqualTo("");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map")).isEqualTo("map");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1]")).isEqualTo("map[key1]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1']")).isEqualTo("map[key1]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"key1\"]")).isEqualTo("map[key1]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1][key2]")).isEqualTo("map[key1][key2]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1'][\"key2\"]")).isEqualTo("map[key1][key2]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1].name")).isEqualTo("map[key1].name");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1'].name")).isEqualTo("map[key1].name");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"key1\"].name")).isEqualTo("map[key1].name");
  }

  @Test
  public void canonicalPropertyNames() {
    assertThat(PropertyAccessorUtils.canonicalPropertyNames(null)).isNull();

    String[] original =
            new String[] { "map", "map[key1]", "map['key1']", "map[\"key1\"]", "map[key1][key2]",
                    "map['key1'][\"key2\"]", "map[key1].name", "map['key1'].name", "map[\"key1\"].name" };
    String[] canonical =
            new String[] { "map", "map[key1]", "map[key1]", "map[key1]", "map[key1][key2]",
                    "map[key1][key2]", "map[key1].name", "map[key1].name", "map[key1].name" };

    assertThat(PropertyAccessorUtils.canonicalPropertyNames(original)).isEqualTo(canonical);
  }

}
