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
package cn.taketoday.core.bytecode.beans;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.lang.reflect.Method;
import java.util.Map;

import cn.taketoday.core.bytecode.CodeGenTestCase;
import cn.taketoday.core.bytecode.proxy.Callback;
import cn.taketoday.core.bytecode.proxy.CallbackFilter;
import cn.taketoday.core.bytecode.proxy.Dispatcher;
import cn.taketoday.core.bytecode.proxy.Enhancer;

import static org.junit.Assert.assertNotEquals;

public class TestBeanMap extends CodeGenTestCase {

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

    public void testBeanMap() {
        TestBeanMapBean bean = new TestBeanMapBean();
        BeanMap map = BeanMap.create(bean);
        BeanMap map2 = BeanMap.create(bean);
        assertEquals("BeanMap.create should use exactly the same bean class when called multiple times",
                     map.getClass(), map2.getClass());
        BeanMap map3 = BeanMap.create(new TestBean2());
        assertNotSame("BeanMap.create should use different classes for different beans",
                      map.getClass(), map3.getClass());
        assertTrue(map.size() == 6);
        assertTrue(map.get("foo") == null);
        map.put("foo", "FOO");
        assertTrue("FOO".equals(map.get("foo")));
        assertTrue(bean.getFoo().equals("FOO"));
        assertTrue("x".equals(map.get("bar")));
        assertTrue(((Integer) map.get("quick")).intValue() == 42);
        map.put("quud", new Integer(13));
        assertTrue(bean.getQuud() == 13);

        assertTrue(map.getPropertyType("foo").equals(String.class));
        assertTrue(map.getPropertyType("quud").equals(Integer.TYPE));
        assertTrue(map.getPropertyType("kdkkj") == null);
    }

    public void testEntrySet() {
        TestBeanMapBean bean = new TestBeanMapBean();
        BeanMap map = BeanMap.create(bean);
        assertTrue(map.entrySet().size() == map.size());
    }

    public void testNoUnderlyingBean() {
        BeanMap.Generator gen = new BeanMap.Generator();
        gen.setBeanClass(TestBeanMapBean.class);
        BeanMap map = gen.create();

        TestBeanMapBean bean = new TestBeanMapBean();
        assertTrue(bean.getFoo() == null);
        assertTrue(map.put(bean, "foo", "FOO") == null);
        assertTrue(bean.getFoo().equals("FOO"));
        assertTrue(map.get(bean, "foo").equals("FOO"));
    }

    public void testMixinMapIntoBean() {
        Object bean = new TestBeanMapBean();
        bean = mixinMapIntoBean(bean);
        ((TestBeanMapBean) bean).setFoo("hello");
        assertTrue(bean instanceof Map);
        assertTrue(((Map) bean).get("foo").equals("hello"));
    }

    public void testRequire() {
        BeanMap.Generator gen = new BeanMap.Generator();
        gen.setBeanClass(TestBeanMapBean.class);
        gen.setRequire(BeanMap.REQUIRE_GETTER);
        BeanMap map = gen.create();
        assertTrue(map.containsKey("foo"));
        assertTrue(map.containsKey("bar"));
        assertTrue(!map.containsKey("baz"));
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

    public TestBeanMap(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TestBeanMap.class);
    }

    public void perform(ClassLoader loader) throws Throwable {
        // tested in enhancer test unit
    }

    public void testFailOnMemoryLeak() throws Throwable {}

}
