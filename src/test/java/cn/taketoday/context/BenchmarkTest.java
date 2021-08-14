package cn.taketoday.context;

import org.junit.Ignore;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cn.taketoday.core.reflect.BeanConstructor;
import cn.taketoday.core.reflect.MethodAccessor;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.core.reflect.PropertyAccessor;
import cn.taketoday.core.utils.ClassUtils;
import cn.taketoday.core.utils.ReflectionUtils;
import lombok.Getter;
import lombok.Setter;
import test.demo.config.Config;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A simple benchmark test
 *
 * @since 2.4
 */
@Ignore
public class BenchmarkTest {

    private long times = 5000;
//	private long times = 1_0000_0000_0;

    @Test
    public void testSingleton() {
        long start = System.currentTimeMillis();
        ApplicationContext applicationContext = new StandardApplicationContext();
        applicationContext.load();
        System.out.println("start context used: " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            applicationContext.getBean(Config.class);
//			applicationContext.getBean("Config");
        }
        long end = System.currentTimeMillis();
        applicationContext.close();
        System.out.println("Singleton used: " + (end - start) + "ms");
    }

    //	@Test
    public void testPrototype() {
        long start = System.currentTimeMillis();
        ApplicationContext applicationContext = new StandardApplicationContext();
        applicationContext.load();
        System.out.println("start context used: " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();

        System.err.println(applicationContext.getBean("prototype_config"));
        for (int i = 0; i < times; i++) {
            applicationContext.getBean("prototype_config");
        }
        long end = System.currentTimeMillis();
        applicationContext.close();
        System.out.println("Prototype used: " + (end - start) + "ms");
    }

    static class ConstructorTestBean {

    }

    @Test
    public void testConstructor() throws Exception {

        Constructor<ConstructorTestBean> constructor = ClassUtils.obtainConstructor(ConstructorTestBean.class);

        BeanConstructor<ConstructorTestBean> beanConstructor = ReflectionUtils.newConstructor(ConstructorTestBean.class);

        long start = System.currentTimeMillis();
        int times = 1_0000_0000;
        for (int i = 0; i < times; i++) {
            ConstructorTestBean constructorTestBean = constructor.newInstance();
        }

        System.out.println("reflect used: " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            ConstructorTestBean constructorTestBean1 = beanConstructor.newInstance();
        }
        System.out.println("beanConstructor used: " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            ConstructorTestBean constructorTestBean1 = new ConstructorTestBean();
        }
        System.out.println("native used: " + (System.currentTimeMillis() - start) + "ms");
    }

    interface ITest {
        String test(String name);
    }

    static class MethodTestBean implements ITest {
        long v;

        public String test(String name) {
            v++;
            return name;
        }
    }

    @Test
    public void testMethod() throws Throwable {

        Method test = ReflectionUtils.findMethod(ITest.class, "test", String.class);
        MethodAccessor methodAccessor = MethodInvoker.create(test);

        final ITest testBean = new MethodTestBean();

        long start = System.currentTimeMillis();
        int times = 1_0000_0000_0;
        for (int i = 0; i < times; i++) {
            String name = (String) test.invoke(testBean, "TODAY");
        }

        System.out.println("reflect used: " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            String name = (String) methodAccessor.invoke(testBean, new Object[] { "TODAY" });
        }
        System.out.println("method accessor used: " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            final String name = testBean.test("TODAY");
        }
        System.out.println("native used: " + (System.currentTimeMillis() - start) + "ms");

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        start = System.currentTimeMillis();

        final MethodType methodType = MethodType.methodType(String.class, String.class);
        final MethodHandle methodHandle = lookup.findVirtual(MethodTestBean.class, "test", methodType);
        for (int i = 0; i < times; i++) {
           String re = (String) methodHandle.invoke(testBean, "TODAY");
        }
        System.out.println("MethodHandles used: " + (System.currentTimeMillis() - start) + "ms");
    }

    @Setter
    @Getter
    static class PropertyTestBean {
        String value;
    }

    @Test
    public void testProperty() throws IllegalAccessException {
        Field field = ReflectionUtils.findField(PropertyTestBean.class, "value");
        PropertyAccessor propertyAccessor = ReflectionUtils.newPropertyAccessor(field);

        PropertyTestBean propertyTestBean = new PropertyTestBean();
        propertyAccessor.set(propertyTestBean, "TODAY");

        Object value = propertyAccessor.get(propertyTestBean);
        assertThat(value).isEqualTo(propertyTestBean.value).isEqualTo("TODAY");

        // set
        long start = System.currentTimeMillis();
        int times = 1_0000_0000_0;
        for (int i = 0; i < times; i++) {
            field.set(propertyTestBean, "reflect");
        }

        System.out.println("reflect set used: " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            propertyAccessor.get(propertyTestBean);
        }
        System.out.println("Accessor set used: " + (System.currentTimeMillis() - start) + "ms");

        // get
        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            field.get(propertyTestBean);
        }
        System.out.println("reflect get used: " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            propertyAccessor.get(propertyTestBean);
        }
        System.out.println("Accessor get used: " + (System.currentTimeMillis() - start) + "ms");
    }

