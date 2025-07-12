/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import infra.reflect.PropertyAccessor;
import infra.reflect.ReflectionException;
import infra.tests.sample.objects.TestObject;
import infra.util.ReflectionUtils.MethodFilter;
import lombok.Getter;
import lombok.Setter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TODAY <br>
 * 2020-08-13 21:55
 */
class ReflectionUtilsTests {

  @Test
  void doWithMethodsUsingUserDeclaredMethodsFilterStartingWithObject() {
    ListSavingMethodCallback mc = new ListSavingMethodCallback();
    ReflectionUtils.doWithMethods(Object.class, mc, ReflectionUtils.USER_DECLARED_METHODS);
    assertThat(mc.getMethodNames()).isEmpty();
  }

  @Test
  void doWithMethodsUsingUserDeclaredMethodsFilterStartingWithTestObject() {
    ListSavingMethodCallback mc = new ListSavingMethodCallback();
    ReflectionUtils.doWithMethods(TestObject.class, mc, ReflectionUtils.USER_DECLARED_METHODS);
    assertThat(mc.getMethodNames())
            .as("user declared methods").contains("absquatulate", "compareTo", "getName", "setName", "getAge", "setAge", "getSpouse", "setSpouse")
            .as("methods on Object").doesNotContain("equals", "hashCode", "toString", "clone", "finalize", "getClass", "notify", "notifyAll", "wait");
  }

  @Test
  void doWithMethodsUsingUserDeclaredMethodsComposedFilter() {
    ListSavingMethodCallback mc = new ListSavingMethodCallback();
    // "q" because both absquatulate() and equals() contain "q"
    MethodFilter isSetterMethodOrNameContainsQ = m -> m.getName().startsWith("set") || m.getName().contains("q");
    MethodFilter methodFilter = ReflectionUtils.USER_DECLARED_METHODS.and(isSetterMethodOrNameContainsQ);
    ReflectionUtils.doWithMethods(TestObject.class, mc, methodFilter);
    assertThat(mc.getMethodNames()).containsExactlyInAnyOrder("setName", "setAge", "setSpouse", "absquatulate");
  }

  public static class POJO1 {

  }

  @Test
  public void testCreate() {
    Object o = ReflectionUtils.newInstance(POJO1.class);
    assertNotNull(o);
    assertSame(POJO1.class, o.getClass());
  }

  public static class POJO3 {
    public boolean constructorInvoked = true;
  }

  public static class POJO4 extends POJO3 {
    public boolean pojo4_constructorInvoked = true;
  }

  @Test
  public void testCallConstructor() {
    POJO3 pojo3 = ReflectionUtils.newInstance(POJO3.class);
    assertNotNull(pojo3);
    assertTrue(pojo3.constructorInvoked);
  }

  @Test
  public void testCallParentConstructor() {
    POJO4 pojo = ReflectionUtils.newInstance(POJO4.class);
    assertNotNull(pojo);
    assertTrue(pojo.constructorInvoked);
    assertTrue(pojo.pojo4_constructorInvoked);
  }

  // --------------

  // -----------------------

  @Getter
  @Setter
  public static class PropertyBean {
    static int static_pro = 0;
    boolean bool = false;
    final long finalPro = 10L;
    static final short staticFinalPro = 100;
  }

  @Test
  public void testNewPropertyAccessor() throws NoSuchFieldException {
    final PropertyBean propertyBean = new PropertyBean();

    final Field declaredField = PropertyBean.class.getDeclaredField("static_pro");
    final PropertyAccessor staticProAccessor = PropertyAccessor.forField(declaredField);

    assertEquals(staticProAccessor.get(null), 0);
    staticProAccessor.set(null, 2);
    assertEquals(staticProAccessor.get(null), 2);

    final Field boolField = PropertyBean.class.getDeclaredField("bool");
    final PropertyAccessor boolAccessor = PropertyAccessor.forField(boolField);

    assertEquals(boolAccessor.get(propertyBean), false);
    boolAccessor.set(propertyBean, true);
    assertEquals(boolAccessor.get(propertyBean), true);

    final Field finalProField = PropertyBean.class.getDeclaredField("finalPro");
    final PropertyAccessor finalProAccessor = PropertyAccessor.forField(finalProField);
    assertEquals(finalProAccessor.get(propertyBean), 10L);

    try {
      finalProAccessor.set(null, 101);
    }
    catch (ReflectionException e) {
      assertEquals(finalProAccessor.get(propertyBean), 10L);
    }
    final Field staticFinalProField = PropertyBean.class.getDeclaredField("staticFinalPro");
    final PropertyAccessor staticFinalProAccessor = PropertyAccessor.forField(staticFinalProField);
    assertEquals(staticFinalProAccessor.get(propertyBean), (short) 100);

    try {
      staticFinalProAccessor.set(null, 101);
    }
    catch (ReflectionException e) {
      assertEquals(staticFinalProAccessor.get(propertyBean), (short) 100);
    }

  }

