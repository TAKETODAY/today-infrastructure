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
package cn.taketoday.bytecode.proxy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Chris Nokleberg, Bob Lee
 * @version $Id: TestProxyRefDispatcher.java,v 1.1 2004/12/10 08:48:43
 * herbyderby Exp $
 */
public class TestProxyRefDispatcher {

  interface Foo {
    String foo();
  }

  interface Bar {
    String bar();
  }

  @Test
  public void testSimple() throws Exception {
    final Object[] impls = new Object[] { new Foo() {
      public String foo() {
        return "foo1";
      }
    }, new Bar() {
      public String bar() {
        return "bar1";
      }
    }
    };

    final Object[] proxyReference = new Object[1];
    Callback[] callbacks = new Callback[] { new ProxyRefDispatcher() {
      public Object loadObject(Object proxy) {
        proxyReference[0] = proxy;
        return impls[0];
      }
    }, new ProxyRefDispatcher() {
      public Object loadObject(Object proxy) {
        proxyReference[0] = proxy;
        return impls[1];
      }
    }
    };

    Enhancer e = new Enhancer();
    e.setInterfaces(Foo.class, Bar.class);
    e.setCallbacks(callbacks);
    e.setCallbackFilter(new CallbackFilter() {
      public int accept(Method method) {
        return (method.getDeclaringClass().equals(Foo.class)) ? 0 : 1;
      }
    });
    Object obj = e.create();

    assertNull(proxyReference[0]);
    assertEquals("foo1", ((Foo) obj).foo());
    assertSame(obj, proxyReference[0]);
    proxyReference[0] = null;
    assertEquals("bar1", ((Bar) obj).bar());
    assertSame(obj, proxyReference[0]);
    proxyReference[0] = null;

    impls[0] = new Foo() {
      public String foo() {
        return "foo2";
      }
    };
    assertEquals("foo2", ((Foo) obj).foo());
    assertSame(obj, proxyReference[0]);
  }

  @Test
  public void testFailOnMemoryLeak() throws Throwable { }

}
