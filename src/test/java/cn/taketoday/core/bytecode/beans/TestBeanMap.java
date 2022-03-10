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
package cn.taketoday.core.bytecode.beans;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import cn.taketoday.core.bytecode.proxy.Callback;
import cn.taketoday.core.bytecode.proxy.CallbackFilter;
import cn.taketoday.core.bytecode.proxy.Dispatcher;
import cn.taketoday.core.bytecode.proxy.Enhancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBeanMap {

  public static class TestBean2 {
    private String foo;
  }

  public static class TestBeanMapBean {
    private String foo;
    private String bar = "x";
    protected String baz;
    private int quud;
    private int quick = 42;
    protected int quip;

    public String getFoo() {
      return foo;
    }

    public void setFoo(String value) {
      foo = value;
    }

    public String getBar() {
      return bar;
    }

    public void setBaz(String value) {
      baz = value;
    }

    public int getQuud() {
      return quud;
    }

    public void setQuud(int value) {
      quud = value;
    }

    public int getQuick() {
      return quick;
    }

    public void setQuip(int value) {
      quip = value;
    }
  }

  @Test
  public void testBeanMap() {
    TestBeanMapBean bean = new TestBeanMapBean();
    BeanMap map = BeanMap.create(bean);
    BeanMap map2 = BeanMap.create(bean);
    assertEquals(map.getClass(),
            map2.getClass(), "BeanMap.create should use exactly the same bean class when called multiple times");
    BeanMap map3 = BeanMap.create(new TestBean2());
    assertNotSame(map.getClass(),
            map3.getClass(), "BeanMap.create should use different classes for different beans");
    assertEquals(6, map.size());
    assertNull(map.get("foo"));
    map.put("foo", "FOO");
    assertEquals("FOO", map.get("foo"));
    assertEquals("FOO", bean.getFoo());
    assertEquals("x", map.get("bar"));
    assertEquals(42, ((Integer) map.get("quick")).intValue());
    map.put("quud", 13);
    assertEquals(13, bean.getQuud());

    assertEquals(map.getPropertyType("foo"), String.class);
    assertEquals(map.getPropertyType("quud"), Integer.TYPE);
    assertNull(map.getPropertyType("kdkkj"));
  }

  @Test
  public void testEntrySet() {
    TestBeanMapBean bean = new TestBeanMapBean();
    BeanMap map = BeanMap.create(bean);
    assertEquals(map.entrySet().size(), map.size());
  }

  @Test
  public void testNoUnderlyingBean() {
    BeanMap.Generator gen = new BeanMap.Generator();
    gen.setBeanClass(TestBeanMapBean.class);
    BeanMap map = gen.create();

    TestBeanMapBean bean = new TestBeanMapBean();
    assertNull(bean.getFoo());
    assertFalse(map.put(bean, "foo", "FOO") != null);
    assertEquals("FOO", bean.getFoo());
    assertEquals("FOO", map.get(bean, "foo"));
  }

  @Test
  public void testMixinMapIntoBean() {
    Object bean = new TestBeanMapBean();
    bean = mixinMapIntoBean(bean);
    ((TestBeanMapBean) bean).setFoo("hello");
    assertTrue(bean instanceof Map);
    assertEquals("hello", ((Map) bean).get("foo"));
  }

  @Test
  public void testRequire() {
    BeanMap.Generator gen = new BeanMap.Generator();
    gen.setBeanClass(TestBeanMapBean.class);
    gen.setRequire(BeanMap.REQUIRE_GETTER);
    BeanMap map = gen.create();
    assertTrue(map.containsKey("foo"));
    assertTrue(map.containsKey("bar"));
    assertFalse(map.containsKey("baz"));
  }

  // testContainsValue
  // -------------------------------------------

  public static class TestBeanFullGetters extends TestBeanMapBean {
    public String getBaz() {
      return baz;
    }

    public int getQuip() {
      return quip;
    }
  }

  @Test
  public void testContainsValue() {
    TestBeanFullGetters bean = new TestBeanFullGetters();
    BeanMap map = BeanMap.create(bean);
    assertTrue(map.containsValue(null));
    bean.setFoo("foo");
    bean.setBaz("baz");
    assertFalse(map.containsValue(null));
    assertTrue(map.containsValue("foo"));
    assertTrue(map.containsValue("baz"));
  }

  @Test
  public void testEquals() {

    TestBeanFullGetters bean = new TestBeanFullGetters();
    TestBeanFullGetters bean1 = new TestBeanFullGetters();
    BeanMap map = BeanMap.create(bean);
    assertEquals(map.size(), 6);

    BeanMap map1 = BeanMap.create(bean1);

    assertEquals(map, map1);
    assertNotEquals(map, null);
    assertNotEquals(map1, null);
    map1.put("foo", "");
    assertNotEquals(map, map1);
    assertNotEquals(map, BeanMap.create(new TestBean2()));
  }

  // -----------------------------------------------------------

  public static Object mixinMapIntoBean(final Object bean) {
    Enhancer e = new Enhancer();
    e.setSuperclass(bean.getClass());
    e.setInterfaces(new Class[] { Map.class });
    final Map map = BeanMap.create(bean);
    e.setCallbackFilter(new CallbackFilter() {
      public int accept(Method method) {
        return method.getDeclaringClass().equals(Map.class) ? 1 : 0;
      }
    });
    e.setCallbacks(new Callback[] { new Dispatcher() {
      public Object loadObject() {
        return bean;
      }
    }, new Dispatcher() {
      public Object loadObject() {
        return map;
      }
    }
    });
    return e.create();
  }

  // TODO: test different package
  // TODO: test change bean instance
  // TODO: test toString

  public void perform(ClassLoader loader) throws Throwable {
    // tested in enhancer test unit
  }

  public void testFailOnMemoryLeak() throws Throwable { }

}