  // ------------------------------------------------------------------------------

  @Test
  public void testFindField() {
    Field field = ReflectionUtils.findField(TestObjectSubclassWithPublicField.class, "publicField", String.class);
    assertThat(field).isNotNull();
    assertThat(field.getName()).isEqualTo("publicField");
    assertThat(field.getType()).isEqualTo(String.class);
    assertThat(Modifier.isPublic(field.getModifiers())).as("Field should be public.").isTrue();

    field = ReflectionUtils.findField(TestObjectSubclassWithNewField.class, "prot", String.class);
    assertThat(field).isNotNull();
    assertThat(field.getName()).isEqualTo("prot");
    assertThat(field.getType()).isEqualTo(String.class);
    assertThat(Modifier.isProtected(field.getModifiers())).as("Field should be protected.").isTrue();

    field = ReflectionUtils.findField(TestObjectSubclassWithNewField.class, "name", String.class);
    assertThat(field).isNotNull();
    assertThat(field.getName()).isEqualTo("name");
    assertThat(field.getType()).isEqualTo(String.class);
    assertThat(Modifier.isPrivate(field.getModifiers())).as("Field should be private.").isTrue();
  }

  @Test
  public void testSetField() {
    TestObjectSubclassWithNewField testBean = new TestObjectSubclassWithNewField();
    Field field = ReflectionUtils.findField(TestObjectSubclassWithNewField.class, "name", String.class);

    ReflectionUtils.makeAccessible(field);

    ReflectionUtils.setField(field, testBean, "FooBar");
    assertThat(testBean.getName()).isNotNull();
    assertThat(testBean.getName()).isEqualTo("FooBar");

    ReflectionUtils.setField(field, testBean, null);
    assertThat((Object) testBean.getName()).isNull();
  }

  @Test
  public void testInvokeMethod() throws Exception {
    String rob = "Rob Harrop";

    TestObject bean = new TestObject();
    bean.setName(rob);

    Method getName = TestObject.class.getMethod("getName");
    Method setName = TestObject.class.getMethod("setName", String.class);

    Object name = ReflectionUtils.invokeMethod(getName, bean);
    assertThat(name).as("Incorrect name returned").isEqualTo(rob);

    String juergen = "Juergen Hoeller";
    ReflectionUtils.invokeMethod(setName, bean, juergen);
    assertThat(bean.getName()).as("Incorrect name set").isEqualTo(juergen);
  }

  @Test
  public void testDeclaresException() throws Exception {
    Method remoteExMethod = A.class.getDeclaredMethod("foo", Integer.class);
    assertThat(ReflectionUtils.declaresException(remoteExMethod, RemoteException.class)).isTrue();
    assertThat(ReflectionUtils.declaresException(remoteExMethod, ConnectException.class)).isTrue();
    assertThat(ReflectionUtils.declaresException(remoteExMethod, NoSuchMethodException.class)).isFalse();
    assertThat(ReflectionUtils.declaresException(remoteExMethod, Exception.class)).isFalse();

    Method illegalExMethod = B.class.getDeclaredMethod("bar", String.class);
    assertThat(ReflectionUtils.declaresException(illegalExMethod, IllegalArgumentException.class)).isTrue();
    assertThat(ReflectionUtils.declaresException(illegalExMethod, NumberFormatException.class)).isTrue();
    assertThat(ReflectionUtils.declaresException(illegalExMethod, IllegalStateException.class)).isFalse();
    assertThat(ReflectionUtils.declaresException(illegalExMethod, Exception.class)).isFalse();
  }

