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

package cn.taketoday.jdbc.type;

import org.junit.jupiter.api.Test;

import cn.taketoday.lang.Enumerable;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/1 23:02
 */
class EnumerableEnumTypeHandlerTests {

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

  enum StringValue implements Enumerable {

    TEST1,
    TEST2;
  }

  @Data
  static class UserModel {

    Gender gender;

    StringValue stringValue;
  }

  @Test
  void test() {
    Class<?> valueType = EnumerableEnumTypeHandler.getValueType(Gender.class);
    assertThat(valueType).isNotNull().isEqualTo(Integer.class);

    assertThat(EnumerableEnumTypeHandler.getValueType(StringValue.class))
            .isEqualTo(String.class);

  }

}