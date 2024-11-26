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

package infra.beans.factory;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import infra.beans.NotWritablePropertyException;
import infra.beans.support.BeanMap;
import infra.bytecode.proxy.Dispatcher;
import infra.bytecode.proxy.Enhancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TODAY 2021/5/29 23:08
 */
class BeanMapTests {

  @Test
  public void testBeanMapping() {
    final BeanMappingTestBean testBean = new BeanMappingTestBean();
    final BeanMap<BeanMappingTestBean> beanMap = BeanMap.forInstance(testBean);

    beanMap.put("stringProperty", "stringProperty value");
    assertThat(beanMap).containsKey("stringProperty");
    assertThat(beanMap).containsEntry("stringProperty", "stringProperty value");
  }

  @Test
  void testBean() {
    BeanMappingTestBean testBean = new BeanMappingTestBean();
    BeanMap<BeanMappingTestBean> beanMap = BeanMap.forInstance(testBean);

    assertThat(beanMap).isEqualTo(BeanMap.forInstance(testBean));

    HashMap<String, Object> expected = new HashMap<>(beanMap);
    assertThat(beanMap.equals(expected)).isTrue();

    beanMap.put("stringProperty", "stringProperty");
    assertThat(beanMap).containsKey("stringProperty");
    assertThat(beanMap.get("stringProperty")).isEqualTo("stringProperty");

    assertThat(beanMap.getTarget()).isEqualTo(testBean).isSameAs(testBean);

    beanMap.setTarget(new BeanMappingTestBean());

    assertThat(beanMap.get("stringProperty")).isNotEqualTo("stringProperty");

    assertThat(beanMap).isNotEqualTo(BeanMap.forInstance(testBean));
  }

  @Test
  void newInstance() {
    var beanMap = BeanMap.forClass(BeanMappingTestBean.class);
    assertThat(beanMap.newInstance()).isSameAs(beanMap.getTarget());

  }

  @Test
  void withInstance() {
    var beanMap = BeanMap.forClass(BeanMappingTestBean.class);
    assertThat(beanMap).isEqualTo(beanMap);
    assertThat(beanMap.newInstance()).isSameAs(beanMap.getTarget());

    beanMap.put("stringProperty", "stringProperty value");
    assertThat(beanMap).containsKey("stringProperty");
    assertThat(beanMap.get("stringProperty")).isEqualTo("stringProperty value");
    var actual = beanMap.withInstance(new BeanMappingTestBean());

    assertThat(actual.get("stringProperty")).isNotEqualTo("stringProperty value");
  }

  @Test
  void ignoreReadOnly() {
    var beanMap = BeanMap.forClass(BeanMappingTestBean.class);
    assertThat(beanMap.isIgnoreReadOnly()).isFalse();
    beanMap.setIgnoreReadOnly(true);
    Object readOnlyProperty = beanMap.get("readOnlyProperty");

    beanMap.put("readOnlyProperty", "readOnlyProperty value");
    assertThat(readOnlyProperty).isEqualTo(beanMap.get("readOnlyProperty"));

    beanMap.setIgnoreReadOnly(false);

    assertThatThrownBy(() -> beanMap.put("readOnlyProperty", "readOnlyProperty value"))
            .isInstanceOf(NotWritablePropertyException.class)
            .hasMessageEndingWith("that is not-writeable");
  }

  @Test
  void keySet() {
    var beanMap = BeanMap.forClass(BeanMappingTestBean.class);
    assertThat(beanMap.keySet()).contains("stringProperty");
  }

  @Test
  void get() {
    var beanMap = BeanMap.forClass(BeanMappingTestBean.class);
    assertThatThrownBy(() -> beanMap.get(1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("key must be a string");
  }

  @Test
  void clear() {
    var beanMap = BeanMap.forClass(BeanMappingTestBean.class);
    assertThatThrownBy(beanMap::clear)
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void remove() {
    var beanMap = BeanMap.forClass(BeanMappingTestBean.class);
    assertThatThrownBy(() -> beanMap.remove("stringProperty"))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  public static class TestBeanMapBean {
    private String foo;
    private final String bar = "x";
    protected String baz;
    private int quud;
    private final int quick = 42;
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
    BeanMap<TestBeanMapBean> map = BeanMap.forInstance(bean);

    assertEquals(7, map.size());
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
    BeanMap<TestBeanMapBean> map = BeanMap.forInstance(bean);
    assertEquals(map.entrySet().size(), map.size());
  }

  @Test
  public void testNoUnderlyingBean() {
    BeanMap<TestBeanMapBean> map = BeanMap.forClass(TestBeanMapBean.class);

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
    BeanMap<TestBeanFullGetters> map = BeanMap.forInstance(bean);
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
    BeanMap<TestBeanFullGetters> map = BeanMap.forInstance(bean);
    assertEquals(map.size(), 7);
    HashMap<Object, Object> map2 = new HashMap<>(map);
    assertThat(map2).isEqualTo(map);
    map2.remove("class");

    assertThat(map2).isNotEqualTo(map);
  }

  // -----------------------------------------------------------

  public static Object mixinMapIntoBean(final Object bean) {
    Enhancer e = new Enhancer();
    e.setSuperclass(bean.getClass());
    e.setInterfaces(Map.class);
    final Map map = BeanMap.forInstance(bean);
    e.setCallbackFilter(method -> method.getDeclaringClass().equals(Map.class) ? 1 : 0);
    e.setCallbacks((Dispatcher) () -> bean, (Dispatcher) () -> map);
    return e.create();
  }

}
