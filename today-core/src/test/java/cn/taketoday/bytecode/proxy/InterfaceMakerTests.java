/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package cn.taketoday.bytecode.proxy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterfaceMakerTests {
  
  @Test
  public void testStandalone() throws Exception {
    InterfaceMaker im = new InterfaceMaker();
    im.add(D1.class);
    im.add(D2.class);
    Class<?> iface = im.create();
    Method[] methods = iface.getMethods();
    assertEquals(2, methods.length);
    String name1 = methods[0].getName();
    String name2 = methods[1].getName();
    assertTrue(("herby".equals(name1) && "derby".equals(name2)) || ("herby".equals(name2) && "derby".equals(name1)));
  }

  @Test
  public void testEnhancer() throws Exception {
    InterfaceMaker im = new InterfaceMaker();
    im.add(D1.class);
    im.add(D2.class);
    Class<?> iface = im.create();
    Object obj = Enhancer.create(Object.class, new Class[] { iface }, new MethodInterceptor() {
      public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) {
        return "test";
      }
    });
    Method method = obj.getClass().getMethod("herby", (Class[]) null);
    assertTrue("test".equals(method.invoke(obj, (Object[]) null)));
  }

}
