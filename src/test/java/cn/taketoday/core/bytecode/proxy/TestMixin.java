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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris Nokleberg
 * @version $Id: TestMixin.java,v 1.6 2012/07/27 16:02:49 baliuka Exp $
 */
public class TestMixin {

  @Test
  public void testSimple() throws Exception {
    Object obj = Mixin.create(new Class[] { DI1.class, DI2.class },
                              new Object[] { new D1(), new D2() });
    assertEquals("D1", ((DI1) obj).herby());
    assertEquals("D2", ((DI2) obj).derby());
  }

  @Test
  public void testDetermineInterfaces() throws Exception {
    Object obj = Mixin.create(new Object[] { new D1(), new D2() });
    Object obj2 = Mixin.create(new Object[] { new D1(), new D2() });
    assertEquals(obj.getClass(),
                 obj2.getClass(), "Mixin.create should use exactly the same class when called with same parameters");
    Object obj3 = Mixin.create(new Object[] { new D1(), new D4() });
    assertNotSame(obj.getClass(),
                  obj3.getClass(), "Mixin.create should use different classes for different parameters");
    assertEquals("D1", ((DI1) obj).herby());
    assertEquals("D2", ((DI2) obj).derby());
  }

  @Test
  public void testOverride() throws Exception {
    Object obj = Mixin.create(new Object[] { new D1(), new D4() });
    assertEquals("D1", ((DI1) obj).herby());
    assertEquals("D4", ((DI2) obj).derby());
  }

  @Test public void testNonOverride() throws Exception {
    Object obj = Mixin.create(new Object[] { new D4(), new D1() });
    assertTrue(((DI1) obj).herby().equals("D4"));
    assertTrue(((DI2) obj).derby().equals("D4"));
  }

  @Test
  public void testSubclass() throws Exception {
    Object obj = Mixin.create(new Object[] { new D3(), new D1() });
    assertTrue(((DI1) obj).herby().equals("D1"));
    assertTrue(((DI2) obj).derby().equals("D2"));
    assertTrue(((DI3) obj).extra().equals("D3"));
  }

  @Test
  public void testBeans() throws Exception {
    Object obj = Mixin.createBean(new Object[] { new DBean1(), new DBean2() });
    Set<?> getters = getGetters(obj.getClass());
    assertTrue(getters.size() == 3); // name, age, class
    assertTrue(getters.contains("name"));
    assertTrue(getters.contains("age"));
    assertTrue(!(obj instanceof DI1));
  }

  @Test
  public void testEverything() throws Exception {
    Mixin.Generator gen = new Mixin.Generator();
    gen.setStyle(Mixin.STYLE_EVERYTHING);
    gen.setDelegates(new Object[] { new DBean1(), new DBean2() });
    Object obj = gen.create();
    Set<?> getters = getGetters(obj.getClass());
    assertTrue(getters.size() == 3); // name, age, class
    assertTrue(obj instanceof DI1);
    assertTrue(new DBean1().herby().equals(((DI1) obj).herby()));
  }

  @Test
  public void testNullDelegates() throws Exception {
    Mixin.Generator gen = new Mixin.Generator();
    gen.setStyle(Mixin.STYLE_BEANS);
    gen.setClasses(new Class[] { DBean1.class, DBean2.class });
    Mixin mixin = gen.create();
    Object obj = mixin.newInstance(new Object[] { new DBean1(), new DBean2() });
  }

  @Test
  public void testVarArgs() throws Exception {
    Object obj = Mixin.create(new Object[] { new D1(), new D5() });
    assertEquals(((DI1) obj).herby(), "D1");
    assertEquals(((DI5) obj).vararg("1", "2"), 2);
  }

  private static Set<String> getGetters(Class<?> beanClass) throws Exception {
    Set<String> getters = new HashSet<>();
    PropertyDescriptor[] descriptors = Introspector.getBeanInfo(beanClass).getPropertyDescriptors();
    for (int i = 0; i < descriptors.length; i++) {
      if (descriptors[i].getReadMethod() != null) {
        getters.add(descriptors[i].getName());
      }
    }
    return getters;
  }

  @SuppressWarnings("unused")
  private static PropertyDescriptor getProperty(Class<?> beanClass, String property) throws Exception {
    Set<?> getters = new HashSet<>();
    PropertyDescriptor[] descriptors = Introspector.getBeanInfo(beanClass).getPropertyDescriptors();
    for (PropertyDescriptor descriptor : descriptors) {
      if (descriptor.getName().equals(property))
        return descriptor;
    }
    return null;
  }

  public void perform(ClassLoader loader) throws Throwable {
    Mixin.createBean(loader, new Object[] { new DBean1(), new DBean2() });
  }

}
