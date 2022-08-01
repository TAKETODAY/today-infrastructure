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

package cn.taketoday.lang;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/1 21:13
 */
class EnumerableTests {

  enum Gender implements Enumerable<Integer> {

    MALE(1, "男"),
    FEMALE(0, "女");

    private final int value;
    private final String desc;

    Gender(int value, String desc) {
      this.value = value;
      this.desc = desc;
    }

    @Override
    public Integer getValue() {
      return value;
    }

    @Override
    public String getDescription() {
      return desc;
    }

  }

  @Test
  void introspect() {
    assertThat(Enumerable.of(Gender.class, 2)).isNull();
    assertThat(Enumerable.of(Gender.class, 1)).isEqualTo(Gender.MALE);
    assertThat(Enumerable.of(Gender.class, 0)).isEqualTo(Gender.FEMALE);

    //
    assertThat(Enumerable.of(Gender.class, 2, Gender.MALE)).isEqualTo(Gender.MALE);
    assertThat(Enumerable.of(Gender.class, 2, (Supplier<Gender>) () -> Gender.MALE)).isEqualTo(Gender.MALE);

  }

  @Test
  void getValue() {
    assertThat(Enumerable.getValue(Gender.class, "MALE")).isEqualTo(1);
    assertThat(Enumerable.getValue(Gender.class, "FEMALE")).isEqualTo(0);
    assertThat(Enumerable.getValue(Gender.class, "FEMALE_")).isNull();

  }

}