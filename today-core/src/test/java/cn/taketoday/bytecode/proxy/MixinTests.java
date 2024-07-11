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
class MixinTests {

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

  @Test
  public void testNonOverride() throws Exception {
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
    for (PropertyDescriptor descriptor : descriptors) {
      if (descriptor.getReadMethod() != null) {
        getters.add(descriptor.getName());
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
