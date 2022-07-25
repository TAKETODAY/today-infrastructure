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

package cn.taketoday.origin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OriginTrackedValue}.
 *
 * @author Phillip Webb
 */
class OriginTrackedValueTests {

  @Test
  void getValueShouldReturnValue() {
    Object value = new Object();
    assertThat(OriginTrackedValue.of(value).getValue()).isEqualTo(value);
  }

  @Test
  void getOriginShouldReturnOrigin() {
    Object value = new Object();
    Origin origin = mock(Origin.class);
    assertThat(OriginTrackedValue.of(value, origin).getOrigin()).isEqualTo(origin);
  }

  @Test
  void toStringShouldReturnValueToString() {
    Object value = new Object();
    assertThat(OriginTrackedValue.of(value).toString()).isEqualTo(value.toString());
  }

  @Test
  void hashCodeAndEqualsShouldIgnoreOrigin() {
    Object value1 = new Object();
    OriginTrackedValue tracked1 = OriginTrackedValue.of(value1);
    OriginTrackedValue tracked2 = OriginTrackedValue.of(value1, mock(Origin.class));
    OriginTrackedValue tracked3 = OriginTrackedValue.of(new Object());
    assertThat(tracked1.hashCode()).isEqualTo(tracked2.hashCode());
    assertThat(tracked1).isEqualTo(tracked1).isEqualTo(tracked2).isNotEqualTo(tracked3);
  }

  @Test
  void ofWhenValueIsNullShouldReturnNull() {
    assertThat(OriginTrackedValue.of(null)).isNull();
    assertThat(OriginTrackedValue.of(null, mock(Origin.class))).isNull();
  }

  @Test
  void ofWhenValueIsCharSequenceShouldReturnCharSequence() {
    String value = "foo";
    OriginTrackedValue tracked = OriginTrackedValue.of(value);
    assertThat(tracked).isInstanceOf(CharSequence.class);
    CharSequence charSequence = (CharSequence) tracked;
    assertThat(charSequence.length()).isEqualTo(value.length());
    assertThat(charSequence.charAt(0)).isEqualTo(value.charAt(0));
    assertThat(charSequence.subSequence(0, 1)).isEqualTo(value.subSequence(0, 1));
  }

}
