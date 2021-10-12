/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.core.bytecode.proxy;

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
