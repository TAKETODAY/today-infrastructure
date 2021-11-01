package cn.taketoday.context;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.core.reflect.MethodAccessor;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.core.reflect.PropertyAccessor;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.demo.config.Config;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.IntSupplier;
import java.util.function.LongFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A simple benchmark test
 *
 * @since 2.4
 */
@Disabled
class BenchmarkTests {

  private long times = 5000;
//	private long times = 1_0000_0000_0;

  @Test
  void testSingleton() {
    long start = System.currentTimeMillis();
    StandardApplicationContext applicationContext = new StandardApplicationContext();
    applicationContext.scan("cn.taketoday.context");
    applicationContext.refresh();
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
  void testPrototype() {
    long start = System.currentTimeMillis();
    StandardApplicationContext applicationContext = new StandardApplicationContext();
    applicationContext.scan();
    applicationContext.refresh();

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
  void testConstructor() throws Exception {

    Constructor<ConstructorTestBean> constructor = BeanUtils.obtainConstructor(ConstructorTestBean.class);

    BeanInstantiator beanInstantiator
            = BeanInstantiator.fromClass(ConstructorTestBean.class);
    constructor.setAccessible(true);
    long start = System.currentTimeMillis();
    int times = 1_0000_00000;
    for (int i = 0; i < times; i++) {
      ConstructorTestBean constructorTestBean = constructor.newInstance();
    }

    System.out.println("reflect used: " + (System.currentTimeMillis() - start) + "ms");

    start = System.currentTimeMillis();
    for (int i = 0; i < times; i++) {
      ConstructorTestBean constructorTestBean1 = (ConstructorTestBean) beanInstantiator.instantiate();
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
  void testMethod() throws Throwable {

    Method test = ReflectionUtils.findMethod(ITest.class, "test", String.class);
    MethodAccessor methodAccessor = MethodInvoker.fromMethod(test);
    test.setAccessible(true);
    final ITest testBean = new MethodTestBean();

    long start = System.currentTimeMillis();
    int times = 1_0000_000_00;
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
  void testProperty() throws IllegalAccessException {
    Field field = ReflectionUtils.findField(PropertyTestBean.class, "value");
    PropertyAccessor propertyAccessor = PropertyAccessor.fromField(field);

    PropertyTestBean propertyTestBean = new PropertyTestBean();
    propertyAccessor.set(propertyTestBean, "TODAY");

    Object value = propertyAccessor.get(propertyTestBean);
    assertThat(value).isEqualTo(propertyTestBean.value).isEqualTo("TODAY");

    // set
    long start = System.currentTimeMillis();
    int times = 1_0000_000;
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

    Bench1() { }

    void func0() { v++; }

    void func1() { v--; }

    void func2() { v++; }

    void func3() { v--; }

    void testInterface() {
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

    void testReflect() throws Throwable {
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

    void testMethodAccessor() throws Throwable {

      MethodInvoker[] ma = new MethodInvoker[] {
              MethodInvoker.fromMethod(Bench1.class.getMethod("func0")),
              MethodInvoker.fromMethod(Bench1.class.getMethod("func1")),
              MethodInvoker.fromMethod(Bench1.class.getMethod("func2")),
              MethodInvoker.fromMethod(Bench1.class.getMethod("func3")),
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

    Bench2() { }

    void func0() { v++; }

    void testInterface() {
      Runnable rs = this::func0;
      long t = System.nanoTime();
      for (int i = 0; i < 4_0000_0000; i++)
        rs.run(); // 关键调用
      t = (System.nanoTime() - t) / 1_000_000;
      System.out.format("Interface: %d %dms\n", v, t);
    }

    void testReflect() throws Throwable {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      MethodHandle ms = lookup.unreflect(Bench2.class.getMethod("func0"));
      long t = System.nanoTime();
      for (int i = 0; i < 4_0000_0000; i++)
        ms.invoke(this);
      t = (System.nanoTime() - t) / 1_000_000;
      System.out.format("MethodHandle  : %d %dms\n", v, t);
    }

    void testMethodAccessor() throws Throwable {

      MethodInvoker ma = MethodInvoker.fromMethod(Bench2.class.getMethod("func0"));

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

  // ---------------------

  static class VariableAccess {
    static final int DEFAULT_VALUE = 100;

    final long iteration;

    int field = DEFAULT_VALUE;
    final int finalField = DEFAULT_VALUE;
    static int staticField = DEFAULT_VALUE;
    static final int staticFinalField = DEFAULT_VALUE;
    volatile int volatileField = DEFAULT_VALUE;

//    int field = DEFAULT_VALUE;
//    final int finalField = DEFAULT_VALUE;
//    static int staticField = DEFAULT_VALUE;
//    static final int staticFinalField = DEFAULT_VALUE;
//    volatile int volatileField = DEFAULT_VALUE;

    VariableAccess() {
      this(900_000_000L);
    }

    VariableAccess(long iteration) {
      this.iteration = iteration;
    }

    public int localVariable() {
      return localVariable(iteration);
    }

    public int localVariable(long iteration) {
      int var = 100;
      int local = 0;
      for (int i = 0; i < iteration; i++) {
        local += var + 1;
      }
      return local;
    }

    public int finalLocalVariable() {
      return finalLocalVariable(iteration);
    }

    public int finalLocalVariable(long iteration) {
      int local = 0;
      final int var = this.field;
      for (int i = 0; i < iteration; i++) {
        local += var + 1;
      }
      return local;
    }

    public int field() {
      return field(iteration);
    }

    public int field(long iteration) {
      int local = 0;
      for (int i = 0; i < iteration; i++) {
        local += field + 1;
      }
      return local;
    }

    public int staticField() {
      return staticField(iteration);
    }

    public int staticField(long iteration) {
      int local = 0;
      for (int i = 0; i < iteration; i++) {
        local += staticField + 1;
      }
      return local;
    }

    public int finalField() {
      return finalField(iteration);
    }

    public int finalField(long iteration) {
      int local = 0;
      for (int i = 0; i < iteration; i++) {
        local += finalField + 1;
      }
      return local;
    }

    public int staticFinalField() {
      return staticFinalField(iteration);
    }

    public int staticFinalField(long iteration) {
      int local = 0;
      for (int i = 0; i < iteration; i++) {
        local += staticFinalField + 1;
      }
      return local;
    }

    public int volatileField() {
      return volatileField(iteration);
    }

    public int volatileField(long iteration) {
      int local = 0;
      for (int i = 0; i < iteration; i++) {
        local += volatileField + 1;
      }
      return local;
    }

    // ref

    Ref ref = new Ref();
    final Ref finalRef = new Ref();

    public int ref() {
      return ref(iteration);
    }

    public int ref(long iteration) {
      int local = 0;
      Ref ref;
      for (int i = 0; i < iteration; i++) {
        ref = this.ref;
        local++;
      }
      return local;
    }

    public int finalRef() {
      return finalRef(iteration);
    }

    public int finalRef(long iteration) {
      int local = 0;
      Ref ref;
      for (int i = 0; i < iteration; i++) {
        local++;
        ref = this.finalRef;
      }
      return local;
    }

    public int localRef() {
      return localRef(iteration);
    }

    public int localRef(long iteration) {
      int local = 0;
      Ref ref_;
      Ref ref = this.finalRef;
      for (int i = 0; i < iteration; i++) {
        local++;
        ref_ = ref;
      }
      return local;
    }
  }

  static class Ref {

  }

  @Test
  void testVariableAccess() {
    final VariableAccess variableAccess = new VariableAccess();
    benchmark(variableAccess::field, "field");
    benchmark(variableAccess::localVariable, "localVariable");
    benchmark(variableAccess::finalLocalVariable, "finalLocalVariable");
    benchmark(variableAccess::staticFinalField, "staticFinalField");
    benchmark(variableAccess::finalField, "finalField");
    benchmark(variableAccess::staticField, "staticField");
    benchmark(variableAccess::volatileField, "volatileField");

    // ref
    benchmark(variableAccess::ref, "ref");
    benchmark(variableAccess::localRef, "localRef");
    benchmark(variableAccess::finalRef, "finalRef");

    //

    benchmark(variableAccess::field, "field", variableAccess.iteration);
    benchmark(variableAccess::localVariable, "localVariable", variableAccess.iteration);
    benchmark(variableAccess::finalLocalVariable, "finalLocalVariable", variableAccess.iteration);
    benchmark(variableAccess::staticFinalField, "staticFinalField", variableAccess.iteration);
    benchmark(variableAccess::finalField, "finalField", variableAccess.iteration);
    benchmark(variableAccess::staticField, "staticField", variableAccess.iteration);
    benchmark(variableAccess::volatileField, "volatileField", variableAccess.iteration);

    // ref
    benchmark(variableAccess::ref, "ref", variableAccess.iteration);
    benchmark(variableAccess::localRef, "localRef", variableAccess.iteration);
    benchmark(variableAccess::finalRef, "finalRef", variableAccess.iteration);

  }

  private void benchmark(LongFunction<Integer> benchmark, String desc, long iteration) {
    long t = System.nanoTime();
    int asInt = benchmark.apply(iteration);
    t = (System.nanoTime() - t) / 1_000_000;
    System.out.format("%s          : %d   %dms\n", desc, asInt, t);
  }

  private void benchmark(IntSupplier benchmark, String desc) {
//    final StopWatch stopWatch = new StopWatch();
//    stopWatch.start(desc);
//    int asInt = benchmark.getAsInt();
//    stopWatch.stop();
//
//    System.out.format("%d : %s\n", asInt, stopWatch);

    long t = System.nanoTime();
    int asInt = benchmark.getAsInt();
    t = (System.nanoTime() - t) / 1_000_000;
    System.out.format("%s          : %d   %dms\n", desc, asInt, t);
  }

  // startsWith

  @Test
  void startsWith() {
    String text = StringUtils.generateRandomString(10);
//    String text = "$testBean";
    boolean result = true;
    for (int i = 0; i < 4_0000; i++) {
      result = i / 2 == 0;
    }

    long t = System.nanoTime();
    for (long i = 0; i < 90_0000_0000L; i++) {
      result = StringUtils.matchesFirst(text, BeanFactory.FACTORY_BEAN_PREFIX_CHAR);
    }
    t = (System.nanoTime() - t) / 1_000_000;
    System.out.format(" matchesFirst %s , result %s %dms\n", text, result, t);

    t = System.nanoTime();
    for (long i = 0; i < 90_0000_0000L; i++) {
      result = text.startsWith(BeanFactory.FACTORY_BEAN_PREFIX);
    }
    t = (System.nanoTime() - t) / 1_000_000;
    System.out.format(" startsWith %s, result %s %dms\n", text, result, t);

  }


}


