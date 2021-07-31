/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.factory;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;

import cn.taketoday.cglib.beans.BeanMap;
import cn.taketoday.cglib.proxy.CallbackFilter;
import cn.taketoday.cglib.proxy.Dispatcher;
import cn.taketoday.cglib.proxy.Enhancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author TODAY 2021/5/29 23:08
 */
public class BeanMappingTests {

  @Test
  public void testBeanMapping() {
    final TestBean testBean = new TestBean();
    final BeanMapping<TestBean> beanMapping = BeanMapping.ofObject(testBean);

    final BeanMap beanMap = BeanMap.create(testBean);

    beanMap.put("stringProperty", "stringProperty value"); // testBean.setStringProperty("stringProperty value");

    assertThat(beanMap).containsKey("stringProperty");
    assertThat(beanMapping).containsKey("stringProperty");

    assertThat(beanMap).containsEntry("stringProperty", "stringProperty value");
    assertThat(beanMapping).containsEntry("stringProperty", "stringProperty value");
  }

  //  @Test
  public void benchmark() {
    final TestBean testBean = new TestBean();
    final BeanMapping<TestBean> beanMapping = BeanMapping.ofObject(testBean);

    System.out.println(beanMapping);

    final BeanMap beanMap = BeanMap.create(testBean);

    beanMap.put("stringProperty", "stringProperty value"); // testBean.setStringProperty("stringProperty value");
    System.out.println(beanMap);

    long start = System.currentTimeMillis();
    int times = 1_0000_0000_0;
    for (int i = 0; i < times; i++) {
      beanMapping.get("stringProperty");
    }
    System.out.println("BeanMapping used: " + (System.currentTimeMillis() - start) + "ms");

    start = System.currentTimeMillis();
    for (int i = 0; i < times; i++) {
      beanMap.get("stringProperty");
    }
    System.out.println("BeanMap used: " + (System.currentTimeMillis() - start) + "ms");

    start = System.currentTimeMillis();
    for (int i = 0; i < times; i++) {
      testBean.getStringProperty();
    }
    System.out.println("native used: " + (System.currentTimeMillis() - start) + "ms");

  }

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
    BeanMapping<TestBeanMapBean> map = BeanMapping.ofObject(bean);
    BeanMapping<TestBeanMapBean> map2 = BeanMapping.ofObject(bean);
    assertEquals("BeanMap.create should use exactly the same bean class when called multiple times",
                 map.getClass(), map2.getClass());

    assertEquals(6, map.size());
    assertNull(map.get("foo"));
    map.put("foo", "FOO");
    assertEquals("FOO", map.get("foo"));
    assertEquals("FOO", bean.getFoo());
    assertEquals("x", map.get("bar"));
    assertEquals(42, (int) (Integer) map.get("quick"));
    map.put("quud", 13);
    assertEquals(13, bean.getQuud());

    assertEquals(map.getPropertyType("foo"), String.class);
    assertEquals(map.getPropertyType("quud"), Integer.TYPE);
    assertNull(map.getPropertyType("kdkkj"));
  }

  @Test
  public void testEntrySet() {
    TestBeanMapBean bean = new TestBeanMapBean();
    BeanMapping<TestBeanMapBean> map = BeanMapping.ofObject(bean);
    assertEquals(map.entrySet().size(), map.size());
  }

  @Test
  public void testNoUnderlyingBean() {
    BeanMapping<TestBeanMapBean> map = BeanMapping.ofClass(TestBeanMapBean.class);

    TestBeanMapBean bean = new TestBeanMapBean();
    assertNull(bean.getFoo());
    assertNull(map.put(bean, "foo", "FOO"));
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
    BeanMapping<TestBeanFullGetters> map = BeanMapping.ofObject(bean);
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
    BeanMapping<TestBeanFullGetters> map = BeanMapping.ofObject(bean);
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
    e.setInterfaces(Map.class);
    final Map map = BeanMapping.ofObject(bean);
    e.setCallbackFilter(new CallbackFilter() {
      public int accept(Method method) {
        return method.getDeclaringClass().equals(Map.class) ? 1 : 0;
      }
    });
    e.setCallbacks(new Dispatcher() {
      public Object loadObject() {
        return bean;
      }
    }, new Dispatcher() {
      public Object loadObject() {
        return map;
      }
    });
    return e.create();
  }

}
