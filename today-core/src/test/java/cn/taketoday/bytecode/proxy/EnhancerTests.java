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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.core.AbstractClassGenerator;
import cn.taketoday.bytecode.core.NamingPolicy;
import cn.taketoday.bytecode.reflect.MethodAccess;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StreamUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Juozas Baliuka <a href="mailto:baliuka@mwm.lt"> baliuka@mwm.lt</a>
 * @version $Id: TestEnhancer.java,v 1.58 2012/07/27 16:02:49 baliuka Exp $
 */
class EnhancerTests {

  private static final MethodInterceptor TEST_INTERCEPTOR = new TestInterceptor();

  private static final Class<?>[] EMPTY_ARG = {};

  private boolean invokedProtectedMethod = false;

  private boolean invokedPackageMethod = false;

  private boolean invokedAbstractMethod = false;

  @Test
  public void testEnhance() throws Throwable {

    java.util.Vector<?> vector1 = (java.util.Vector<?>) Enhancer.create(
            java.util.Vector.class,
            new Class[]
                    { java.util.List.class }, TEST_INTERCEPTOR);

    java.util.Vector<?> vector2 = (java.util.Vector<?>) Enhancer.create(
            java.util.Vector.class,
            new Class[]
                    { java.util.List.class }, TEST_INTERCEPTOR);

    assertSame(vector1.getClass(), vector2.getClass(), "Cache failed");
  }

  @Test
  public void testMethods() throws Throwable {

    MethodInterceptor interceptor = new TestInterceptor() {

      private static final long serialVersionUID = 1L;

      public Object afterReturn(Object obj, Method method,
              Object[] args,
              boolean invokedSuper,
              Object retValFromSuper,
              Throwable e) {

        int mod = method.getModifiers();

        if (Modifier.isProtected(mod)) {
          invokedProtectedMethod = true;
        }

        if (Modifier.isAbstract(mod)) {
          invokedAbstractMethod = true;
        }

        if (!(Modifier.isProtected(mod) || Modifier.isPublic(mod))) {
          invokedPackageMethod = true;
        }

        return retValFromSuper;// return the same as supper
      }

    };

    Source source = (Source) Enhancer.create(Source.class, null, interceptor);

    source.callAll();
    assertTrue(invokedProtectedMethod, "protected");
    assertTrue(invokedPackageMethod, "package");
    assertTrue(invokedAbstractMethod, "abstract");
  }

  @Test
  public void testEnhanced() throws Throwable {
    Source source = (Source) Enhancer.create(Source.class, null, TEST_INTERCEPTOR);
    assertNotSame(Source.class, source.getClass(), "enhance");
  }

  @Test
  public void testFinalizeNotProxied() throws Throwable {
    Source source = (Source) Enhancer.create(
            Source.class,
            null, TEST_INTERCEPTOR
    );

    try {
      Method finalize = source.getClass().getDeclaredMethod("finalize");
      assertNull(finalize,
              "CGLIB should enhanced object should not declare finalize() method so proxy objects are not eligible for finalization, thus faster");
    }
    catch (NoSuchMethodException e) {
      // expected
    }
  }

  @Test
  public void testEnhanceObject() throws Throwable {
    EA obj = new EA();
    EA save = obj;
    obj.setName("herby");
    EA proxy = (EA) Enhancer.create(EA.class, new DelegateInterceptor(save));

    assertEquals("herby", proxy.getName(), "proxy.getName()");

    Factory factory = (Factory) proxy;
    assertEquals("herby", ((EA) factory.newInstance(
            factory.getCallbacks()))
            .getName(), "((EA)factory.newInstance(factory.getCallbacks())).getName()");
  }

  static class DelegateInterceptor implements MethodInterceptor {
    Object delegate;

    DelegateInterceptor(Object delegate) {
      this.delegate = delegate;
    }

