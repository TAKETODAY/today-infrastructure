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

package cn.taketoday.context.properties.bind.validation;

import org.junit.jupiter.api.Test;

import cn.taketoday.origin.MockOrigin;
import cn.taketoday.origin.Origin;
import cn.taketoday.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OriginTrackedFieldError}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class OriginTrackedFieldErrorTests {

  private static final FieldError FIELD_ERROR = new FieldError("foo", "bar", "faf");

  private static final Origin ORIGIN = MockOrigin.of("afile");

  @Test
  void ofWhenFieldErrorIsNullShouldReturnNull() {
    assertThat(OriginTrackedFieldError.of(null, ORIGIN)).isNull();
  }

  @Test
  void ofWhenOriginIsNullShouldReturnFieldErrorWithoutOrigin() {
    assertThat(OriginTrackedFieldError.of(FIELD_ERROR, null)).isSameAs(FIELD_ERROR);
  }

  @Test
  void ofShouldReturnOriginCapableFieldError() {
    FieldError fieldError = OriginTrackedFieldError.of(FIELD_ERROR, ORIGIN);
    assertThat(fieldError.getObjectName()).isEqualTo("foo");
    assertThat(fieldError.getField()).isEqualTo("bar");
    assertThat(Origin.from(fieldError)).isEqualTo(ORIGIN);
  }

  @Test
  void toStringShouldAddOrigin() {
    assertThat(OriginTrackedFieldError.of(FIELD_ERROR, ORIGIN).toString())
            .isEqualTo("Field error in object 'foo' on field 'bar': rejected value [null]"
                    + "; codes []; arguments []; default message [faf]; origin afile");
  }

}
