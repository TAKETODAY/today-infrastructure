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

package cn.taketoday.util;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.Autowired;
import cn.taketoday.beans.Prototype;
import cn.taketoday.beans.Singleton;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.core.bytecode.proxy.Enhancer;
import cn.taketoday.core.bytecode.proxy.MethodInterceptor;
import cn.taketoday.core.bytecode.proxy.MethodProxy;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.objects.TestObject;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Today <br>
 * 2018-06-0? ?
 */
@Singleton("singleton")
@Prototype("prototype")
public class ClassUtilsTest {
  private static final Logger log = LoggerFactory.getLogger(ClassUtilsTest.class);
  final ClassLoader classLoader = getClass().getClassLoader();

//    public static void main(String[] args) {
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < 10; i++) {
//            try (ApplicationContext applicationContext = new StandardApplicationContext("", "")) {
//                System.err.println(applicationContext.getBean(User.class));
//            }
//        }
//        System.err.println(System.currentTimeMillis() - start + "ms");
//    }

  @Test
  public void resolvePrimitiveClassName() {

    assert ClassUtils.resolvePrimitiveClassName("java.lang.Float") == null;
    assert ClassUtils.resolvePrimitiveClassName("float") == float.class;
    assert ClassUtils.resolvePrimitiveClassName(null) == null;
  }

  @Test
  public void forName() throws ClassNotFoundException {

    assert ClassUtils.forName("java.lang.Float") == Float.class;
    assert ClassUtils.forName("float") == float.class;
    assert ClassUtils.forName("java.lang.String[]") == String[].class;
    assert ClassUtils.forName("[Ljava.lang.String;") == String[].class;
    assert ClassUtils.forName("[[Ljava.lang.String;") == String[][].class;

    try {
      ClassUtils.forName("Float");
    }
    catch (ClassNotFoundException e) { }
    assert ClassUtils.forName("cn.taketoday.util.ClassUtilsTest.INNER") == INNER.class;
    try {
      ClassUtils.forName("cn.taketoday.util.ClassUtilsTest.INNERs");//
    }
    catch (ClassNotFoundException e) { }
    assertThat(ClassUtils.forName("java.lang.String", classLoader)).isEqualTo(String.class);
    assertThat(ClassUtils.forName("java.lang.String[]", classLoader)).isEqualTo(String[].class);
    assertThat(ClassUtils.forName(String[].class.getName(), classLoader)).isEqualTo(String[].class);
    assertThat(ClassUtils.forName(String[][].class.getName(), classLoader)).isEqualTo(String[][].class);
    assertThat(ClassUtils.forName(String[][][].class.getName(), classLoader)).isEqualTo(String[][][].class);
    assertThat(ClassUtils.forName("cn.taketoday.context.objects.TestObject", classLoader)).isEqualTo(TestObject.class);
    assertThat(ClassUtils.forName("cn.taketoday.context.objects.TestObject[]", classLoader)).isEqualTo(TestObject[].class);
    assertThat(ClassUtils.forName(TestObject[].class.getName(), classLoader)).isEqualTo(TestObject[].class);
    assertThat(ClassUtils.forName("cn.taketoday.context.objects.TestObject[][]", classLoader)).isEqualTo(TestObject[][].class);
    assertThat(ClassUtils.forName(TestObject[][].class.getName(), classLoader)).isEqualTo(TestObject[][].class);
    assertThat(ClassUtils.forName("[[[S", classLoader)).isEqualTo(short[][][].class);

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

  @Test
  public void forNameWithNestedType() throws ClassNotFoundException {
    assertThat(ClassUtils.forName("cn.taketoday.util.ClassUtilsTest$NestedClass", classLoader)).isEqualTo(NestedClass.class);
    assertThat(ClassUtils.forName("cn.taketoday.util.ClassUtilsTest.NestedClass", classLoader)).isEqualTo(NestedClass.class);
  }

  @Test
  public void forNameWithPrimitiveClasses() throws ClassNotFoundException {
    assertThat(ClassUtils.forName("boolean", classLoader)).isEqualTo(boolean.class);
    assertThat(ClassUtils.forName("byte", classLoader)).isEqualTo(byte.class);
    assertThat(ClassUtils.forName("char", classLoader)).isEqualTo(char.class);
    assertThat(ClassUtils.forName("short", classLoader)).isEqualTo(short.class);
    assertThat(ClassUtils.forName("int", classLoader)).isEqualTo(int.class);
    assertThat(ClassUtils.forName("long", classLoader)).isEqualTo(long.class);
    assertThat(ClassUtils.forName("float", classLoader)).isEqualTo(float.class);
    assertThat(ClassUtils.forName("double", classLoader)).isEqualTo(double.class);
    assertThat(ClassUtils.forName("void", classLoader)).isEqualTo(void.class);
  }

  @Test
  public void forNameWithPrimitiveArrays() throws ClassNotFoundException {
    assertThat(ClassUtils.forName("boolean[]", classLoader)).isEqualTo(boolean[].class);
    assertThat(ClassUtils.forName("byte[]", classLoader)).isEqualTo(byte[].class);
    assertThat(ClassUtils.forName("char[]", classLoader)).isEqualTo(char[].class);
    assertThat(ClassUtils.forName("short[]", classLoader)).isEqualTo(short[].class);
    assertThat(ClassUtils.forName("int[]", classLoader)).isEqualTo(int[].class);
    assertThat(ClassUtils.forName("long[]", classLoader)).isEqualTo(long[].class);
    assertThat(ClassUtils.forName("float[]", classLoader)).isEqualTo(float[].class);
    assertThat(ClassUtils.forName("double[]", classLoader)).isEqualTo(double[].class);
  }

  @Test
  public void forNameWithPrimitiveArraysInternalName() throws ClassNotFoundException {
    assertThat(ClassUtils.forName(boolean[].class.getName(), classLoader)).isEqualTo(boolean[].class);
    assertThat(ClassUtils.forName(byte[].class.getName(), classLoader)).isEqualTo(byte[].class);
    assertThat(ClassUtils.forName(char[].class.getName(), classLoader)).isEqualTo(char[].class);
    assertThat(ClassUtils.forName(short[].class.getName(), classLoader)).isEqualTo(short[].class);
    assertThat(ClassUtils.forName(int[].class.getName(), classLoader)).isEqualTo(int[].class);
    assertThat(ClassUtils.forName(long[].class.getName(), classLoader)).isEqualTo(long[].class);
    assertThat(ClassUtils.forName(float[].class.getName(), classLoader)).isEqualTo(float[].class);
    assertThat(ClassUtils.forName(double[].class.getName(), classLoader)).isEqualTo(double[].class);
  }

  private static class INNER {

  }

  @Test
  public void isPresent() {

    assert ClassUtils.isPresent("java.lang.Float");
    assert !ClassUtils.isPresent("Float");
  }

  @Test
  public void testAutowiredOnConstructor() {
    try (ApplicationContext context = new StandardApplicationContext(new HashSet<>())) {
      context.importBeans(AutowiredOnConstructor.class, AutowiredOnConstructorThrow.class);

      assertThat(context.getBean(AutowiredOnConstructor.class))
              .isNotNull();

      context.registerBean("testAutowiredOnConstructorThrow", AutowiredOnConstructorThrow.class);
      try {
        context.getBean(AutowiredOnConstructorThrow.class);
        assert false;
      }
      catch (Exception e) {
        assert true;
      }
    }
  }

  @Test
  public void testOther() throws NoSuchMethodException, SecurityException, ClassNotFoundException {
    final Method method = AutowiredOnConstructor.class.getDeclaredMethod("test");
    ReflectionUtils.accessInvokeMethod(method, new AutowiredOnConstructor(null));

    assert BeanUtils.newInstance(ClassUtilsTest.class.getName()) != null;

    try {
      BeanUtils.newInstance("not found");
      assert false;
    }
    catch (Exception e) {
      assert true;
    }
    try {

      final Method throwing = AutowiredOnConstructor.class.getDeclaredMethod("throwing");
      ReflectionUtils.accessInvokeMethod(throwing, new AutowiredOnConstructor(null));

      assert false;
    }
    catch (Exception e) {
      assert true;
    }
  }

  @MySingleton
  @SuppressWarnings("unused")
  public static class AutowiredOnConstructor {

    @Autowired
    private AutowiredOnConstructor(ApplicationContext context) {
      System.err.println("init");
    }

    private void test() {
      System.err.println("test");
    }

    private void throwing() {
      throw new ApplicationContextException();
    }
  }

  @Singleton
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  public @interface MySingleton {

  }

  static class AutowiredOnConstructorThrow {

    public AutowiredOnConstructorThrow(ApplicationContext applicationContext) {
      System.err.println("init");
      throw new ApplicationContextException();
    }

  }

  // v2.1.7 test code
  // ----------------------------------------

  @Test
  public void testGetUserClass() {
    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(getClass()));
    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(getClass().getName()));
    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(ClassUtilsTest.class.getName()));

    Enhancer enhancer = new Enhancer();

    enhancer.setCallback(new MethodInterceptor() {
      @Override
      public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        return null;
      }
    });

    enhancer.setSuperclass(ClassUtilsTest.class);

    final Object create = enhancer.create();

    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(create));
    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(create.getClass()));
    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(create.getClass().getName()));
  }

  // ------

  static class Genericity<T> { }

  static class Generic extends Genericity<Integer> {
    List<?> list;
    List<String> stringList;
    Map<String, Object> stringMap;

    Generic(List<String> stringList,
            Map<String, Object> stringMap) { }

    void generic(List<String> stringList,
                 Map<String, Object> stringMap) { }
  }

  @Test
  public void testGetGenerics() {
    assertThat(ClassUtils.getGenerics(Generic.class, Genericity.class))
            .isNotNull()
            .hasSize(1)
            .contains(Integer.class);

    // param
    Constructor<Generic> constructor = BeanUtils.getConstructor(Generic.class);
    Parameter[] parameters = constructor.getParameters();

    assertThat(ClassUtils.getGenericTypes(parameters[0]))
            .isNotNull()
            .hasSize(1)
            .contains(String.class);

    assertThat(ClassUtils.getGenericTypes(parameters[1]))
            .isNotNull()
            .hasSize(2)
            .contains(String.class, Object.class);

    Method method = ReflectionUtils.findMethod(Generic.class, "generic");

    assertThat(ClassUtils.getGenericTypes(method.getParameters()[0]))
            .isNotNull()
            .hasSize(1)
            .contains(String.class);

    assertThat(ClassUtils.getGenericTypes(method.getParameters()[1]))
            .isNotNull()
            .hasSize(2)
            .contains(String.class, Object.class);
//
//    Field list = ReflectionUtils.findField(Generic.class, "list");
//    final Type[] genericityClass = ClassUtils.getGenerics(list);
//    assertThat(genericityClass)
//            .isNotNull()
//            .hasSize(1);

//    assertThat(genericityClass[0])
//            .isEqualTo(Object.class)
//            .isInstanceOf(Class.class);

  }

  // fix bug
  // @off
  interface Interface<T> {}
  interface Interface1<T> {}
  interface Interface2<T> {}
  interface NestedGenericInterface extends Interface<String> {}
  static abstract class Abs {}
  static abstract class GenericAbs implements Interface<String> {}
  static class AbsGeneric extends Abs
          implements Interface<String>,
                     Interface1<Integer>,
                     Interface2<Interface<String>> {}
  static class GenericAbsGeneric extends GenericAbs
          implements Interface1<Integer>, Interface2<Interface<String>> {}
  static class NestedGenericInterfaceBean extends GenericAbs
          implements NestedGenericInterface, Interface1<Integer>, Interface2<Interface<String>> {}
  static class NoGeneric {}

  // @on