    public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy) throws Throwable {
      return proxy.invoke(delegate, args);
    }

  }

  @Test
  public void testEnhanceObjectDelayed() throws Throwable {

    DelegateInterceptor mi = new DelegateInterceptor(null);
    EA proxy = (EA) Enhancer.create(EA.class, mi);
    EA obj = new EA();
    obj.setName("herby");
    mi.delegate = obj;
    assertEquals("herby", proxy.getName());
  }

  @Test
  public void testTypes() throws Throwable {

    Source source = (Source) Enhancer.create(
            Source.class,
            null, TEST_INTERCEPTOR);
    assertEquals(1, source.intType(1), "intType");
    assertEquals(1L, source.longType(1L), "longType");
    assertEquals(1.1f, source.floatType(1.1f), "floatType");
    assertEquals(1.1, source.doubleType(1.1), "doubleType");
    assertEquals("1", source.objectType("1"), "objectType");
    assertEquals("", source.toString(), "objectType");
    source.arrayType(new int[] {});

  }

  @Test
  public void testModifiers() throws Throwable {

    Source source = (Source) Enhancer.create(
            Source.class,
            null, TEST_INTERCEPTOR);

    Class<?> enhancedClass = source.getClass();

    assertTrue(Modifier.isProtected(enhancedClass.getDeclaredMethod("protectedMethod", EMPTY_ARG).getModifiers()), "isProtected");
    int mod = enhancedClass.getDeclaredMethod("packageMethod", EMPTY_ARG).getModifiers();
    assertFalse(Modifier.isProtected(mod) || Modifier.isPublic(mod), "isPackage");

    // not sure about this (do we need it for performace ?)
    assertTrue(Modifier.isFinal(mod), "isFinal");

    mod = enhancedClass.getDeclaredMethod("synchronizedMethod", EMPTY_ARG).getModifiers();
    assertFalse(Modifier.isSynchronized(mod), "isSynchronized");

  }

  @Test
  public void testObject() throws Throwable {

    Object source = Enhancer.create(
            null,
            null, TEST_INTERCEPTOR);

    assertSame(source.getClass().getSuperclass(), Object.class, "parent is object");

  }

  @Test
  public void testSystemClassLoader() throws Throwable {

    Object source = enhance(
            null,
            null, TEST_INTERCEPTOR, ClassLoader.getSystemClassLoader());
    source.toString();
    assertSame(source.getClass().getClassLoader(), ClassLoader.getSystemClassLoader(), "SystemClassLoader");

  }

  @Test
  public void testCustomClassLoader() throws Throwable {

    ClassLoader custom = new ClassLoader(this.getClass().getClassLoader()) { };

    Object source = enhance(null, null, TEST_INTERCEPTOR, custom);
    source.toString();
    assertSame(source.getClass().getClassLoader(), custom, "Custom classLoader");

    custom = new ClassLoader() { };

    source = enhance(null, null, TEST_INTERCEPTOR, custom);
    source.toString();
    assertSame(source.getClass().getClassLoader(), custom, "Custom classLoader");

  }

  @Test
  public void testProxyClassReuseAcrossGC() throws InterruptedException {
    String proxyClassName = null;
    for (int i = 0; i < 50; i++) {
      Enhancer enhancer = new Enhancer();
      enhancer.setSuperclass(Source.class);
      enhancer.setCallbackFilter(new CallbackFilter() {
        public int accept(Method method) {
          return 0;
        }

        @Override
        public boolean equals(Object obj) {
          return true;
        }

        @Override
        public int hashCode() {
          return 0;
        }
      });
      enhancer.setInterfaces(Serializable.class);
      enhancer.setCallback((InvocationHandler) (proxy, method, args) -> {
        if (method.getDeclaringClass() != Object.class && method.getReturnType() == String.class) {
          return null;
        }
        else {
          throw new RuntimeException("Do not know what to do.");
        }
      });
      Source proxy = (Source) enhancer.create();
      String actualProxyClassName = proxy.getClass().getName();
      if (proxyClassName == null) {
        proxyClassName = actualProxyClassName;
      }
      else {
        assertEquals(proxyClassName, actualProxyClassName, "GC iteration " + i + ", proxy class should survive GC and be reused even across GC");
      }
      System.gc();
    }
  }

  /**
   * Verifies that the cache in {@link AbstractClassGenerator} SOURCE doesn't leak
   * class definitions of classloaders that are no longer used.
   */
  @Test
  @Disabled
  public void testSourceCleanAfterAllLoaderDispose() throws Throwable {
    ClassLoader custom = new ClassLoader(this.getClass().getClassLoader()) {

      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        if ("cn.taketoday.bytecode.proxy.EA".equals(name)) {

          try {
            InputStream classStream = ResourceUtils.getResource(
                            "classpath:cn/taketoday/core/bytecode/proxy/EA.class")
                    .getInputStream();

            byte[] classBytes = StreamUtils.copyToByteArray(classStream);
            return this.defineClass("cn.taketoday.bytecode.proxy.EA", classBytes, 0, classBytes.length);
          }
          catch (IOException e) {
            return super.loadClass(name);
          }
        }
        else {
          return super.loadClass(name);
        }
      }
    };

