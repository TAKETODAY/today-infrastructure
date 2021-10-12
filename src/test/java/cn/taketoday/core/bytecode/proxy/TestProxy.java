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
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.bytecode.proxysample.ProxySampleInterface_ReturnsBasic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Nokleberg
 * <a href="mailto:chris@nokleberg.com">chris@nokleberg.com</a>
 * @version $Id: TestProxy.java,v 1.6 2012/07/27 16:02:49 baliuka Exp $
 */
public class TestProxy {

  private class SimpleInvocationHandler implements InvocationHandler {
    Object o = null;

    public SimpleInvocationHandler(Object o) {
      this.o = o;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
      System.out.println("invoking " + m + " on " + o + " with " + args);
      Object r = m.invoke(o, args);
      System.out.println("done: " + m + " on " + o + " with " + args + ", result is " + r);
      return r;
    }
  }

  @Test
  public void testGetProxyClassAndConstructor() throws Exception {
    HashMap map = new HashMap();
    map.put("test", "test");
    InvocationHandler handler = new SimpleInvocationHandler(map);
    Class proxyClass = Proxy.getProxyClass(TestProxy.class.getClassLoader(), new Class[] { Map.class });
    Map proxyMap = (Map) proxyClass.getConstructor(new Class[] { InvocationHandler.class }).newInstance(new Object[] { handler });
    assertEquals(map.get("test"),
                 proxyMap.get("test"), "proxy delegation not correct");
  }

  @Test
  public void testGetProxyInstance() throws Exception {
    HashMap map = new HashMap();
    map.put("test", "test");
    InvocationHandler handler = new SimpleInvocationHandler(map);
    Map proxyMap = (Map) Proxy.newProxyInstance(TestProxy.class.getClassLoader(), new Class[] { Map.class }, handler);
    assertEquals(map.get("test"), proxyMap.get("test"), "proxy delegation not correct");
  }

  @Test
  public void testIsProxyClass() throws Exception {
    HashMap map = new HashMap();
    map.put("test", "test");
    InvocationHandler handler = new SimpleInvocationHandler(map);
    Map proxyMap = (Map) Proxy.newProxyInstance(TestProxy.class.getClassLoader(), new Class[] { Map.class }, handler);
    assertTrue(Proxy.isProxyClass(proxyMap.getClass()), "real proxy not accepted");
  }

  private class FakeProxy extends Proxy {
    public FakeProxy(InvocationHandler ih) {
      super(ih);
    }
  }

  @Test
  public void testIsNotProxyClass() throws Exception {
    assertFalse(Proxy.isProxyClass(FakeProxy.class), "fake proxy accepted as real");
  }

  private static class ReturnNullHandler implements InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      return null;
    }
  }

  @Test
  public void testReturnNull() throws Exception {
    System.err.println("hello");
    ProxySampleInterface_ReturnsBasic rb = (ProxySampleInterface_ReturnsBasic)
            Proxy.newProxyInstance(null, new Class[] { ProxySampleInterface_ReturnsBasic.class },
                                   new ReturnNullHandler());
    try {
      int result = rb.getKala(11);
      fail("must throw an exception, but returned " + result);
    }
    catch (NullPointerException ignore) { }
  }

  @Test
  public void testGetInvocationHandler() throws Exception {
    HashMap map = new HashMap();
    map.put("test", "test");
    InvocationHandler handler = new InvocationHandler() {
      public Object invoke(Object o, Method method, Object[] args) throws Exception {
        throw new Exception("test!");
      }
    };
    Map proxyMap = (Map) Proxy.newProxyInstance(TestProxy.class.getClassLoader(), new Class[] { Map.class }, handler);
    assertSame(handler, Proxy.getInvocationHandler(proxyMap), "should be the same handler");
  }

  @Test
  public void testException() throws Exception {
    HashMap map = new HashMap();
    map.put("test", "test");
    InvocationHandler handler = new InvocationHandler() {
      public Object invoke(Object o, Method method, Object[] args) throws Exception {
        throw new Exception("test!");
      }
    };
    Map proxyMap = (Map) Proxy.newProxyInstance(TestProxy.class.getClassLoader(), new Class[] { Map.class }, handler);
    try {
      proxyMap.get("test"); // should throw exception
      fail("proxy exception handling not correct, should throw exception");
    }
    catch (UndeclaredThrowableException e) {
      System.out.println("exception: " + e);
    }
    catch (Exception e) {
      fail("proxy exception handling not correct, threw wrong exception: " + e);
    }
  }

  @Test
  public void testEquals() throws Exception {
    final Object k1 = new Object();
    final Object k2 = new Object();
    InvocationHandler handler = new InvocationHandler() {
      public Object invoke(Object o, Method method, Object[] args) throws Exception {
        if (method.getName().equals("equals")) {
          return (args[0] == k1) ? Boolean.TRUE : Boolean.FALSE;
        }
        return null;
      }
    };
    Object proxy = Proxy.newProxyInstance(TestProxy.class.getClassLoader(), new Class[] { Map.class }, handler);
    assertEquals(proxy, k1);
    assertNotEquals(proxy, k2);
  }

  public void perform(ClassLoader loader) throws Throwable {
    InvocationHandler handler = new InvocationHandler() {
      public Object invoke(Object o, Method method, Object[] args) throws Exception {
        throw new Exception("test!");
      }
    };
    Proxy.newProxyInstance(loader, new Class[] { Map.class }, handler);
  }

}
