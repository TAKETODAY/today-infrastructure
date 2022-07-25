/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jndi;

import org.junit.jupiter.api.Test;

import javax.naming.Context;
import javax.naming.NameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 08.07.2003
 */
public class JndiTemplateTests {

  @Test
  public void testLookupSucceeds() throws Exception {
    Object o = new Object();
    String name = "foo";
    final Context context = mock(Context.class);
    given(context.lookup(name)).willReturn(o);

    JndiTemplate jt = new JndiTemplate() {
      @Override
      protected Context createInitialContext() {
        return context;
      }
    };

    Object o2 = jt.lookup(name);
    assertThat(o2).isEqualTo(o);
    verify(context).close();
  }

  @Test
  public void testLookupFails() throws Exception {
    NameNotFoundException ne = new NameNotFoundException();
    String name = "foo";
    final Context context = mock(Context.class);
    given(context.lookup(name)).willThrow(ne);

    JndiTemplate jt = new JndiTemplate() {
      @Override
      protected Context createInitialContext() {
        return context;
      }
    };

    assertThatExceptionOfType(NameNotFoundException.class).isThrownBy(() ->
            jt.lookup(name));
    verify(context).close();
  }

  @Test
  public void testLookupReturnsNull() throws Exception {
    String name = "foo";
    final Context context = mock(Context.class);
    given(context.lookup(name)).willReturn(null);

    JndiTemplate jt = new JndiTemplate() {
      @Override
      protected Context createInitialContext() {
        return context;
      }
    };

    assertThatExceptionOfType(NameNotFoundException.class).isThrownBy(() ->
            jt.lookup(name));
    verify(context).close();
  }

  @Test
  public void testLookupFailsWithTypeMismatch() throws Exception {
    Object o = new Object();
    String name = "foo";
    final Context context = mock(Context.class);
    given(context.lookup(name)).willReturn(o);

    JndiTemplate jt = new JndiTemplate() {
      @Override
      protected Context createInitialContext() {
        return context;
      }
    };

    assertThatExceptionOfType(TypeMismatchNamingException.class).isThrownBy(() ->
            jt.lookup(name, String.class));
    verify(context).close();
  }

  @Test
  public void testBind() throws Exception {
    Object o = new Object();
    String name = "foo";
    final Context context = mock(Context.class);

    JndiTemplate jt = new JndiTemplate() {
      @Override
      protected Context createInitialContext() {
        return context;
      }
    };

    jt.bind(name, o);
    verify(context).bind(name, o);
    verify(context).close();
  }

  @Test
  public void testRebind() throws Exception {
    Object o = new Object();
    String name = "foo";
    final Context context = mock(Context.class);

    JndiTemplate jt = new JndiTemplate() {
      @Override
      protected Context createInitialContext() {
        return context;
      }
    };

    jt.rebind(name, o);
    verify(context).rebind(name, o);
    verify(context).close();
  }

  @Test
  public void testUnbind() throws Exception {
    String name = "something";
    final Context context = mock(Context.class);

    JndiTemplate jt = new JndiTemplate() {
      @Override
      protected Context createInitialContext() {
        return context;
      }
    };

    jt.unbind(name);
    verify(context).unbind(name);
    verify(context).close();
  }

}