//    PhantomReference<ClassLoader> clRef = new PhantomReference<>(custom, null);
    PhantomReference<ClassLoader> clRef = new PhantomReference<>(custom, new ReferenceQueue<>());

    buildAdvised(custom);
    custom = null;

    for (int i = 0; i < 10; ++i) {
      System.gc();
      Thread.sleep(100);
      if (clRef.enqueue()) {
        break;
      }
    }
    assertTrue(
            clRef.enqueue(),
            "CGLIB should allow classloaders to be evicted. PhantomReference<ClassLoader> was not cleared after 10 gc cycles, thus it is likely some cache is preventing the class loader to be garbage collected");

  }

  protected Object buildAdvised(ClassLoader custom)
          throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    final Class<?> eaClassFromCustomClassloader = custom.loadClass(EA.class.getName());

    CallbackFilter callbackFilter = new CallbackFilter() {
      private final Object advised = ReflectionUtils.newInstance(eaClassFromCustomClassloader);

      public int accept(Method method) {
        return 0;
      }

      @SuppressWarnings("unused")
      public Object getAdvised() {
        return advised;
      }

    };

    // Need to test both orders: "null classloader first" and "null last" just in case
    Object source_ = enhance(eaClassFromCustomClassloader, null, callbackFilter, TEST_INTERCEPTOR, null);
    Object source = enhance(eaClassFromCustomClassloader, null, callbackFilter, TEST_INTERCEPTOR, custom);
    assertSame(
            source.getClass(),
            source_.getClass(), "Same proxy class is expected since Enhancer with null (default) ClassLoader should use "
                    + " target class.getClassLoader(), thus the same cache key instance should be reused");

    Object source2 = enhance(eaClassFromCustomClassloader, null, callbackFilter, TEST_INTERCEPTOR, custom);
    assertSame(source.getClass(),
            source2.getClass(), "enhance should return cached Enhancer when calling with same parameters");

    Object source2_ = enhance(eaClassFromCustomClassloader, null, callbackFilter, TEST_INTERCEPTOR, null);
    assertSame(
            source.getClass(),
            source2_.getClass(), "Same proxy class is expected since Enhancer with null (default) ClassLoader should use "
                    + " target class.getClassLoader(), thus the same cache key instance should be reused");

    Object source3 = enhance(eaClassFromCustomClassloader, null, null, TEST_INTERCEPTOR, custom);
    assertNotSame(source.getClass(),
            source3.getClass(), "enhance should return different instance when callbackFilter differs");

    return source;
  }

  private static class TestFilter implements CallbackFilter {
    private final int pk;

    TestFilter(int pk) {
      this.pk = pk;
    }

    public int accept(Method method) {
      return 0;
    }

    @Override
    public int hashCode() {
      return 1; // Make sure Enhancer uses equals, not just hashCode alone
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof TestFilter && ((TestFilter) obj).pk == pk;
    }
  }

  @Test
  public void testCallbackFilterEqualsVsClassReuse() {
    Callback[] callbacks = new Callback[] { NoOp.INSTANCE };
    Object a = Enhancer.create(Source.class, null, new TestFilter(1), callbacks);
    Object b = Enhancer.create(Source.class, null, new TestFilter(1), callbacks);
    assertSame(a.getClass(),
            b.getClass(), "Using the same as per .equal() CallbackFilter, thus Enhancer should reuse the same proxy class");
  }

  @Test
  public void testCallbackFilterNotEqualsVsClassReuse() {
    Callback[] callbacks = new Callback[] { NoOp.INSTANCE };
    Object a = Enhancer.create(Source.class, null, new TestFilter(1), callbacks);
    Object b = Enhancer.create(Source.class, null, new TestFilter(2), callbacks);
    assertNotSame(a.getClass(),
            b.getClass(), "Using the different CallbackFilter instances, thus Enhancer should generate new proxy class");
  }

  @Test
  public void testRuntimException() throws Throwable {

    Source source = (Source) Enhancer.create(
            Source.class,
            null, TEST_INTERCEPTOR);

    try {
      source.throwIndexOutOfBoundsException();
      fail("must throw an exception");
    }
    catch (IndexOutOfBoundsException ok) {

    }

  }

  static abstract class CastTest {
    CastTest() { }

    abstract int getInt();
  }

  static class CastTestInterceptor implements MethodInterceptor {

    @Override
    public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy) throws Throwable {
      return (short) 0;
    }

  }

  @Test
  public void testCast() throws Throwable {
    CastTest castTest = (CastTest) Enhancer.create(CastTest.class, null, new CastTestInterceptor());
    assertEquals(0, castTest.getInt());
  }

  @Test
  public void testABC() throws Throwable {
    Enhancer.create(EA.class, null, TEST_INTERCEPTOR);
    Enhancer.create(EC1.class, null, TEST_INTERCEPTOR).toString();
    ((EB) Enhancer.create(EB.class, null, TEST_INTERCEPTOR)).finalTest();

    assertEquals(-1, ((EC1) Enhancer.create(
            EC1.class, null, TEST_INTERCEPTOR)).compareTo(new EC1()), "abstract method");

    Enhancer.create(ED.class, null, TEST_INTERCEPTOR).toString();
    Enhancer.create(ClassLoader.class, null, TEST_INTERCEPTOR).toString();
  }

  public static class AroundDemo {
    public String getFirstName() {
      return "Chris";
    }

    public String getLastName() {
      return "Nokleberg";
    }
  }

  @Test
  public void testAround() throws Throwable {
    AroundDemo demo = (AroundDemo) Enhancer.create(AroundDemo.class, null, new MethodInterceptor() {
      public Object intercept(Object obj, Method method, Object[] args,
              MethodProxy proxy) throws Throwable {
        if (method.getName().equals("getFirstName")) {
          return "Christopher";
        }
        return proxy.invokeSuper(obj, args);
      }
    });
    assertEquals("Christopher", demo.getFirstName());
    assertEquals("Nokleberg", demo.getLastName());
  }

  public static interface TestClone extends Cloneable {
    public Object clone() throws java.lang.CloneNotSupportedException;

  }

  public static class TestCloneImpl implements TestClone {
    public Object clone() throws java.lang.CloneNotSupportedException {
      return super.clone();
    }
  }

  @Test
  public void testClone() throws Throwable {

    TestClone testClone = (TestClone) Enhancer.create(TestCloneImpl.class, TEST_INTERCEPTOR);
    assertNotNull(testClone.clone());

    testClone = (TestClone) Enhancer.create(
            TestClone.class, new MethodInterceptor() {

              public Object intercept(
                      Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                return proxy.invokeSuper(obj, args);
              }

            });
    assertNotNull(testClone.clone());

  }

  public static interface FinalA {
    void foo();
  }

  public static class FinalB implements FinalA {
    final public void foo() { }
  }

  @Test
  public void testFinal() throws Throwable {
    ((FinalA) Enhancer.create(FinalB.class, TEST_INTERCEPTOR)).foo();
  }

  public static interface ConflictA {
    int foo();
  }

  public static interface ConflictB {
    String foo();
  }

  @Test
  public void testConflict() throws Throwable {
    Object foo = Enhancer.create(Object.class, new Class[] { ConflictA.class, ConflictB.class }, TEST_INTERCEPTOR);
    ((ConflictA) foo).foo();
    ((ConflictB) foo).foo();
  }

  @Test
  // TODO: make this work again
  public void testArgInit() throws Throwable {

    Enhancer e = new Enhancer();
    e.setSuperclass(ArgInit.class);
    e.setCallbackType(MethodInterceptor.class);
    Class<?> f = e.createClass();
    ArgInit a = (ArgInit) ReflectionUtils.newInstance(
            f,
            new Class[] { String.class },
            new Object[] { "test" }
    );
    assertEquals("test", a.toString());
    ((Factory) a).setCallback(0, TEST_INTERCEPTOR);
    assertEquals("test", a.toString());

    Callback[] callbacks = new Callback[] { TEST_INTERCEPTOR };
    ArgInit b = (ArgInit) ((Factory) a).newInstance(new Class[] { String.class },
            new Object[] { "test2" },
            callbacks);
    assertEquals("test2", b.toString());
    try {
      ((Factory) a).newInstance(new Class[] { String.class, String.class },
              new Object[] { "test" },
              callbacks);
      fail("must throw exception");
    }
    catch (IllegalArgumentException ignored) {

    }
  }

  public static class Signature {
    public int interceptor() {
      return 42;
    }
  }

  @Test
  public void testSignature() throws Throwable {
    Signature sig = (Signature) Enhancer.create(Signature.class, TEST_INTERCEPTOR);
    assertSame(((Factory) sig).getCallback(0), TEST_INTERCEPTOR);
    assertEquals(42, sig.interceptor());
  }

  public abstract static class AbstractMethodCallInConstructor {
    public AbstractMethodCallInConstructor() {
      foo();
    }

    public abstract void foo();
  }

  @Test
  public void testAbstractMethodCallInConstructor() throws Throwable {
    AbstractMethodCallInConstructor obj = (AbstractMethodCallInConstructor)
            Enhancer.create(AbstractMethodCallInConstructor.class, TEST_INTERCEPTOR);
    obj.foo();
  }

  @Test
  public void testProxyIface() {
    final DI1 other = new DI1() {
      public String herby() {
        return "boop";
      }
    };
    DI1 d = (DI1) Enhancer.create(DI1.class, (MethodInterceptor) (obj, method, args, proxy) -> proxy.invoke(other, args));
    assertEquals("boop", d.herby());
  }

//  static class NamingPolicyDummy { }

