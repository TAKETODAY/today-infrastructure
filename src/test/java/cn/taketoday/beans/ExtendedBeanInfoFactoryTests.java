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

package cn.taketoday.beans;

import org.junit.jupiter.api.Test;

import java.beans.IntrospectionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ExtendedBeanInfoTests}.
 *
 * @author Chris Beams
 */
public class ExtendedBeanInfoFactoryTests {

  private final ExtendedBeanInfoFactory factory = new ExtendedBeanInfoFactory();

  @Test
  public void shouldNotSupportClassHavingOnlyVoidReturningSetter() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public void setFoo(String s) { }
    }
    assertThat(factory.getBeanInfo(C.class)).isNull();
  }

  @Test
  public void shouldSupportClassHavingNonVoidReturningSetter() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public C setFoo(String s) { return this; }
    }
    assertThat(factory.getBeanInfo(C.class)).isNotNull();
  }

  @Test
  public void shouldSupportClassHavingNonVoidReturningIndexedSetter() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public C setFoo(int i, String s) { return this; }
    }
    assertThat(factory.getBeanInfo(C.class)).isNotNull();
  }

  @Test
  public void shouldNotSupportClassHavingNonPublicNonVoidReturningIndexedSetter() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      void setBar(String s) { }
    }
    assertThat(factory.getBeanInfo(C.class)).isNull();
  }

  @Test
  public void shouldNotSupportClassHavingNonVoidReturningParameterlessSetter() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      C setBar() { return this; }
    }
    assertThat(factory.getBeanInfo(C.class)).isNull();
  }

  @Test
  public void shouldNotSupportClassHavingNonVoidReturningMethodNamedSet() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      C set(String s) { return this; }
    }
    assertThat(factory.getBeanInfo(C.class)).isNull();
  }

}
