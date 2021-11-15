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
package cn.taketoday.core.bytecode.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Nokleberg
 * @version $Id: TestDispatcher.java,v 1.6 2004/06/24 21:15:17 herbyderby Exp $
 */
public class TestDispatcher {
  interface Foo {
    String foo();
  }

  interface Bar {
    String bar();
  }

  @Test
  public void testSimple() throws Exception {
    final Object[] impls = new Object[] { (Foo) () -> "foo1", (Bar) () -> "bar1" };

    Callback[] callbacks = new Callback[] { (Dispatcher) () -> impls[0], (Dispatcher) () -> impls[1] };

    Enhancer e = new Enhancer();
    e.setInterfaces(Foo.class, Bar.class);
    e.setCallbacks(callbacks);
    e.setCallbackFilter(method -> (method.getDeclaringClass().equals(Foo.class)) ? 0 : 1);
    Object obj = e.create();

    assertEquals("foo1", ((Foo) obj).foo());
    assertEquals("bar1", ((Bar) obj).bar());

    impls[0] = (Foo) () -> "foo2";
    assertEquals("foo2", ((Foo) obj).foo());
  }

  public void perform(ClassLoader loader) throws Throwable { }

  public void testFailOnMemoryLeak() throws Throwable { }

}
