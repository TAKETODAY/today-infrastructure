/*
 * Copyright 2004 The Apache Software Foundation
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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestInterfaceMaker {

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
