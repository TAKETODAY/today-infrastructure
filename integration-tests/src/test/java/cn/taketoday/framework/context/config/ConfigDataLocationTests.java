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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import cn.taketoday.origin.Origin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLocation}.
 *
 * @author Phillip Webb
 */
class ConfigDataLocationTests {

  @Test
  void isOptionalWhenNotPrefixedWithOptionalReturnsFalse() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("test");
    assertThat(location.isOptional()).isFalse();
  }

  @Test
  void isOptionalWhenPrefixedWithOptionalReturnsTrue() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("optional:test");
    assertThat(location.isOptional()).isTrue();
  }

  @Test
  void getValueWhenNotPrefixedWithOptionalReturnsValue() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("test");
    assertThat(location.getValue()).isEqualTo("test");
  }

  @Test
  void getValueWhenPrefixedWithOptionalReturnsValueWithoutPrefix() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("optional:test");
    assertThat(location.getValue()).isEqualTo("test");
  }

  @Test
  void hasPrefixWhenPrefixedReturnsTrue() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("optional:test:path");
    assertThat(location.hasPrefix("test:")).isTrue();
  }

  @Test
  void hasPrefixWhenNotPrefixedReturnsFalse() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("optional:file:path");
    assertThat(location.hasPrefix("test:")).isFalse();
  }

  @Test
  void getNonPrefixedValueWhenPrefixedReturnsNonPrefixed() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("optional:test:path");
    assertThat(location.getNonPrefixedValue("test:")).isEqualTo("path");
  }

  @Test
  void getNonPrefixedValueWhenNotPrefixedReturnsOriginalValue() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("optional:file:path");
    assertThat(location.getNonPrefixedValue("test:")).isEqualTo("file:path");
  }

  @Test
  void getOriginWhenNoOriginReturnsNull() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("test");
    assertThat(location.getOrigin()).isNull();
  }

  @Test
  void getOriginWhenWithOriginReturnsOrigin() {
    Origin origin = mock(Origin.class);
    ConfigDataLocation location = ConfigDataLocation.valueOf("test").withOrigin(origin);
    assertThat(location.getOrigin()).isSameAs(origin);
  }

  @Test
  void equalsAndHashCode() {
    ConfigDataLocation l1 = ConfigDataLocation.valueOf("a");
    ConfigDataLocation l2 = ConfigDataLocation.valueOf("a");
    ConfigDataLocation l3 = ConfigDataLocation.valueOf("optional:a");
    ConfigDataLocation l4 = ConfigDataLocation.valueOf("b");
    assertThat(l1.hashCode()).isEqualTo(l2.hashCode()).isEqualTo(l3.hashCode());
    assertThat(l1).isEqualTo(l2).isEqualTo(l3).isNotEqualTo(l4);
  }

  @Test
  void toStringReturnsOriginalString() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("optional:test");
    assertThat(location).hasToString("optional:test");
  }

  @Test
  void withOriginSetsOrigin() {
    Origin origin = mock(Origin.class);
    ConfigDataLocation location = ConfigDataLocation.valueOf("test").withOrigin(origin);
    assertThat(location.getOrigin()).isSameAs(origin);
  }

  @Test
  void ofWhenNullValueReturnsNull() {
    assertThat(ConfigDataLocation.valueOf(null)).isNull();
  }

  @Test
  void ofWhenEmptyValueReturnsNull() {
    assertThat(ConfigDataLocation.valueOf("")).isNull();
  }

  @Test
  void ofWhenEmptyOptionalValueReturnsNull() {
    assertThat(ConfigDataLocation.valueOf("optional:")).isNull();
  }

  @Test
  void ofReturnsLocation() {
    assertThat(ConfigDataLocation.valueOf("test")).hasToString("test");
  }

  @Test
  void splitWhenNoSemiColonReturnsSingleElement() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("test");
    ConfigDataLocation[] split = location.split();
    Assertions.assertThat(split).containsExactly(ConfigDataLocation.valueOf("test"));
  }

  @Test
  void splitWhenSemiColonReturnsElements() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("one;two;three");
    ConfigDataLocation[] split = location.split();
    Assertions.assertThat(split).containsExactly(ConfigDataLocation.valueOf("one"), ConfigDataLocation.valueOf("two"),
            ConfigDataLocation.valueOf("three"));
  }

  @Test
  void splitOnCharReturnsElements() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("one::two::three");
    ConfigDataLocation[] split = location.split("::");
    Assertions.assertThat(split).containsExactly(ConfigDataLocation.valueOf("one"), ConfigDataLocation.valueOf("two"),
            ConfigDataLocation.valueOf("three"));
  }

  @Test
  void splitWhenHasOriginReturnsElementsWithOriginSet() {
    Origin origin = mock(Origin.class);
    ConfigDataLocation location = ConfigDataLocation.valueOf("a;b").withOrigin(origin);
    ConfigDataLocation[] split = location.split();
    assertThat(split[0].getOrigin()).isEqualTo(origin);
    assertThat(split[1].getOrigin()).isEqualTo(origin);
  }

}
