/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.
package infra.bytecode.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Nokleberg
 * @version $Id: TestDispatcher.java,v 1.6 2004/06/24 21:15:17 herbyderby Exp $
 */
class DispatcherTests {
  
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
