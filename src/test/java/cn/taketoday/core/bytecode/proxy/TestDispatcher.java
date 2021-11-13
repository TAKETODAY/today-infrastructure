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

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

    Callback[] callbacks = new Callback[] { new Dispatcher() {
      public Object loadObject() {
        return impls[0];
      }
    }, new Dispatcher() {
      public Object loadObject() {
        return impls[1];
      }
    }
    };

    Enhancer e = new Enhancer();
    e.setInterfaces(new Class[] { Foo.class, Bar.class });
    e.setCallbacks(callbacks);
    e.setCallbackFilter(new CallbackFilter() {
      public int accept(Method method) {
        return (method.getDeclaringClass().equals(Foo.class)) ? 0 : 1;
      }
    });
    Object obj = e.create();

    assertTrue(((Foo) obj).foo().equals("foo1"));
    assertTrue(((Bar) obj).bar().equals("bar1"));

    impls[0] = new Foo() {
      public String foo() {
        return "foo2";
      }
    };
    assertTrue(((Foo) obj).foo().equals("foo2"));
  }

  public void perform(ClassLoader loader) throws Throwable { }

  public void testFailOnMemoryLeak() throws Throwable { }

}
