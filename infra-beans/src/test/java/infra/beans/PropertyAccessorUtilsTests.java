/*
 * Copyright 2002-present the original author or authors.
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

package infra.beans;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 22:01
 */
@SuppressWarnings("removal")
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
    assertThat(PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex("map[key[0]].name")).isEqualTo(11);
    assertThat(PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex("a[b[c.d]]")).isEqualTo(-1);
  }

  @Test
  public void getLastNestedPropertySeparatorIndex() {
    assertThat(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex("[user]")).isEqualTo(-1);
    assertThat(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex("user.address.street")).isEqualTo(12);
    assertThat(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex("map[key[0]].name")).isEqualTo(11);
    assertThat(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex("d.c]b]a")).isEqualTo(-1);
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
  void matchesPropertyNeverThrowsForMalformedInput() {
    // Non-throwing, best-effort contract, same as canonicalPropertyName.
    assertThat(PropertyAccessorUtils.matchesProperty("map[key1]other", "map")).isFalse();
    assertThat(PropertyAccessorUtils.matchesProperty("map", "map[key1]other")).isFalse();
  }

  @Test
  public void canonicalPropertyName() {
    assertThat(PropertyAccessorUtils.canonicalPropertyName(null)).isEmpty();
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map")).isEqualTo("map");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1]")).isEqualTo("map[key1]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1']")).isEqualTo("map[key1]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"key1\"]")).isEqualTo("map[key1]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1][key2]")).isEqualTo("map[key1][key2]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1'][\"key2\"]")).isEqualTo("map[key1][key2]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1].name")).isEqualTo("map[key1].name");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1'].name")).isEqualTo("map[key1].name");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"key1\"].name")).isEqualTo("map[key1].name");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1]")).isEqualTo("map['key1]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"key1]")).isEqualTo("map[\"key1]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[']")).isEqualTo("map[']");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"]")).isEqualTo("map[\"]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"key[0]\"]")).isEqualTo("map[key[0]]");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key[0]'].name")).isEqualTo("map[key[0]].name");
    assertThat(PropertyAccessorUtils.canonicalPropertyName("users['admin[0]']")).isEqualTo("users[admin[0]]");
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
