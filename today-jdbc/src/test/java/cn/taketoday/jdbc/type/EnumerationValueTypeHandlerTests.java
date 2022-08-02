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

import cn.taketoday.beans.BeanProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/2 21:19
 */
class EnumerationValueTypeHandlerTests {
  enum StringCode {

    TEST1(1, "desc1"),
    TEST2(2, "desc2");

    @EnumerationValue
    final int code;

    final String desc;

    StringCode(int code, String desc) {
      this.code = code;
      this.desc = desc;
    }
  }

  enum StringValue {

    TEST1(1, "desc1"),
    TEST2(2, "desc2");

    final int value;

    final String desc;

    StringValue(int value, String desc) {
      this.value = value;
      this.desc = desc;
    }
  }

  enum StringValueNotFound {

    TEST1(1, "desc1"),
    TEST2(2, "desc2");

    final int value1;

    final String desc;

    StringValueNotFound(int value, String desc) {
      this.value1 = value;
      this.desc = desc;
    }
  }

  @Test
  void getAnnotatedProperty() {
    BeanProperty annotatedProperty = EnumerationValueTypeHandler.getAnnotatedProperty(StringCode.class);
    assertThat(annotatedProperty).isNotNull();
    assertThat(annotatedProperty.getType()).isEqualTo(int.class);

    //
    annotatedProperty = EnumerationValueTypeHandler.getAnnotatedProperty(StringValue.class);
    assertThat(annotatedProperty).isNotNull();
    assertThat(annotatedProperty.getType()).isEqualTo(int.class);

    assertThat(EnumerationValueTypeHandler.getAnnotatedProperty(StringValueNotFound.class)).isNull();

  }

}