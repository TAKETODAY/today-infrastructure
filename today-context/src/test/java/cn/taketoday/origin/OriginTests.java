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

import java.util.List;

import cn.taketoday.context.testfixture.origin.MockOrigin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Origin}.
 *
 * @author Phillip Webb
 */
class OriginTests {

  @Test
  void getParentWhenDefaultIsNull() {
    Origin origin = new Origin() {
    };
    assertThat(origin.getParent()).isNull();
  }

  @Test
  void fromWhenSourceIsNullReturnsNull() {
    assertThat(Origin.from(null)).isNull();
  }

  @Test
  void fromWhenSourceIsRegularObjectReturnsNull() {
    Object source = new Object();
    assertThat(Origin.from(source)).isNull();
  }

  @Test
  void fromWhenSourceIsOriginReturnsSource() {
    Origin origin = mock(Origin.class);
    assertThat(Origin.from(origin)).isEqualTo(origin);
  }

  @Test
  void fromWhenSourceIsOriginProviderReturnsProvidedOrigin() {
    Origin origin = mock(Origin.class);
    OriginProvider originProvider = mock(OriginProvider.class);
    given(originProvider.getOrigin()).willReturn(origin);
    assertThat(Origin.from(origin)).isEqualTo(origin);
  }

  @Test
  void fromWhenSourceIsThrowableUsesCause() {
    Origin origin = mock(Origin.class);
    Exception exception = new RuntimeException(new TestException(origin, null));
    assertThat(Origin.from(exception)).isEqualTo(origin);
  }

  @Test
  void fromWhenSourceIsThrowableAndOriginProviderThatReturnsNullUsesCause() {
    Origin origin = mock(Origin.class);
    Exception exception = new TestException(null, new TestException(origin, null));
    assertThat(Origin.from(exception)).isEqualTo(origin);
  }

  @Test
  void parentsFromWhenSourceIsNullReturnsEmptyList() {
    assertThat(Origin.parentsFrom(null)).isEmpty();
  }

  @Test
  void parentsFromReturnsParents() {
    Origin o1 = MockOrigin.of("1");
    Origin o2 = MockOrigin.of("2", o1);
    Origin o3 = MockOrigin.of("3", o2);
    List<Origin> parents = Origin.parentsFrom(o3);
    assertThat(parents).containsExactly(o2, o1);
  }

  @SuppressWarnings("serial")
  static class TestException extends RuntimeException implements OriginProvider {

    private final Origin origin;

    TestException(Origin origin, Throwable cause) {
      super(cause);
      this.origin = origin;
    }

    @Override
    public Origin getOrigin() {
      return this.origin;
    }

  }

}
