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

package infra.core.style;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Harry Yang 2021/10/11 14:54
 */
class ToStringBuilderTests {

  private SomeObject s1, s2, s3;

  @BeforeEach
  void setUp() throws Exception {
    s1 = new SomeObject() {
      @Override
      public String toString() {
        return "A";
      }
    };
    s2 = new SomeObject() {
      @Override
      public String toString() {
        return "B";
      }
    };
    s3 = new SomeObject() {
      @Override
      public String toString() {
        return "C";
      }
    };
  }

  @Test
  void defaultStyleMap() {
    final Map<String, String> map = getMap();
    Object stringy = new Object() {
      @Override
      public String toString() {
        return new ToStringBuilder(this).append("familyFavoriteSport", map).toString();
      }
    };
    assertThat(stringy.toString()).isEqualTo(("[ToStringBuilderTests.4@" + ObjectUtils.getIdentityHexString(stringy) +
            " familyFavoriteSport = map['Keri' -> 'Softball', 'Scot' -> 'Fishing', 'Keith' -> 'Flag Football']]"));
  }

  private Map<String, String> getMap() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("Keri", "Softball");
    map.put("Scot", "Fishing");
    map.put("Keith", "Flag Football");
    return map;
  }

  @Test
  void defaultStyleArray() {
    SomeObject[] array = new SomeObject[] { s1, s2, s3 };
    String str = new ToStringBuilder(array).toString();
    assertThat(str).isEqualTo(("[@" + ObjectUtils.getIdentityHexString(array) +
            " array<ToStringBuilderTests.SomeObject>[A, B, C]]"));
  }

  @Test
  void primitiveArrays() {
    int[] integers = new int[] { 0, 1, 2, 3, 4 };
    String str = new ToStringBuilder(integers).toString();
    assertThat(str).isEqualTo(("[@" + ObjectUtils.getIdentityHexString(integers) + " array<Integer>[0, 1, 2, 3, 4]]"));
  }

  @Test
  void appendList() {
    List<SomeObject> list = new ArrayList<>();
    list.add(s1);
    list.add(s2);
    list.add(s3);
    String str = new ToStringBuilder(this).append("myLetters", list).toString();
    assertThat(str).isEqualTo(("[ToStringBuilderTests@" + ObjectUtils.getIdentityHexString(this) + " myLetters = list[A, B, C]]"));
  }

  @Test
  void appendSet() {
    Set<SomeObject> set = new LinkedHashSet<>();
    set.add(s1);
    set.add(s2);
    set.add(s3);
    String str = new ToStringBuilder(this).append("myLetters", set).toString();
    assertThat(str).isEqualTo(("[ToStringBuilderTests@" + ObjectUtils.getIdentityHexString(this) + " myLetters = set[A, B, C]]"));
  }

  @Test
  void appendClass() {
    String str = new ToStringBuilder(this).append("myClass", this.getClass()).toString();
    assertThat(str).isEqualTo(("[ToStringBuilderTests@" + ObjectUtils.getIdentityHexString(this) +
            " myClass = ToStringBuilderTests]"));
  }

  @Test
  void appendMethod() throws Exception {
    String str = new ToStringBuilder(this)
            .append("myMethod", this.getClass().getDeclaredMethod("appendMethod")).toString();
    assertThat(str).isEqualTo(("[ToStringBuilderTests@" + ObjectUtils.getIdentityHexString(this) +
            " myMethod = appendMethod@ToStringBuilderTests]"));
  }

  public static class SomeObject {
  }

}