/*
  @Test
  public void test() {
    final Class<NoGeneric> noGeneric = NoGeneric.class;
    final Class<AbsGeneric> absGenericClass = AbsGeneric.class;
    final Class<GenericAbsGeneric> genericAbsGeneric = GenericAbsGeneric.class;
    final Class<NestedGenericInterfaceBean> nestedGenericInterfaceBeanClass = NestedGenericInterfaceBean.class;

    // 直接在第一级接口
    final java.lang.reflect.Type[] genericInterfaces = absGenericClass.getGenericInterfaces();

    for (final Type genericInterface : genericInterfaces) {
//      System.out.println(genericInterface);
    }
    // 在父类上找
    final Type genericSuperclass = genericAbsGeneric.getGenericSuperclass();

//    System.out.println(genericSuperclass);
    // 第一级没有
    final Type[] genericInterfaces1 = nestedGenericInterfaceBeanClass.getGenericInterfaces();
    for (final Type genericInterface : genericInterfaces1) {
//      System.out.println(genericInterface);
    }

    // 没有
//    System.out.println(Arrays.toString(noGeneric.getGenericInterfaces()));

    //

    for (final Type genericInterface : genericInterfaces) {
      System.out.println(genericInterface.getClass());
    }

  }*/

  @Test
  public void testGetGenericsInterface() {
    final Type[] generics = ClassUtils.getGenerics(AbsGeneric.class, Interface.class);
    final Type[] generics1 = ClassUtils.getGenerics(GenericAbsGeneric.class, Interface.class);
    final Type[] generics2 = ClassUtils.getGenerics(NestedGenericInterfaceBean.class, Interface.class);

    System.out.println(Arrays.toString(generics));
    System.out.println(Arrays.toString(generics1));
    System.out.println(Arrays.toString(generics2));

    assertThat(generics1)
            .hasSize(1)
            .isEqualTo(generics)
            .isEqualTo(generics2)
            .contains(String.class)
    ;

    assertThat(ClassUtils.getGenerics(NoGeneric.class))
            .isNull();

  }

  // super class generic-s

  static class SuperClass<T> {

  }

  static abstract class AbsSuperClass<T> extends SuperClass<T> {

  }

  static class Child extends AbsSuperClass<Integer> {

  }

  static class AbsSuperClassMultiple<A, T> extends SuperClass<T> {

  }

  static class ChildMultiple extends AbsSuperClassMultiple<Integer, String> {

  }

  static class ChildMultiple0 extends ChildMultiple {

  }

  @Test
  public void testGetGenericsSuperClass() {

    final Class[] generics = ClassUtils.getGenerics(Child.class, SuperClass.class);
    final Class[] ChildMultiple = ClassUtils.getGenerics(ChildMultiple.class, SuperClass.class);
    final Class[] ChildMultiple0 = ClassUtils.getGenerics(ChildMultiple0.class, SuperClass.class);

    assertThat(generics).hasSize(1);

    assertThat(ChildMultiple)
            .isEqualTo(ChildMultiple0)
            .contains(String.class);

  }

  //

  @Test
  public void isAssignable() {
    assertThat(ClassUtils.isAssignable(Object.class, Object.class)).isTrue();
    assertThat(ClassUtils.isAssignable(String.class, String.class)).isTrue();
    assertThat(ClassUtils.isAssignable(Object.class, String.class)).isTrue();
    assertThat(ClassUtils.isAssignable(Object.class, Integer.class)).isTrue();
    assertThat(ClassUtils.isAssignable(Number.class, Integer.class)).isTrue();
    assertThat(ClassUtils.isAssignable(Number.class, int.class)).isTrue();
    assertThat(ClassUtils.isAssignable(Integer.class, int.class)).isTrue();
    assertThat(ClassUtils.isAssignable(int.class, Integer.class)).isTrue();
    assertThat(ClassUtils.isAssignable(String.class, Object.class)).isFalse();
    assertThat(ClassUtils.isAssignable(Integer.class, Number.class)).isFalse();
    assertThat(ClassUtils.isAssignable(Integer.class, double.class)).isFalse();
    assertThat(ClassUtils.isAssignable(double.class, Integer.class)).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
          "boolean, boolean",
          "byte, byte",
          "char, char",
          "short, short",
          "int, int",
          "long, long",
          "float, float",
          "double, double",
          "[Z, boolean[]",
          "[B, byte[]",
          "[C, char[]",
          "[S, short[]",
          "[I, int[]",
          "[J, long[]",
          "[F, float[]",
          "[D, double[]"
  })
  void resolvePrimitiveClassName(String input, Class<?> output) {
    assertThat(ClassUtils.resolvePrimitiveClassName(input)).isEqualTo(output);
  }

  @ParameterizedTest
  @WrapperTypes
  public void isPrimitiveWrapper(Class<?> type) {
    assertThat(ClassUtils.isPrimitiveWrapper(type)).isTrue();
  }

  @ParameterizedTest
  @PrimitiveTypes
  public void isPrimitiveOrWrapperWithPrimitive(Class<?> type) {
    assertThat(ClassUtils.isPrimitiveOrWrapper(type)).isTrue();
  }

  @ParameterizedTest
  @WrapperTypes
  public void isPrimitiveOrWrapperWithWrapper(Class<?> type) {
    assertThat(ClassUtils.isPrimitiveOrWrapper(type)).isTrue();
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @ValueSource(classes = { Boolean.class, Character.class, Byte.class, Short.class,
          Integer.class, Long.class, Float.class, Double.class, Void.class }) @interface WrapperTypes {
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @ValueSource(classes = { boolean.class, char.class, byte.class, short.class,
          int.class, long.class, float.class, double.class, void.class }) @interface PrimitiveTypes {
  }

}