    // ----------------------------------

    static class Bench1 {
        long v;

        Bench1() {}

        public void func0() { v++; }

        public void func1() { v--; }

        public void func2() { v++; }

        public void func3() { v--; }

        public void testInterface() {
            Runnable[] rs = {
              this::func0,
              this::func1,
              this::func2,
              this::func3,
            };
            long t = System.nanoTime();
            for (int i = 0; i < 1_0000_0000; i++)
                rs[i & 3].run(); // 关键调用
            t = (System.nanoTime() - t) / 1_000_000;
            System.out.format("Interface    : %d %dms\n", v, t);
        }

        public void testReflect() throws Throwable {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle[] ms = {
              lookup.unreflect(Bench1.class.getMethod("func0")),
              lookup.unreflect(Bench1.class.getMethod("func1")),
              lookup.unreflect(Bench1.class.getMethod("func2")),
              lookup.unreflect(Bench1.class.getMethod("func3")),
            };
            long t = System.nanoTime();
            for (int i = 0; i < 1_0000_0000; i++)
                ms[i & 3].invokeExact(this);
            t = (System.nanoTime() - t) / 1_000_000;
            System.out.format("MethodHandle : %d %dms\n", v, t);
        }

        public void testMethodAccessor() throws Throwable {

            MethodInvoker[] ma = new MethodInvoker[] {
              MethodInvoker.create(Bench1.class.getMethod("func0")),
              MethodInvoker.create(Bench1.class.getMethod("func1")),
              MethodInvoker.create(Bench1.class.getMethod("func2")),
              MethodInvoker.create(Bench1.class.getMethod("func3")),
            };
            Bench1 self = this;
            long t = System.nanoTime();
            for (int i = 0; i < 1_0000_0000; i++)
                ma[i & 3].invoke(self, null);
            t = (System.nanoTime() - t) / 1_000_000;
            System.out.format("Accessor     : %d %dms\n", v, t);
        }

        public static void main(String[] args) throws Throwable {
            Bench1 b;
            for (int i = 0; i < 10; i++) {

                System.out.println("===================================");

                b = new Bench1(); // 实测部分
                b.testMethodAccessor();

                b = new Bench1();
                b.testInterface();

                b = new Bench1();
                b.testReflect();
            }
        }
    }

    static class Bench2 {
        long v;

        Bench2() {}

        public void func0() { v++; }

        public void testInterface() {
            Runnable rs = this::func0;
            long t = System.nanoTime();
            for (int i = 0; i < 4_0000_0000; i++)
                rs.run(); // 关键调用
            t = (System.nanoTime() - t) / 1_000_000;
            System.out.format("Interface: %d %dms\n", v, t);
        }

        public void testReflect() throws Throwable {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle ms = lookup.unreflect(Bench2.class.getMethod("func0"));
            long t = System.nanoTime();
            for (int i = 0; i < 4_0000_0000; i++)
                ms.invoke(this);
            t = (System.nanoTime() - t) / 1_000_000;
            System.out.format("MethodHandle  : %d %dms\n", v, t);
        }

        public void testMethodAccessor() throws Throwable {

            MethodInvoker ma = MethodInvoker.create(Bench2.class.getMethod("func0"));

            Bench2 self = this;
            long t = System.nanoTime();
            for (int i = 0; i < 4_0000_0000; i++)
                ma.invoke(self, null);
            t = (System.nanoTime() - t) / 1_000_000;
            System.out.format("Accessor  : %d %dms\n", v, t);
        }

        public static void main(String[] args) throws Throwable {
            Bench2 b;
            for (int i = 0; i < 10; i++) {
                System.out.println("===================================");

                b = new Bench2(); // 实测部分
                b.testMethodAccessor();

                b = new Bench2();
                b.testReflect();

                b = new Bench2();
                b.testInterface();
            }
        }
    }
}