//  public void testNamingPolicy() throws Throwable {
//    Enhancer e = new Enhancer();
//    e.setSuperclass(NamingPolicyDummy.class);
//    e.setUseCache(false);
//    e.setUseFactory(false);
//    e.setNamingPolicy(new DefaultNamingPolicy() {
//      public String getTag() {
//        return "ByHerby";
//      }
//
//      public String toString() {
//        return getTag();
//      }
//    });
//    e.setCallbackType(MethodInterceptor.class);
//    Class<?> proxied = e.createClass();
//    final boolean[] ran = new boolean[1];
//    Enhancer.registerStaticCallbacks(proxied, new Callback[] { new MethodInterceptor() {
//      public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
//        ran[0] = true;
//        assertTrue(proxy.getSuperFastClass().getClass().getName().indexOf("$MethodAccessByHerby$") >= 0);
//        return proxy.invokeSuper(obj, args);
//      }
//    }
//    });
//    NamingPolicyDummy dummy = (NamingPolicyDummy) proxied.newInstance();
//    dummy.toString();
//    assertTrue(ran[0]);
//  }

  @Test
  public void testBadNamingPolicyStillReservesNames() throws Throwable {
    Enhancer e = new Enhancer();
    e.setUseCache(false);
    e.setCallback(NoOp.INSTANCE);
    e.setClassLoader(new ClassLoader(this.getClass().getClassLoader()) { });
    e.setNamingPolicy((prefix, source, key, names) -> "cn.taketoday.bytecode.Object$$ByDerby$$123");
    Class<?> proxied = e.create().getClass();
    final String name = proxied.getCanonicalName();
    final boolean[] ran = new boolean[1];
    e.setNamingPolicy((prefix, source, key, names) -> {
      ran[0] = true;
      assertTrue(names.test(name));
      return name + "45";
    });
    Class<?> proxied2 = e.create().getClass();
    assertTrue(ran[0]);
    assertEquals(name + "45", proxied2.getCanonicalName());
  }

  /**
   * In theory, every sane implementation of {@link NamingPolicy} should check if
   * the class name is occupied, however, in practice there are implementations in
   * the wild that just return whatever they feel is good.
   *
   * @throws Throwable if something wrong happens
   */
  @Test
  public void testNamingPolicyThatReturnsConstantNames() throws Throwable {
    Enhancer e = new Enhancer();
    final String desiredClassName = "cn.taketoday.bytecode.Object$$42";
    e.setCallback(NoOp.INSTANCE);
    e.setClassLoader(new ClassLoader(this.getClass().getClassLoader()) { });
    e.setNamingPolicy((prefix, source, key, names) -> desiredClassName);
    Class<?> proxied = e.create().getClass();
    assertEquals(desiredClassName, proxied.getName(),
            "Class name should match the one returned by NamingPolicy");
  }

  public static Object enhance(
          Class<?> cls, Class<?>[] interfaces, Callback callback, ClassLoader loader) {
    Enhancer e = new Enhancer();
    e.setSuperclass(cls);
    e.setInterfaces(interfaces);
    e.setCallback(callback);
    e.setClassLoader(loader);
    return e.create();
  }

  public static Object enhance(
          Class<?> cls, Class<?>[] interfaces, CallbackFilter callbackFilter, Callback callback, ClassLoader loader) {
    Enhancer e = new Enhancer();
    e.setSuperclass(cls);
    e.setInterfaces(interfaces);
    e.setCallbackFilter(callbackFilter);
    e.setCallback(callback);
    e.setClassLoader(loader);
    e.setNeighbor(null);
    return e.create();
  }

  public interface PublicClone extends Cloneable {
    Object clone() throws CloneNotSupportedException;
  }

  @Test
  public void testNoOpClone() throws Exception {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(PublicClone.class);
    enhancer.setCallback(NoOp.INSTANCE);
    ((PublicClone) enhancer.create()).clone();
  }

  @Test
  public void testNoFactory() throws Exception {
    noFactoryHelper();
    noFactoryHelper();
  }

  private void noFactoryHelper() {
    MethodInterceptor mi = new MethodInterceptor() {
      public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        return "Foo";
      }
    };
    Enhancer enhancer = new Enhancer();
    enhancer.setUseFactory(false);
    enhancer.setSuperclass(AroundDemo.class);
    enhancer.setCallback(mi);
    AroundDemo obj = (AroundDemo) enhancer.create();
    assertEquals("Foo", obj.getFirstName());
    assertFalse(obj instanceof Factory);
  }

  interface MethDec {
    void foo();
  }

  abstract static class MethDecImpl implements MethDec { }

  @Test
  public void testMethodDeclarer() throws Exception {
    final boolean[] result = new boolean[] { false };
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(MethDecImpl.class);
    enhancer.setCallback(new MethodInterceptor() {
      public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {

        result[0] = method.getDeclaringClass().getName().equals(MethDec.class.getName());
        return null;
      }
    });
    ((MethDecImpl) enhancer.create()).foo();
    assertTrue(result[0]);
  }

  interface ClassOnlyX { }

  @Test
  public void testClassOnlyFollowedByInstance() {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(ClassOnlyX.class);
    enhancer.setCallbackType(NoOp.class);
    Class<?> type = enhancer.createClass();

    enhancer = new Enhancer();
    enhancer.setSuperclass(ClassOnlyX.class);
    enhancer.setCallback(NoOp.INSTANCE);
    Object instance = enhancer.create();

    assertTrue(instance instanceof ClassOnlyX);
    assertEquals(type, instance.getClass(), "types of enhancer.createClass() and enhancer.create().getClass() should match");
  }

  //  public void testSql() {