  @Test
  public void testCopySrcToDestinationOfIncorrectClass() {
    TestObject src = new TestObject();
    String dest = new String();
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionUtils.shallowCopyFieldState(src, dest));
  }

  @Test
  public void testRejectsNullSrc() {
    TestObject src = null;
    String dest = new String();
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionUtils.shallowCopyFieldState(src, dest));
  }

  @Test
  public void testRejectsNullDest() {
    TestObject src = new TestObject();
    String dest = null;
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionUtils.shallowCopyFieldState(src, dest));
  }

  @Test
  public void testValidCopy() {
    TestObject src = new TestObject();
    TestObject dest = new TestObject();
    testValidCopy(src, dest);
  }

  @Test
  public void testValidCopyOnSubTypeWithNewField() {
    TestObjectSubclassWithNewField src = new TestObjectSubclassWithNewField();
    TestObjectSubclassWithNewField dest = new TestObjectSubclassWithNewField();
    src.magic = 11;

    // Will check inherited fields are copied
    testValidCopy(src, dest);

    // Check subclass fields were copied
    assertThat(dest.magic).isEqualTo(src.magic);
    assertThat(dest.prot).isEqualTo(src.prot);
  }

  @Test
  public void testValidCopyToSubType() {
    TestObject src = new TestObject();
    TestObjectSubclassWithNewField dest = new TestObjectSubclassWithNewField();
    dest.magic = 11;
    testValidCopy(src, dest);
    // Should have left this one alone
    assertThat(dest.magic).isEqualTo(11);
  }

  @Test
  public void testValidCopyToSubTypeWithFinalField() {
    TestObjectSubclassWithFinalField src = new TestObjectSubclassWithFinalField();
    TestObjectSubclassWithFinalField dest = new TestObjectSubclassWithFinalField();
    // Check that this doesn't fail due to attempt to assign final
    testValidCopy(src, dest);
  }

  private void testValidCopy(TestObject src, TestObject dest) {
    src.setName("freddie");
    src.setAge(15);
    src.setSpouse(new TestObject());
    assertThat(src.getAge() == dest.getAge()).isFalse();

    ReflectionUtils.shallowCopyFieldState(src, dest);
    assertThat(dest.getAge()).isEqualTo(src.getAge());
    assertThat(dest.getSpouse()).isEqualTo(src.getSpouse());
  }

  @Test
  public void testDoWithProtectedMethods() {
    ListSavingMethodCallback mc = new ListSavingMethodCallback();
    ReflectionUtils.doWithMethods(TestObject.class, mc, new MethodFilter() {
      @Override
      public boolean matches(Method m) {
        return Modifier.isProtected(m.getModifiers());
      }
    });
    assertThat(mc.getMethodNames().isEmpty()).isFalse();
    assertThat(mc.getMethodNames().contains("clone")).as("Must find protected method on Object").isTrue();
    assertThat(mc.getMethodNames().contains("finalize")).as("Must find protected method on Object").isTrue();
    assertThat(mc.getMethodNames().contains("hashCode")).as("Public, not protected").isFalse();
    assertThat(mc.getMethodNames().contains("absquatulate")).as("Public, not protected").isFalse();
  }

  @Test
  public void testDuplicatesFound() {
    ListSavingMethodCallback mc = new ListSavingMethodCallback();
    ReflectionUtils.doWithMethods(TestObjectSubclass.class, mc);
    int absquatulateCount = 0;
    for (String name : mc.getMethodNames()) {
      if (name.equals("absquatulate")) {
        ++absquatulateCount;
      }
    }
    assertThat(absquatulateCount).as("Found 2 absquatulates").isEqualTo(2);
  }

  @Test
  public void testFindMethod() throws Exception {
    assertThat(ReflectionUtils.findMethod(B.class, "bar", String.class)).isNotNull();
    assertThat(ReflectionUtils.findMethod(B.class, "foo", Integer.class)).isNotNull();
    assertThat(ReflectionUtils.findMethod(B.class, "getClass")).isNotNull();
  }

  @Test
  public void testGetAllDeclaredMethods() throws Exception {
    class Foo {
      @Override
      public String toString() {
        return super.toString();
      }
    }
    int toStringMethodCount = 0;
    final Method[] allDeclaredMethods = ReflectionUtils.getAllDeclaredMethods(Foo.class);
    for (Method method : allDeclaredMethods) {
      if (method.getName().equals("toString")) {
        toStringMethodCount++;
      }
    }
    assertThat(toStringMethodCount).isEqualTo(2);
  }

  @Test
  public void testGetUniqueDeclaredMethods() throws Exception {
    class Foo {
      @Override
      public String toString() {
        return super.toString();
      }
    }
    int toStringMethodCount = 0;
    for (Method method : ReflectionUtils.getUniqueDeclaredMethods(Foo.class)) {
      if (method.getName().equals("toString")) {
        toStringMethodCount++;
      }
    }
    assertThat(toStringMethodCount).isEqualTo(1);
  }

  @Test
  public void testGetUniqueDeclaredMethods_withCovariantReturnType() throws Exception {
    class Parent {
      @SuppressWarnings("unused")
      public Number m1() {
        return Integer.valueOf(42);
      }
    }
    class Leaf extends Parent {
      @Override
      public Integer m1() {
        return Integer.valueOf(42);
      }
    }
    int m1MethodCount = 0;
    Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(Leaf.class);
    for (Method method : methods) {
      if (method.getName().equals("m1")) {
        m1MethodCount++;
      }
    }
    assertThat(m1MethodCount).isEqualTo(1);
  }

  @Test
  public void testGetUniqueDeclaredMethods_isFastEnough() {
    @SuppressWarnings("unused")
    class C { //@formatter:off
            void m00() { } void m01() { } void m02() { } void m03() { } void m04() { }
            void m05() { } void m06() { } void m07() { } void m08() { } void m09() { }
            void m10() { } void m11() { } void m12() { } void m13() { } void m14() { }
            void m15() { } void m16() { } void m17() { } void m18() { } void m19() { }
            void m20() { } void m21() { } void m22() { } void m23() { } void m24() { }
            void m25() { } void m26() { } void m27() { } void m28() { } void m29() { }
            void m30() { } void m31() { } void m32() { } void m33() { } void m34() { }
            void m35() { } void m36() { } void m37() { } void m38() { } void m39() { }
            void m40() { } void m41() { } void m42() { } void m43() { } void m44() { }
            void m45() { } void m46() { } void m47() { } void m48() { } void m49() { }
            void m50() { } void m51() { } void m52() { } void m53() { } void m54() { }
            void m55() { } void m56() { } void m57() { } void m58() { } void m59() { }
            void m60() { } void m61() { } void m62() { } void m63() { } void m64() { }
            void m65() { } void m66() { } void m67() { } void m68() { } void m69() { }
            void m70() { } void m71() { } void m72() { } void m73() { } void m74() { }
            void m75() { } void m76() { } void m77() { } void m78() { } void m79() { }
            void m80() { } void m81() { } void m82() { } void m83() { } void m84() { }
            void m85() { } void m86() { } void m87() { } void m88() { } void m89() { }
            void m90() { } void m91() { } void m92() { } void m93() { } void m94() { }
            void m95() { } void m96() { } void m97() { } void m98() { } void m99() { }
        } //@formatter:on

    Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(C.class);
    assertThat(methods.length).isGreaterThan(100);
  }

  @Test
  public void testGetDeclaredMethodsReturnsCopy() {
    Method[] m1 = ReflectionUtils.getDeclaredMethods(A.class);
    Method[] m2 = ReflectionUtils.getDeclaredMethods(A.class);
    assertThat(m1).isNotSameAs(m2);
  }

  private static class ListSavingMethodCallback implements ReflectionUtils.MethodCallback {

    private List<String> methodNames = new ArrayList<>();

    private List<Method> methods = new ArrayList<>();

    @Override
    public void doWith(Method m) throws IllegalArgumentException, IllegalAccessException {
      this.methodNames.add(m.getName());
      this.methods.add(m);
    }

    public List<String> getMethodNames() {
      return this.methodNames;
    }

    @SuppressWarnings("unused")
    public List<Method> getMethods() {
      return this.methods;
    }
  }

  private static class TestObjectSubclass extends TestObject {

    @Override
    public void absquatulate() {
      throw new UnsupportedOperationException();
    }
  }

  private static class TestObjectSubclassWithPublicField extends TestObject {

    @SuppressWarnings("unused")
    public String publicField = "foo";
  }

  private static class TestObjectSubclassWithNewField extends TestObject {

    private int magic;

    protected String prot = "foo";
  }

  private static class TestObjectSubclassWithFinalField extends TestObject {

    @SuppressWarnings("unused")
    private final String foo = "will break naive copy that doesn't exclude statics";
  }

  private static class A {

    @SuppressWarnings("unused")
    private void foo(Integer i) throws RemoteException { }
  }

  @SuppressWarnings("unused")
  private static class B extends A {

    void bar(String s) throws IllegalArgumentException { }

    int add(int... args) {
      int sum = 0;
      for (int i = 0; i < args.length; i++) {
        sum += args[i];
      }
      return sum;
    }
  }

  // --------------------

  @Test
  public void getterPropertyName() {

    assertThat(ReflectionUtils.getterPropertyName("isName", boolean.class))
            .isEqualTo("isName");
    assertThat(ReflectionUtils.getterPropertyName("isName", Boolean.class))
            .isEqualTo("getIsName");
    assertThat(ReflectionUtils.getterPropertyName("isName", String.class))
            .isEqualTo("getIsName");

  }

  @Test
  public void setterPropertyName() {
    assertThat(ReflectionUtils.setterPropertyName("isName", boolean.class))
            .isEqualTo("setName");
    assertThat(ReflectionUtils.setterPropertyName("isName", Boolean.class))
            .isEqualTo("setIsName");
    assertThat(ReflectionUtils.setterPropertyName("isName", String.class))
            .isEqualTo("setIsName");

  }

  @Test
  public void getMethodIfAvailable() {
    Method method = ReflectionUtils.getMethodIfAvailable(Collection.class, "size");
    assertThat(method).isNotNull();
    assertThat(method.getName()).isEqualTo("size");

    method = ReflectionUtils.getMethodIfAvailable(Collection.class, "remove", Object.class);
    assertThat(method).isNotNull();
    assertThat(method.getName()).isEqualTo("remove");

    assertThat(ReflectionUtils.getMethodIfAvailable(Collection.class, "remove")).isNull();
    assertThat(ReflectionUtils.getMethodIfAvailable(Collection.class, "someOtherMethod")).isNull();
  }

  @Test
  public void hasMethod() {
    assertThat(ReflectionUtils.hasMethod(Collection.class, "size")).isTrue();
    assertThat(ReflectionUtils.hasMethod(Collection.class, "remove", Object.class)).isTrue();
    assertThat(ReflectionUtils.hasMethod(Collection.class, "remove")).isFalse();
    assertThat(ReflectionUtils.hasMethod(Collection.class, "someOtherMethod")).isFalse();
  }

  @Test
  public void getMethodCountForName() {
    assertThat(ReflectionUtils.getMethodCountForName(OverloadedMethodsClass.class, "print"))
            .as("Verifying number of overloaded 'print' methods for OverloadedMethodsClass.")
            .isEqualTo(2);
    assertThat(ReflectionUtils.getMethodCountForName(SubOverloadedMethodsClass.class, "print"))
            .as("Verifying number of overloaded 'print' methods for SubgetPackageNameOverloadedMethodsClass.")
            .isEqualTo(4);
  }

  @Test
  public void argsStaticMethod() throws IllegalAccessException, InvocationTargetException {
    Method method = ReflectionUtils.getStaticMethod(NestedClass.class, "argStaticMethod", String.class);
    method.invoke(null, "test");
    assertThat(NestedClass.argCalled).as("argument method was not invoked.").isTrue();
  }

  @Nested
  class GetInterfaceMethodTests {

    @Test
    void publicMethodInPublicClass() throws Exception {
      Class<?> originalType = String.class;
      Method originalMethod = originalType.getDeclaredMethod("getBytes");

      Method interfaceMethod = ReflectionUtils.getInterfaceMethodIfPossible(originalMethod, null);
      assertThat(interfaceMethod.getDeclaringClass()).isEqualTo(originalType);
      assertThat(interfaceMethod).isSameAs(originalMethod);
      assertNotInterfaceMethod(interfaceMethod);
      assertPubliclyAccessible(interfaceMethod);
    }

    @Test
    void publicMethodInNonPublicInterface() throws Exception {
      Class<?> originalType = PrivateInterface.class;
      Method originalMethod = originalType.getDeclaredMethod("getMessage");

      // Prerequisites for this use case:
      assertPublic(originalMethod);
      assertNotPublic(originalMethod.getDeclaringClass());

      Method interfaceMethod = ReflectionUtils.getInterfaceMethodIfPossible(originalMethod, null);
      assertThat(interfaceMethod).isSameAs(originalMethod);
      assertInterfaceMethod(interfaceMethod);
      assertNotPubliclyAccessible(interfaceMethod);
    }

    @Test
    void publicInterfaceMethodInPublicClass() throws Exception {
      Class<?> originalType = ArrayList.class;
      Method originalMethod = originalType.getDeclaredMethod("size");

      Method interfaceMethod = ReflectionUtils.getInterfaceMethodIfPossible(originalMethod, null);
      assertThat(interfaceMethod.getDeclaringClass()).isEqualTo(List.class);
      assertThat(interfaceMethod.getName()).isEqualTo("size");
      assertThat(interfaceMethod.getParameterTypes()).isEmpty();
      assertInterfaceMethod(interfaceMethod);
      assertPubliclyAccessible(interfaceMethod);
    }

    @Test
    void publicInterfaceMethodDeclaredInNonPublicClassWithLateBindingOfClassMethodToSubclassDeclaredInterface() throws Exception {
      HashMap<String, String> hashMap = new HashMap<>();
      // Returns a package-private java.util.HashMap.KeyIterator which extends java.util.HashMap.HashIterator
      // which declares hasNext(), even though HashIterator does not implement Iterator. Rather, KeyIterator
      // implements HashIterator.
      Iterator<String> iterator = hashMap.keySet().iterator();
      Class<?> targetClass = iterator.getClass();

      // Prerequisites for this use case:
      assertNotPublic(targetClass);

      Method originalMethod = targetClass.getMethod("hasNext");

      Method interfaceMethod = ReflectionUtils.getInterfaceMethodIfPossible(originalMethod, targetClass);
      assertThat(interfaceMethod.getDeclaringClass()).isEqualTo(Iterator.class);
      assertThat(interfaceMethod.getName()).isEqualTo("hasNext");
      assertThat(interfaceMethod.getParameterTypes()).isEmpty();
      assertInterfaceMethod(interfaceMethod);
      assertPubliclyAccessible(interfaceMethod);
    }

    @Test
    void privateSubclassOverridesPropertyInPublicInterface() throws Exception {
      Method originalMethod = PrivateSubclass.class.getDeclaredMethod("getText");

      // Prerequisite: type must not be public for this use case.
      assertNotPublic(originalMethod.getDeclaringClass());

      Method interfaceMethod = ReflectionUtils.getInterfaceMethodIfPossible(originalMethod, null);
      assertThat(interfaceMethod.getDeclaringClass()).isEqualTo(PublicInterface.class);
      assertThat(interfaceMethod.getName()).isEqualTo("getText");
      assertThat(interfaceMethod.getParameterTypes()).isEmpty();
      assertInterfaceMethod(interfaceMethod);
      assertPubliclyAccessible(interfaceMethod);
    }

    @Test
    void privateSubclassOverridesPropertyInPrivateInterface() throws Exception {
      Method originalMethod = PrivateSubclass.class.getDeclaredMethod("getMessage");

      // Prerequisite: type must not be public for this use case.
      assertNotPublic(originalMethod.getDeclaringClass());

      Method interfaceMethod = ReflectionUtils.getInterfaceMethodIfPossible(originalMethod, null);
      assertThat(interfaceMethod.getDeclaringClass()).isEqualTo(PrivateInterface.class);
      assertThat(interfaceMethod.getName()).isEqualTo("getMessage");
      assertThat(interfaceMethod.getParameterTypes()).isEmpty();
      assertInterfaceMethod(interfaceMethod);
      assertNotPubliclyAccessible(interfaceMethod);
    }

    @Test
    void packagePrivateSubclassOverridesMethodInPublicInterface() throws Exception {
      List<String> unmodifiableList = Collections.unmodifiableList(Arrays.asList("foo", "bar"));
      Class<?> targetClass = unmodifiableList.getClass();

      // Prerequisites for this use case:
      assertNotPublic(targetClass);

      Method originalMethod = targetClass.getMethod("contains", Object.class);

      // Prerequisite: type must not be public for this use case.
      assertNotPublic(originalMethod.getDeclaringClass());

      Method interfaceMethod = ReflectionUtils.getInterfaceMethodIfPossible(originalMethod, null);
      assertThat(interfaceMethod.getDeclaringClass()).isEqualTo(Collection.class);
      assertThat(interfaceMethod.getName()).isEqualTo("contains");
      assertThat(interfaceMethod.getParameterTypes()).containsExactly(Object.class);
      assertInterfaceMethod(interfaceMethod);
      assertPubliclyAccessible(interfaceMethod);
    }

    @Test
    void privateSubclassOverridesMethodInPrivateInterface() throws Exception {
      Method originalMethod = PrivateSubclass.class.getMethod("greet", String.class);

      // Prerequisite: type must not be public for this use case.
      assertNotPublic(originalMethod.getDeclaringClass());

      Method interfaceMethod = ReflectionUtils.getInterfaceMethodIfPossible(originalMethod, null);
      assertThat(interfaceMethod.getDeclaringClass()).isEqualTo(PrivateInterface.class);
      assertThat(interfaceMethod.getName()).isEqualTo("greet");
      assertThat(interfaceMethod.getParameterTypes()).containsExactly(String.class);
      assertInterfaceMethod(interfaceMethod);
      assertNotPubliclyAccessible(interfaceMethod);
    }

  }

  @Nested
  class GetPubliclyAccessibleMethodTests {

    @Test
    void nonPublicMethod(TestInfo testInfo) {
      Method originalMethod = testInfo.getTestMethod().get();

      // Prerequisites for this use case:
      assertNotPublic(originalMethod);

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      assertThat(publiclyAccessibleMethod).isSameAs(originalMethod);
      assertNotPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    // This method is intentionally public.
    public void publicMethodInNonPublicClass(TestInfo testInfo) {
      Method originalMethod = testInfo.getTestMethod().get();

      // Prerequisites for this use case:
      assertPublic(originalMethod);
      assertNotPublic(originalMethod.getDeclaringClass());

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      assertThat(publiclyAccessibleMethod).isSameAs(originalMethod);
      assertNotPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void publicMethodInNonPublicInterface() throws Exception {
      Class<?> originalType = PrivateInterface.class;
      Method originalMethod = originalType.getDeclaredMethod("getMessage");

      // Prerequisites for this use case:
      assertPublic(originalMethod);
      assertNotPublic(originalMethod.getDeclaringClass());

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      assertThat(publiclyAccessibleMethod).isSameAs(originalMethod);
      assertNotPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void publicMethodInObjectClass() throws Exception {
      Class<?> originalType = String.class;
      Method originalMethod = originalType.getDeclaredMethod("hashCode");

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      assertThat(publiclyAccessibleMethod.getDeclaringClass()).isEqualTo(Object.class);
      assertThat(publiclyAccessibleMethod.getName()).isEqualTo("hashCode");
      assertPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void publicInterfaceMethodInPublicClass() throws Exception {
      Class<?> originalType = ArrayList.class;
      Method originalMethod = originalType.getDeclaredMethod("size");

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      // Should find the interface method in List.
      assertThat(publiclyAccessibleMethod.getDeclaringClass()).isEqualTo(List.class);
      assertThat(publiclyAccessibleMethod.getName()).isEqualTo("size");
      assertPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void publicMethodInJavaLangObjectDeclaredInNonPublicClass() throws Exception {
      List<String> unmodifiableList = Collections.unmodifiableList(Arrays.asList("foo", "bar"));
      Class<?> targetClass = unmodifiableList.getClass();

      // Prerequisites for this use case:
      assertNotPublic(targetClass);

      Method originalMethod = targetClass.getMethod("toString");

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      assertThat(publiclyAccessibleMethod.getDeclaringClass()).isEqualTo(Object.class);
      assertThat(publiclyAccessibleMethod.getName()).isEqualTo("toString");
      assertThat(publiclyAccessibleMethod.getParameterTypes()).isEmpty();
      assertPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void publicMethodInJavaTimeZoneIdDeclaredInNonPublicSubclass() throws Exception {
      // Returns a package-private java.time.ZoneRegion.
      ZoneId zoneId = ZoneId.of("CET");
      Class<?> targetClass = zoneId.getClass();

      // Prerequisites for this use case:
      assertNotPublic(targetClass);

      Method originalMethod = targetClass.getDeclaredMethod("getId");

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      assertThat(publiclyAccessibleMethod.getDeclaringClass()).isEqualTo(ZoneId.class);
      assertThat(publiclyAccessibleMethod.getName()).isEqualTo("getId");
      assertThat(publiclyAccessibleMethod.getParameterTypes()).isEmpty();
      assertPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void publicInterfaceMethodDeclaredInNonPublicClassWithLateBindingOfClassMethodToSubclassDeclaredInterface() throws Exception {
      HashMap<String, String> hashMap = new HashMap<>();
      // Returns a package-private java.util.HashMap.KeyIterator which extends java.util.HashMap.HashIterator
      // which declares hasNext(), even though HashIterator does not implement Iterator. Rather, KeyIterator
      // implements HashIterator.
      Iterator<String> iterator = hashMap.keySet().iterator();
      Class<?> targetClass = iterator.getClass();

      // Prerequisites for this use case:
      assertNotPublic(targetClass);

      Method originalMethod = targetClass.getMethod("hasNext");

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, targetClass);
      assertThat(publiclyAccessibleMethod.getDeclaringClass()).isEqualTo(Iterator.class);
      assertThat(publiclyAccessibleMethod.getName()).isEqualTo("hasNext");
      assertThat(publiclyAccessibleMethod.getParameterTypes()).isEmpty();
      assertPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void privateSubclassOverridesPropertyInPublicInterface() throws Exception {
      Method originalMethod = PrivateSubclass.class.getDeclaredMethod("getText");

      // Prerequisite: type must not be public for this use case.
      assertNotPublic(originalMethod.getDeclaringClass());

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      assertThat(publiclyAccessibleMethod.getDeclaringClass()).isEqualTo(PublicInterface.class);
      assertThat(publiclyAccessibleMethod.getName()).isEqualTo("getText");
      assertThat(publiclyAccessibleMethod.getParameterTypes()).isEmpty();
      assertPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void privateSubclassOverridesPropertyInPrivateInterface() throws Exception {
      Method originalMethod = PrivateSubclass.class.getDeclaredMethod("getMessage");

      // Prerequisite: type must not be public for this use case.
      assertNotPublic(originalMethod.getDeclaringClass());

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      // Should not find the interface method in PrivateInterface.
      assertThat(publiclyAccessibleMethod.getDeclaringClass()).isEqualTo(PublicSuperclass.class);
      assertThat(publiclyAccessibleMethod.getName()).isEqualTo("getMessage");
      assertThat(publiclyAccessibleMethod.getParameterTypes()).isEmpty();
      assertPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void privateSubclassOverridesPropertyInPublicSuperclass() throws Exception {
      Method originalMethod = PrivateSubclass.class.getDeclaredMethod("getNumber");

      // Prerequisite: type must not be public for this use case.
      assertNotPublic(originalMethod.getDeclaringClass());

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      assertThat(publiclyAccessibleMethod.getDeclaringClass()).isEqualTo(PublicSuperclass.class);
      assertThat(publiclyAccessibleMethod.getName()).isEqualTo("getNumber");
      assertThat(publiclyAccessibleMethod.getParameterTypes()).isEmpty();
      assertPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void packagePrivateSubclassOverridesMethodInPublicInterface() throws Exception {
      List<String> unmodifiableList = Collections.unmodifiableList(Arrays.asList("foo", "bar"));
      Class<?> targetClass = unmodifiableList.getClass();

      // Prerequisites for this use case:
      assertNotPublic(targetClass);

      Method originalMethod = targetClass.getMethod("contains", Object.class);

      // Prerequisite: type must not be public for this use case.
      assertNotPublic(originalMethod.getDeclaringClass());

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      assertThat(publiclyAccessibleMethod.getDeclaringClass()).isEqualTo(Collection.class);
      assertThat(publiclyAccessibleMethod.getName()).isEqualTo("contains");
      assertThat(publiclyAccessibleMethod.getParameterTypes()).containsExactly(Object.class);
      assertPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void privateSubclassOverridesMethodInPrivateInterface() throws Exception {
      Method originalMethod = PrivateSubclass.class.getMethod("greet", String.class);

      // Prerequisite: type must not be public for this use case.
      assertNotPublic(originalMethod.getDeclaringClass());

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      assertThat(publiclyAccessibleMethod.getDeclaringClass()).isEqualTo(PublicSuperclass.class);
      assertThat(publiclyAccessibleMethod.getName()).isEqualTo("greet");
      assertThat(publiclyAccessibleMethod.getParameterTypes()).containsExactly(String.class);
      assertPubliclyAccessible(publiclyAccessibleMethod);
    }

    @Test
    void privateSubclassOverridesMethodInPublicSuperclass() throws Exception {
      Method originalMethod = PrivateSubclass.class.getMethod("process", int.class);

      // Prerequisite: type must not be public for this use case.
      assertNotPublic(originalMethod.getDeclaringClass());

      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(originalMethod, null);
      assertThat(publiclyAccessibleMethod.getDeclaringClass()).isEqualTo(PublicSuperclass.class);
      assertThat(publiclyAccessibleMethod.getName()).isEqualTo("process");
      assertThat(publiclyAccessibleMethod.getParameterTypes()).containsExactly(int.class);
      assertPubliclyAccessible(publiclyAccessibleMethod);
    }

  }

  private static void assertInterfaceMethod(Method method) {
    assertThat(method.getDeclaringClass()).as("%s must be an interface method", method).isInterface();
  }

  private static void assertNotInterfaceMethod(Method method) {
    assertThat(method.getDeclaringClass()).as("%s must not be an interface method", method).isNotInterface();
  }

  private static void assertPubliclyAccessible(Method method) {
    assertPublic(method);
    assertPublic(method.getDeclaringClass());
  }

  private static void assertNotPubliclyAccessible(Method method) {
    assertThat(!isPublic(method) || !isPublic(method.getDeclaringClass()))
            .as("%s must not be publicly accessible", method)
            .isTrue();
  }

  private static void assertPublic(Member member) {
    assertThat(isPublic(member)).as("%s must be public", member).isTrue();
  }

  private static void assertPublic(Class<?> clazz) {
    assertThat(isPublic(clazz)).as("%s must be public", clazz).isTrue();
  }

  private static void assertNotPublic(Member member) {
    assertThat(!isPublic(member)).as("%s must be not be public", member).isTrue();
  }

  private static void assertNotPublic(Class<?> clazz) {
    assertThat(!isPublic(clazz)).as("%s must be not be public", clazz).isTrue();
  }

  private static boolean isPublic(Class<?> clazz) {
    return Modifier.isPublic(clazz.getModifiers());
  }

  private static boolean isPublic(Member member) {
    return Modifier.isPublic(member.getModifiers());
  }

  @SuppressWarnings("unused")
  private static class OverloadedMethodsClass {

    public void print(String messages) {
      /* no-op */
    }

    public void print(String[] messages) {
      /* no-op */
    }
  }

  @SuppressWarnings("unused")
  private static class SubOverloadedMethodsClass extends OverloadedMethodsClass {

    public void print(String header, String[] messages) {
      /* no-op */
    }

    void print(String header, String[] messages, String footer) {
      /* no-op */
    }
  }

  public static class NestedClass {

    static boolean noArgCalled;
    static boolean argCalled;
    static boolean overloadedCalled;

    public static void staticMethod() {
      noArgCalled = true;
    }

    public static void staticMethod(String anArg) {
      overloadedCalled = true;
    }

    public static void argStaticMethod(String anArg) {
      argCalled = true;
    }
  }

  private interface PrivateInterface {

    String getMessage();

    String greet(String name);
  }

  private static class PrivateSubclass extends PublicSuperclass implements PublicInterface, PrivateInterface {

    @Override
    public int getNumber() {
      return 2;
    }

    @Override
    public String getMessage() {
      return "hello";
    }

    @Override
    public String greet(String name) {
      return "Hello, " + name;
    }

    @Override
    public int process(int num) {
      return num * 2;
    }

    @Override
    public String getText() {
      return "enigma";
    }

  }

}
