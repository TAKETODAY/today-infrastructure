/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.context.testfixture.origin.MockOrigin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OriginLookup}.
 *
 * @author Phillip Webb
 */
class OriginLookupTests {

  @Test
  void getOriginWhenSourceIsNullShouldReturnNull() {
    assertThat(OriginLookup.getOrigin(null, "foo")).isNull();
  }

  @Test
  void getOriginWhenSourceIsNotLookupShouldReturnLookupOrigin() {
    Object source = new Object();
    assertThat(OriginLookup.getOrigin(source, "foo")).isNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  void getOriginWhenSourceIsLookupShouldReturnLookupOrigin() {
    OriginLookup<String> source = mock(OriginLookup.class);
    Origin origin = MockOrigin.of("bar");
    given(source.getOrigin("foo")).willReturn(origin);
    assertThat(OriginLookup.getOrigin(source, "foo")).isEqualTo(origin);
  }

  @Test
  @SuppressWarnings("unchecked")
  void getOriginWhenSourceLookupThrowsAndErrorShouldReturnNull() {
    OriginLookup<String> source = mock(OriginLookup.class);
    willThrow(RuntimeException.class).given(source).getOrigin("foo");
    assertThat(OriginLookup.getOrigin(source, "foo")).isNull();
  }

}