//    final Enhancer enhancer = new Enhancer();
//    enhancer.setInterfaces(java.sql.PreparedStatement.class);
//    enhancer.setCallback(TEST_INTERCEPTOR);
//    enhancer.setClassLoader(ClassUtils.getClassLoader());
//
//    enhancer.create();
//  }
  @Test
  public void testEquals() throws Exception {
//        final boolean[] result = new boolean[] { false };
    EqualsInterceptor intercept = new EqualsInterceptor();
    Object obj = Enhancer.create(null, intercept);
    obj.equals(obj);
    assertTrue(intercept.called);
  }

  public static class EqualsInterceptor implements MethodInterceptor {
    final static Method EQUALS_METHOD =
            ReflectionUtils.findMethod(Object.class, "equals", Object.class);
    boolean called;

    public Object intercept(Object obj,
            Method method,
            Object[] args,
            MethodProxy proxy) throws Throwable {
      if (method.equals(EQUALS_METHOD)) {
        return proxy.invoke(this, args);
      }
      else {
        return proxy.invokeSuper(obj, args);
      }
    }

    public boolean equals(Object other) {
      called = true;
      return super.equals(other);
    }

    @Override
    public int hashCode() {
      return Objects.hash(called);
    }
  }

  private static interface ExceptionThrower {
    void throwsThrowable(int arg) throws Throwable;

    void throwsException(int arg) throws Exception;

    void throwsNothing(int arg);
  }

  private static class MyThrowable extends Throwable {

    private static final long serialVersionUID = 1L;
  }

  private static class MyException extends Exception {

    private static final long serialVersionUID = 1L;
  }

  private static class MyRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;
  }

  @Test
  public void testExceptions() {
    Enhancer e = new Enhancer();
    e.setSuperclass(ExceptionThrower.class);
    e.setCallback(new MethodInterceptor() {
      public Object intercept(Object obj,
              Method method,
              Object[] args,
              MethodProxy proxy) throws Throwable {
        switch ((Integer) args[0]) {
          case 1:
            throw new MyThrowable();
          case 2:
            throw new MyException();
          case 3:
            throw new MyRuntimeException();
          default:
            return null;
        }
      }
    });
    ExceptionThrower et = (ExceptionThrower) e.create();
    try {
      et.throwsThrowable(1);
    }
    catch (MyThrowable t) { }
    catch (Throwable t) {
      fail();
    }
    try {
      et.throwsThrowable(2);
    }
    catch (MyException t) { }
    catch (Throwable t) {
      fail();
    }
    try {
      et.throwsThrowable(3);
    }
    catch (MyRuntimeException t) { }
    catch (Throwable t) {
      fail();
    }

    try {
      et.throwsException(1);
    }
    catch (Throwable t) {
      assertTrue(t instanceof MyThrowable);
    }
    try {
      et.throwsException(2);
    }
    catch (MyException t) { }
    catch (Throwable t) {
      fail();
    }
    try {
      et.throwsException(3);
    }
    catch (MyRuntimeException t) { }
    catch (Throwable t) {
      fail();
    }
    try {
      et.throwsException(4);
    }
    catch (Throwable t) {
      fail();
    }

    try {
      et.throwsNothing(1);
    }
    catch (Throwable t) {
      assertTrue(t instanceof MyThrowable);
    }
    try {
      et.throwsNothing(2);
    }
    catch (Exception t) {
      assertTrue(t instanceof MyException);
    }
    try {
      et.throwsNothing(3);
    }
    catch (MyRuntimeException t) { }
    catch (Throwable t) {
      fail();
    }
    try {
      et.throwsNothing(4);
    }
    catch (Throwable t) {
      fail();
    }
  }

  @Test
  public void testUnusedCallback() {
    Enhancer e = new Enhancer();
    e.setCallbackTypes(MethodInterceptor.class, NoOp.class);
    e.setCallbackFilter(new CallbackFilter() {
      public int accept(Method method) {
        return 0;
      }
    });
    e.createClass();
  }

  private static ArgInit newArgInit(Class<?> clazz, String value) {
    return (ArgInit) ReflectionUtils.newInstance(
            clazz,
            new Class[] { String.class },
            new Object[] { value });
  }

  private static class StringValue
          implements MethodInterceptor {
    private String value;

    public StringValue(String value) {
      this.value = value;
    }

    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) {
      return value;
    }
  }

  @Test
  public void testRegisterCallbacks()
          throws InterruptedException {
    Enhancer e = new Enhancer();
    e.setSuperclass(ArgInit.class);
    e.setCallbackType(MethodInterceptor.class);
    e.setUseFactory(false);
    final Class<?> clazz = e.createClass();

    assertFalse(Factory.class.isAssignableFrom(clazz));
    assertEquals("test", newArgInit(clazz, "test").toString());

    Enhancer.registerCallbacks(clazz, new Callback[] { new StringValue("fizzy") });
    assertEquals("fizzy", newArgInit(clazz, "test").toString());
    assertEquals("fizzy", newArgInit(clazz, "test").toString());

    Enhancer.registerCallbacks(clazz, new Callback[] { null });
    assertEquals("test", newArgInit(clazz, "test").toString());

    Enhancer.registerStaticCallbacks(clazz, new Callback[] { new StringValue("soda") });
    assertEquals("test", newArgInit(clazz, "test").toString());

    Enhancer.registerCallbacks(clazz, null);
    assertEquals("soda", newArgInit(clazz, "test").toString());

    Thread thread = new Thread() {
      public void run() {
        assertEquals("soda", newArgInit(clazz, "test").toString());
      }
    };
    thread.start();
    thread.join();

    // clean-up static callback
    Enhancer.registerStaticCallbacks(clazz, null);
    assertEquals("test", newArgInit(clazz, "test").toString());
  }

  public void perform(ClassLoader loader) throws Exception {

    enhance(Source.class, null, TEST_INTERCEPTOR, loader);

  }

  @Test
  public void testCallbackHelper() {
    @SuppressWarnings("unused") final ArgInit delegate = new ArgInit("helper");
    Class<?> sc = ArgInit.class;
    Class<?>[] interfaces = new Class[] { DI1.class, DI2.class };

    AbstractCallbackFilter helper = new AbstractCallbackFilter(sc, interfaces) {
      protected Object getCallback(final Method method) {
        return (FixedValue) () -> "You called method " + method.getName();
      }
    };

    Enhancer e = new Enhancer();
    e.setSuperclass(sc);
    e.setInterfaces(interfaces);
    e.setCallbacks(helper.getCallbacks());
    e.setCallbackFilter(helper);

    ArgInit proxy = (ArgInit) e.create(new Class[] { String.class }, new Object[] { "whatever" });
    assertEquals("You called method toString", proxy.toString());
    assertEquals("You called method herby", ((DI1) proxy).herby());
    assertEquals("You called method derby", ((DI2) proxy).derby());
  }

  @Test
  public void testSerialVersionUID() throws Exception {
    Long suid = 0xABBADABBAD00L;

    Enhancer e = new Enhancer();
    e.setSerialVersionUID(suid);
    e.setCallback(NoOp.INSTANCE);
    Object obj = e.create();

    Field field = obj.getClass().getDeclaredField("serialVersionUID");
    field.setAccessible(true);
    assertEquals(suid, field.get(obj));
  }

  interface ReturnTypeA {
    int foo(String x);
  }

  interface ReturnTypeB {
    String foo(String x);
  }

  @Test
  public void testMethodsDifferingByReturnTypeOnly() throws IOException {
    Enhancer e = new Enhancer();
    e.setInterfaces(ReturnTypeA.class, ReturnTypeB.class);
    e.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
      if (method.getReturnType().equals(String.class))
        return "hello";
      return 42;
    });
    Object obj = e.create();
    assertEquals(42, ((ReturnTypeA) obj).foo("foo"));
    assertEquals("hello", ((ReturnTypeB) obj).foo("foo"));
    assertEquals(-1, MethodAccess.from(obj.getClass()).getIndex("foo", new Class[] { String.class }));
  }

  public static class ConstructorCall {
    private String x;

    public ConstructorCall() {
      x = toString();
    }

    public String toString() {
      return "foo";
    }
  }

  @Test
  public void testInterceptDuringConstruction() {
    FixedValue fixedValue = new FixedValue() {
      public Object loadObject() {
        return "bar";
      }
    };

    Enhancer e = new Enhancer();
    e.setSuperclass(ConstructorCall.class);
    e.setCallback(fixedValue);
    assertEquals("bar", ((ConstructorCall) e.create()).x);

    e = new Enhancer();
    e.setSuperclass(ConstructorCall.class);
    e.setCallback(fixedValue);
    e.setInterceptDuringConstruction(false);
    assertEquals("foo", ((ConstructorCall) e.create()).x);
  }

  void assertThreadLocalCallbacks(Class<?> cls) throws Exception {

    Field field = cls.getDeclaredField("today$ThreadCallbacks");
    field.setAccessible(true);

    assertNull(((ThreadLocal<?>) field.get(null)).get());
  }

  @Test
  public void testThreadLocalCleanup1() throws Exception {

    Enhancer e = new Enhancer();
    e.setUseCache(false);
    e.setCallbackType(NoOp.class);
    Class<?> cls = e.createClass();

    assertThreadLocalCallbacks(cls);

  }

  @Test
  public void testThreadLocalCleanup2() throws Exception {

    Enhancer e = new Enhancer();
    e.setCallback(NoOp.INSTANCE);
    Object obj = e.create();

    assertThreadLocalCallbacks(obj.getClass());

  }

  @Test
  public void testThreadLocalCleanup3() throws Exception {

    Enhancer e = new Enhancer();
    e.setCallback(NoOp.INSTANCE);
    Factory obj = (Factory) e.create();
    obj.newInstance(NoOp.INSTANCE);

    assertThreadLocalCallbacks(obj.getClass());

  }

  @Test
  public void testUseCache() throws Exception {
    Enhancer noCache = new Enhancer();
    noCache.setUseCache(false);
    noCache.setSuperclass(Foo.class);
    noCache.setCallback(NoOp.INSTANCE);
    Class<?> a = noCache.create().getClass();
    Class<?> b = noCache.create().getClass();
    assertNotSame(a, b);

    Enhancer withCache = new Enhancer();
    withCache.setUseCache(true);
    withCache.setSuperclass(Foo.class);
    withCache.setCallback(NoOp.INSTANCE);
    Class<?> c = withCache.create().getClass();
    Class<?> d = withCache.create().getClass();
    assertNotSame(a, c);
    assertNotSame(b, c);
    assertSame(c, d);
  }

  static class Foo {
    Foo() { }
  }

  @Test
  public void testBridgeForcesInvokeVirtual() {
    List<Class<?>> retTypes = new ArrayList<>();
    List<Class<?>> paramTypes = new ArrayList<>();
    Interceptor interceptor = new Interceptor(retTypes, paramTypes);

    Enhancer e = new Enhancer();
    e.setSuperclass(Impl.class);
    e.setCallbackFilter(method -> method.getDeclaringClass() != Object.class ? 0 : 1);
    e.setCallbacks(interceptor, NoOp.INSTANCE);
    // We expect the bridge ('ret') to be called & forward us to the non-bridge
    // 'erased'
    Interface intf = (Interface) e.create();
    intf.aMethod(null);
    // Make sure the right things got called in the right order:
    assertEquals(Arrays.asList(RetType.class, ErasedType.class), retTypes);

    // Validate calling the refined just gives us that.
    retTypes.clear();
    Impl impl = (Impl) intf;
    impl.aMethod((Refined) null);
    assertEquals(Collections.singletonList(Refined.class), retTypes);

    // When calling from the impl, we are dispatched directly to the non-bridge,
    // because that's just how it works.
    retTypes.clear();
    impl.aMethod((RetType) null);
    assertEquals(Collections.singletonList(ErasedType.class), retTypes);

    // Do a whole bunch of checks for the other methods too

    paramTypes.clear();
    intf.intReturn(null);
    assertEquals(Arrays.asList(RetType.class, ErasedType.class), paramTypes);

    paramTypes.clear();
    intf.voidReturn(null);
    assertEquals(Arrays.asList(RetType.class, ErasedType.class), paramTypes);

    paramTypes.clear();
    intf.widenReturn(null);
    assertEquals(Arrays.asList(RetType.class, ErasedType.class), paramTypes);
  }

  @Test
  public void testBridgeForcesInvokeVirtualEvenWithoutInterceptingBridge() {
    List<Class<?>> retTypes = new ArrayList<>();
    Interceptor interceptor = new Interceptor(retTypes);

    Enhancer e = new Enhancer();
    e.setSuperclass(Impl.class);
    e.setCallbackFilter(method -> {
      // Ideally this would be:
      // return !method.isBridge() && method.getDeclaringClass() != Object.class ? 0 :
      // 1;
      // But Eclipse sometimes labels the wrong things as bridge methods, so we're
      // more
      // explicit:
      return method.getDeclaringClass() != Object.class && method.getReturnType() != RetType.class ? 0 : 1;
    });
    e.setCallbacks(interceptor, NoOp.INSTANCE);
    // We expect the bridge ('ret') to be called & forward us to non-bridge
    // ('erased'),
    // and we only intercept on the non-bridge.
    Interface intf = (Interface) e.create();
    intf.aMethod(null);
    assertEquals(Collections.singletonList(ErasedType.class), retTypes);

    // Validate calling the refined just gives us that.
    retTypes.clear();
    Impl impl = (Impl) intf;
    impl.aMethod((Refined) null);
    assertEquals(Collections.singletonList(Refined.class), retTypes);

    // Make sure we still get our non-bride interception if we didn't intercept the
    // bridge.
    retTypes.clear();
    impl.aMethod((RetType) null);
    assertEquals(Collections.singletonList(ErasedType.class), retTypes);
  }

  @Test
  public void testReverseBridge() {
    List<Class<?>> retTypes = new ArrayList<>();
    Interceptor interceptor = new Interceptor(retTypes);

    Enhancer e = new Enhancer();
    e.setSuperclass(ReverseImpl.class);
    e.setCallbackFilter(method -> method.getDeclaringClass() != Object.class ? 0 : 1);
    e.setCallbacks(interceptor, NoOp.INSTANCE);
    // We expect the bridge ('erased') to be called & forward us to 'ret'
    // (non-bridge)
    ReverseSuper<?> superclass = (ReverseSuper<?>) e.create();
    superclass.aMethod(null, null, null, null);
    assertEquals(Arrays.asList(ErasedType.class, RetType.class), retTypes);

    // Calling the Refined type gives us just that.
    retTypes.clear();
    ReverseImpl impl2 = (ReverseImpl) superclass;
    impl2.aMethod(null, (Refined) null, null, null);
    assertEquals(Collections.singletonList(Refined.class), retTypes);

    retTypes.clear();
    impl2.aMethod(null, (RetType) null, null, null);
    assertEquals(Collections.singletonList(RetType.class), retTypes);
  }

  @Test
  public void testBridgeForMoreViz() {
    List<Class<?>> retTypes = new ArrayList<>();
    List<Class<?>> paramTypes = new ArrayList<>();
    Interceptor interceptor = new Interceptor(retTypes, paramTypes);

    Enhancer e = new Enhancer();
    e.setSuperclass(PublicViz.class);
    e.setCallbackFilter(method -> method.getDeclaringClass() != Object.class ? 0 : 1);
    e.setCallbacks(interceptor, NoOp.INSTANCE);

    VizIntf intf = (VizIntf) e.create();
    intf.aMethod(null);
    assertEquals(Collections.singletonList(Concrete.class), paramTypes);
  }

  @Test
  public void testBridgeParameterCheckcast() throws Exception {

    // If the compiler used for Z omits the bridge method, and X is compiled with
    // javac,
    // javac will generate an invokespecial bridge in X.

    // public interface I<A, B> {
    // public A f(B b);
    // }
    // public abstract class Z<U extends Number> implements I<U, Long> {
    // public U f(Long id) {
    // return null;
    // }
    // }
    // public class X extends Z<Integer> {}

    final Map<String, byte[]> classes = new HashMap<String, byte[]>();

    {
      ClassWriter cw = new ClassWriter(0);
      cw.visit(
              49,
              Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
              "I",
              "<A:Ljava/lang/Object;B:Ljava/lang/Object;>Ljava/lang/Object;",
              "java/lang/Object",
              null);
      {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "f",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                "(TB;)TA;",
                null);
        mv.visitEnd();
      }
      cw.visitEnd();
      classes.put("I.class", cw.toByteArray());
    }
    {
      ClassWriter cw = new ClassWriter(0);
      cw.visit(
              49,
              Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_ABSTRACT,
              "Z",
              "<U:Ljava/lang/Number;>Ljava/lang/Object;LI<TU;Ljava/lang/String;>;",
              "java/lang/Object",
              new String[]
                      { "I" });
      {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
      }
      {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "f",
                "(Ljava/lang/String;)Ljava/lang/Number;",
                "(Ljava/lang/String;)TU;",
                null);
        mv.visitCode();
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 2);
        mv.visitEnd();
      }
      cw.visitEnd();
      classes.put("Z.class", cw.toByteArray());
    }
    {
      ClassWriter cw = new ClassWriter(0);
      cw.visit(
              49, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "X", "LZ<Ljava/lang/Integer;>;", "Z", null);
      {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "Z", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
      }
      {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC,
                "f",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "Z", "f", "(Ljava/lang/String;)Ljava/lang/Number;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
      }

      cw.visitEnd();

      classes.put("X.class", cw.toByteArray());
    }

    ClassLoader classLoader = new ClassLoader(getClass().getClassLoader()) {
      @Override
      public InputStream getResourceAsStream(String name) {
        InputStream is = super.getResourceAsStream(name);
        if (is != null) {
          return is;
        }
        if (classes.containsKey(name)) {
          return new ByteArrayInputStream(classes.get(name));
        }
        return null;
      }

      public Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] ba = classes.get(name.replace('.', '/') + ".class");
        if (ba != null) {
          return defineClass(name, ba, 0, ba.length);
        }
        throw new ClassNotFoundException(name);
      }
    };

    List<Class<?>> retTypes = new ArrayList<>();
    List<Class<?>> paramTypes = new ArrayList<>();
    Interceptor interceptor = new Interceptor(retTypes, paramTypes);

    Enhancer e = new Enhancer();
    e.setClassLoader(classLoader);
    e.setSuperclass(classLoader.loadClass("X"));
    e.setCallback(interceptor);

    Object c = e.create();

    for (Method m : c.getClass().getDeclaredMethods()) {
      if (m.getName().equals("f") && m.getReturnType().equals(Object.class)) {
        m.invoke(c, new Object[] { null });
      }
    }

    // f(Object)Object should bridge to f(Number)String
    assertEquals(Arrays.asList(Object.class, Number.class), retTypes);
    assertEquals(Arrays.asList(Object.class, String.class), paramTypes);
  }

  @Test
  public void testInterfaceBridge() throws Exception {
    // recent versions of javac will generate a synthetic default bridge for
    // f(Object) in B

    // interface A<T> {
    // void f(T t);
    // }
    // interface B<T extends Number> extends A<T>{
    // void f(T t);
    // }

    if (getMajor() < 8) {
      // The test relies on Java 8 bytecode for default methods.
      return;
    }

    final Map<String, byte[]> classes = new HashMap<>();
    {
      ClassWriter classWriter = new ClassWriter(0);
      classWriter.visit(
              Opcodes.V1_8,
              Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE | Opcodes.ACC_PUBLIC,
              "A",
              "<T:Ljava/lang/Object;>Ljava/lang/Object;",
              "java/lang/Object",
              null);
      MethodVisitor methodVisitor = classWriter.visitMethod(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
              "f",
              "(Ljava/lang/Object;)V",
              "(TT;)V",
              null);
      methodVisitor.visitEnd();
      classWriter.visitEnd();

      classes.put("A.class", classWriter.toByteArray());
    }

    {
      ClassWriter classWriter = new ClassWriter(0);
      classWriter.visit(
              Opcodes.V1_8,
              Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE | Opcodes.ACC_PUBLIC,
              "B",
              "<T:Ljava/lang/Number;>Ljava/lang/Object;LA<TT;>;",
              "java/lang/Object",
              new String[]
                      { "A" });
      {
        MethodVisitor methodVisitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "f",
                "(Ljava/lang/Number;)V",
                "(TT;)V",
                null);
        methodVisitor.visitEnd();
      }
      {
        MethodVisitor methodVisitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC,
                "f",
                "(Ljava/lang/Object;)V",
                null,
                null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, "B", "f", "(Ljava/lang/Number;)V", true);
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(2, 2);
        methodVisitor.visitEnd();
      }
      classWriter.visitEnd();

      classes.put("B.class", classWriter.toByteArray());
    }

    ClassLoader classLoader = new ClassLoader(getClass().getClassLoader()) {
      @Override
      public InputStream getResourceAsStream(String name) {
        InputStream is = super.getResourceAsStream(name);
        if (is != null) {
          return is;
        }
        if (classes.containsKey(name)) {
          return new ByteArrayInputStream(classes.get(name));
        }
        return null;
      }

      public Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] ba = classes.get(name.replace('.', '/') + ".class");
        if (ba != null) {
          return defineClass(name, ba, 0, ba.length);
        }
        throw new ClassNotFoundException(name);
      }
    };

    final List<Class<?>> paramTypes = new ArrayList<>();

    Enhancer e = new Enhancer();
    e.setClassLoader(classLoader);
    e.setInterfaces(classLoader.loadClass("B"));
    e.setCallbackFilter(
            method -> method.isBridge() ? 1 : 0);
    e.setCallbacks((MethodInterceptor) (obj, method, args, proxy) -> {
      if (method.getParameterTypes().length > 0) {
        paramTypes.add(method.getParameterTypes()[0]);
      }
      return null;
    }, NoOp.INSTANCE);

    Object c = e.create();

    final Class<?> loadClass = classLoader.loadClass("A");
    for (Method m : loadClass.getDeclaredMethods()) {
      if (m.getName().equals("f")) {
        m.invoke(c, new Object[] { null });
      }
    }

    // f(Object)void should bridge to f(Number)void
    assertEquals(Collections.singletonList(Number.class), paramTypes);
  }

  private static int getMajor() {
    try {
      Method versionMethod = Runtime.class.getMethod("version");
      Object version = versionMethod.invoke(null);
      return (Integer) version.getClass().getMethod("major").invoke(version);
    }
    catch (Exception e) {
      // continue below
    }

    int version = (int) Double.parseDouble(System.getProperty("java.class.version"));
    if (49 <= version && version <= 52) {
      return version - (49 - 5);
    }
    throw new IllegalStateException("Unknown Java version: " + System.getProperty("java.specification.version"));
  }

  static class ErasedType { }

  static class RetType extends ErasedType { }

  static class Refined extends RetType { }

  static abstract class Superclass<T extends ErasedType> {
    // Check narrowing return value & parameters
    public T aMethod(T t) {
      return null;
    }

    // Check void return value
    public void voidReturn(T t) { }

    // Check primitive return value
    public int intReturn(T t) {
      return 1;
    }

    // Check widening return value
    public RetType widenReturn(T t) {
      return null;
    }
  }

  public interface Interface { // the usage of the interface forces the bridge
    RetType aMethod(RetType obj);

    void voidReturn(RetType obj);

    int intReturn(RetType obj);

    // a wider type than in superclass
    ErasedType widenReturn(RetType obj);
  }

  public static class Impl extends Superclass<RetType> implements Interface {
    // An even more narrowed type, just to make sure
    // it doesn't confuse us.
    public Refined aMethod(Refined obj) {
      return null;
    }
  }

  // Another set of classes -- this time with the bridging in reverse,
  // to make sure that if we define the concrete type, a bridge
  // is created to call it from an erased type.
  static abstract class ReverseSuper<T extends ErasedType> {
    // the various parameters are to make sure we only
    // change signature when we have to -- only 'c' goes
    // from ErasedType -> RetType
    public T aMethod(Concrete b, T c, RetType d, ErasedType e) {
      return null;
    }
  }

  static class Concrete { }

  static class ReverseImpl extends ReverseSuper<RetType> {
    public Refined aMethod(Concrete b, Refined c, RetType d, ErasedType e) {
      return null;
    }

    public RetType aMethod(Concrete b, RetType c, RetType d, ErasedType e) {
      return null;
    }
  }

  public interface VizIntf {
    public void aMethod(Concrete a);
  }

  static abstract class PackageViz implements VizIntf {
    public void aMethod(Concrete e) { }
  }

  // inherits aMethod from PackageViz, but bridges to make it
  // publicly accessible. the bridge here has the same
  // target signature, so it absolutely requires invokespecial,
  // otherwise we recurse forever.
  public static class PublicViz extends PackageViz implements VizIntf { }

  private static class Interceptor implements MethodInterceptor {
    private final List<Class<?>> retList;
    private final List<Class<?>> paramList;

    public Interceptor(List<Class<?>> retList) {
      this(retList, new ArrayList<>());
    }

    public Interceptor(List<Class<?>> retList, List<Class<?>> paramList) {
      this.retList = retList;
      this.paramList = paramList;
    }

    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
      retList.add(method.getReturnType());
      if (method.getParameterTypes().length > 0) {
        paramList.add(method.getParameterTypes()[0]);
      }
      return proxy.invokeSuper(obj, args);
    }
  }

}
