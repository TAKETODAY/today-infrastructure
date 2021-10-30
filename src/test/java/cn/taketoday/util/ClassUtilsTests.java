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

package cn.taketoday.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.objects.DerivedTestObject;
import cn.taketoday.context.objects.ITestInterface;
import cn.taketoday.context.objects.ITestObject;
import cn.taketoday.context.objects.TestObject;
import cn.taketoday.core.bytecode.proxy.Enhancer;
import cn.taketoday.core.bytecode.proxy.MethodInterceptor;
import cn.taketoday.core.bytecode.proxy.MethodProxy;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Prototype;
import cn.taketoday.lang.Singleton;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Today <br>
 * 2018-06-0? ?
 */
@Singleton("singleton")
@Prototype("prototype")
class ClassUtilsTests {
  private static final Logger log = LoggerFactory.getLogger(ClassUtilsTests.class);
  final ClassLoader classLoader = getClass().getClassLoader();

  @Test
  void resolvePrimitiveClassName() {

    assert ClassUtils.resolvePrimitiveClassName("java.lang.Float") == null;
    assert ClassUtils.resolvePrimitiveClassName("float") == float.class;
    assert ClassUtils.resolvePrimitiveClassName(null) == null;
  }

  @Test
  void forName() throws ClassNotFoundException {

    assert ClassUtils.forName("java.lang.Float") == Float.class;
    assert ClassUtils.forName("float") == float.class;
    assert ClassUtils.forName("java.lang.String[]") == String[].class;
    assert ClassUtils.forName("[Ljava.lang.String;") == String[].class;
    assert ClassUtils.forName("[[Ljava.lang.String;") == String[][].class;

    try {
      ClassUtils.forName("Float");
    }
    catch (ClassNotFoundException e) {
    }
    assert ClassUtils.forName("cn.taketoday.util.ClassUtilsTests.INNER") == INNER.class;
    try {
      ClassUtils.forName("cn.taketoday.util.ClassUtilsTests.INNERs");//
    }
    catch (ClassNotFoundException e) {
    }
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
  void forNameWithNestedType() throws ClassNotFoundException {
    assertThat(ClassUtils.forName("cn.taketoday.util.ClassUtilsTests$NestedClass", classLoader)).isEqualTo(NestedClass.class);
    assertThat(ClassUtils.forName("cn.taketoday.util.ClassUtilsTests.NestedClass", classLoader)).isEqualTo(NestedClass.class);
  }

  @Test
  void forNameWithPrimitiveClasses() throws ClassNotFoundException {
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
  void forNameWithPrimitiveArrays() throws ClassNotFoundException {
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
  void forNameWithPrimitiveArraysInternalName() throws ClassNotFoundException {
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
  void isPresent() {
    assertThat(ClassUtils.isPresent("java.lang.String", classLoader)).isTrue();
    assertThat(ClassUtils.isPresent("java.lang.MySpecialString", classLoader)).isFalse();
    assert ClassUtils.isPresent("java.lang.Float");
    assert !ClassUtils.isPresent("Float");
  }

  @Test
  void testAutowiredOnConstructor() {

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      context.refresh();

      AutowireCapableBeanFactory beanFactory = context.getAutowireCapableBeanFactory();

      context.register(AutowiredOnConstructor.class, AutowiredOnConstructorThrow.class);

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
  void testOther() throws NoSuchMethodException, SecurityException, ClassNotFoundException {
    final Method method = AutowiredOnConstructor.class.getDeclaredMethod("test");
    ReflectionUtils.accessInvokeMethod(method, new AutowiredOnConstructor(null));

    assert BeanUtils.newInstance(ClassUtilsTests.class.getName()) != null;

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
  void testGetUserClass() {
    assertEquals(ClassUtilsTests.class, ClassUtils.getUserClass(getClass()));
    assertEquals(ClassUtilsTests.class, ClassUtils.getUserClass(getClass().getName()));
    assertEquals(ClassUtilsTests.class, ClassUtils.getUserClass(ClassUtilsTests.class.getName()));

    Enhancer enhancer = new Enhancer();

    enhancer.setCallback(new MethodInterceptor() {
      @Override
      public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        return null;
      }
    });

    enhancer.setSuperclass(ClassUtilsTests.class);

    final Object create = enhancer.create();

    assertEquals(ClassUtilsTests.class, ClassUtils.getUserClass(create));
    assertEquals(ClassUtilsTests.class, ClassUtils.getUserClass(create.getClass()));
    assertEquals(ClassUtilsTests.class, ClassUtils.getUserClass(create.getClass().getName()));
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
  void testGetGenerics() {
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
  // @formatter:off
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

  // @formatter:on

/*
  @Test
  void test() {
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
  void testGetGenericsInterface() {
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
  void testGetGenericsSuperClass() {

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
  void isAssignable() {
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

  @Test
  void isCacheSafe() {
    ClassLoader childLoader1 = new ClassLoader(classLoader) { };
    ClassLoader childLoader2 = new ClassLoader(classLoader) { };
    ClassLoader childLoader3 = new ClassLoader(classLoader) {
      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        return childLoader1.loadClass(name);
      }
    };
    Class<?> composite = ClassUtils.createCompositeInterface(
            new Class<?>[] { Serializable.class, Externalizable.class }, childLoader1);

    assertThat(ClassUtils.isCacheSafe(String.class, null)).isTrue();
    assertThat(ClassUtils.isCacheSafe(String.class, classLoader)).isTrue();
    assertThat(ClassUtils.isCacheSafe(String.class, childLoader1)).isTrue();
    assertThat(ClassUtils.isCacheSafe(String.class, childLoader2)).isTrue();
    assertThat(ClassUtils.isCacheSafe(String.class, childLoader3)).isTrue();
    assertThat(ClassUtils.isCacheSafe(NestedClass.class, null)).isFalse();
    assertThat(ClassUtils.isCacheSafe(NestedClass.class, classLoader)).isTrue();
    assertThat(ClassUtils.isCacheSafe(NestedClass.class, childLoader1)).isTrue();
    assertThat(ClassUtils.isCacheSafe(NestedClass.class, childLoader2)).isTrue();
    assertThat(ClassUtils.isCacheSafe(NestedClass.class, childLoader3)).isTrue();
    assertThat(ClassUtils.isCacheSafe(composite, null)).isFalse();
    assertThat(ClassUtils.isCacheSafe(composite, classLoader)).isFalse();
    assertThat(ClassUtils.isCacheSafe(composite, childLoader1)).isTrue();
    assertThat(ClassUtils.isCacheSafe(composite, childLoader2)).isFalse();
    assertThat(ClassUtils.isCacheSafe(composite, childLoader3)).isTrue();
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

  @Test
  void getAllInterfaces() {
    DerivedTestObject testBean = new DerivedTestObject();
    List<Class<?>> ifcs = Arrays.asList(ClassUtils.getAllInterfaces(testBean));
    assertThat(ifcs.size()).as("Correct number of interfaces").isEqualTo(4);
    assertThat(ifcs.contains(Serializable.class)).as("Contains Serializable").isTrue();
    assertThat(ifcs.contains(ITestObject.class)).as("Contains ITestBean").isTrue();
    assertThat(ifcs.contains(ITestInterface.class)).as("Contains IOther").isTrue();
  }

  @Test
  void classNamesToString() {
    List<Class<?>> ifcs = new ArrayList<>();
    ifcs.add(Serializable.class);
    ifcs.add(Runnable.class);
    assertThat(ifcs.toString()).isEqualTo("[interface java.io.Serializable, interface java.lang.Runnable]");
    assertThat(ClassUtils.classNamesToString(ifcs)).isEqualTo("[java.io.Serializable, java.lang.Runnable]");

    List<Class<?>> classes = new ArrayList<>();
    classes.add(ArrayList.class);
    classes.add(Integer.class);
    assertThat(classes.toString()).isEqualTo("[class java.util.ArrayList, class java.lang.Integer]");
    assertThat(ClassUtils.classNamesToString(classes)).isEqualTo("[java.util.ArrayList, java.lang.Integer]");

    assertThat(Collections.singletonList(List.class).toString()).isEqualTo("[interface java.util.List]");
    assertThat(ClassUtils.classNamesToString(List.class)).isEqualTo("[java.util.List]");

    assertThat(Collections.EMPTY_LIST.toString()).isEqualTo("[]");
    assertThat(ClassUtils.classNamesToString(Collections.emptyList())).isEqualTo("[]");
  }

  @Test
  void determineCommonAncestor() {
    assertThat(ClassUtils.determineCommonAncestor(Integer.class, Number.class)).isEqualTo(Number.class);
    assertThat(ClassUtils.determineCommonAncestor(Number.class, Integer.class)).isEqualTo(Number.class);
    assertThat(ClassUtils.determineCommonAncestor(Number.class, null)).isEqualTo(Number.class);
    assertThat(ClassUtils.determineCommonAncestor(null, Integer.class)).isEqualTo(Integer.class);
    assertThat(ClassUtils.determineCommonAncestor(Integer.class, Integer.class)).isEqualTo(Integer.class);

    assertThat(ClassUtils.determineCommonAncestor(Integer.class, Float.class)).isEqualTo(Number.class);
    assertThat(ClassUtils.determineCommonAncestor(Float.class, Integer.class)).isEqualTo(Number.class);
    assertThat(ClassUtils.determineCommonAncestor(Integer.class, String.class)).isNull();
    assertThat(ClassUtils.determineCommonAncestor(String.class, Integer.class)).isNull();

    assertThat(ClassUtils.determineCommonAncestor(List.class, Collection.class)).isEqualTo(Collection.class);
    assertThat(ClassUtils.determineCommonAncestor(Collection.class, List.class)).isEqualTo(Collection.class);
    assertThat(ClassUtils.determineCommonAncestor(Collection.class, null)).isEqualTo(Collection.class);
    assertThat(ClassUtils.determineCommonAncestor(null, List.class)).isEqualTo(List.class);
    assertThat(ClassUtils.determineCommonAncestor(List.class, List.class)).isEqualTo(List.class);

    assertThat(ClassUtils.determineCommonAncestor(List.class, Set.class)).isNull();
    assertThat(ClassUtils.determineCommonAncestor(Set.class, List.class)).isNull();
    assertThat(ClassUtils.determineCommonAncestor(List.class, Runnable.class)).isNull();
    assertThat(ClassUtils.determineCommonAncestor(Runnable.class, List.class)).isNull();

    assertThat(ClassUtils.determineCommonAncestor(List.class, ArrayList.class)).isEqualTo(List.class);
    assertThat(ClassUtils.determineCommonAncestor(ArrayList.class, List.class)).isEqualTo(List.class);
    assertThat(ClassUtils.determineCommonAncestor(List.class, String.class)).isNull();
    assertThat(ClassUtils.determineCommonAncestor(String.class, List.class)).isNull();
  }

  @ParameterizedTest
  @WrapperTypes
  void isPrimitiveWrapper(Class<?> type) {
    assertThat(ClassUtils.isPrimitiveWrapper(type)).isTrue();
  }

  @ParameterizedTest
  @PrimitiveTypes
  void isPrimitiveOrWrapperWithPrimitive(Class<?> type) {
    assertThat(ClassUtils.isPrimitiveOrWrapper(type)).isTrue();
  }

  @ParameterizedTest
  @WrapperTypes
  void isPrimitiveOrWrapperWithWrapper(Class<?> type) {
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

  @Test
  void classPackageAsResourcePath() {
    String result = ClassUtils.classPackageAsResourcePath(Proxy.class);
    assertThat(result).isEqualTo("java/lang/reflect");
  }

  @Test
  void addResourcePathToPackagePath() {
    String result = "java/lang/reflect/xyzabc.xml";
    assertThat(ClassUtils.addResourcePathToPackagePath(Proxy.class, "xyzabc.xml")).isEqualTo(result);
    assertThat(ClassUtils.addResourcePathToPackagePath(Proxy.class, "/xyzabc.xml")).isEqualTo(result);

    assertThat(ClassUtils.addResourcePathToPackagePath(Proxy.class, "a/b/c/d.xml")).isEqualTo("java/lang/reflect/a/b/c/d.xml");
  }

  @Test
  void getShortName() {
    String className = ClassUtils.getShortName(getClass());
    assertThat(className).as("Class name did not match").isEqualTo("ClassUtilsTests");
  }

  @Test
  void getShortNameForObjectArrayClass() {
    String className = ClassUtils.getShortName(Object[].class);
    assertThat(className).as("Class name did not match").isEqualTo("Object[]");
  }

  @Test
  void getShortNameForMultiDimensionalObjectArrayClass() {
    String className = ClassUtils.getShortName(Object[][].class);
    assertThat(className).as("Class name did not match").isEqualTo("Object[][]");
  }

  @Test
  void getShortNameForPrimitiveArrayClass() {
    String className = ClassUtils.getShortName(byte[].class);
    assertThat(className).as("Class name did not match").isEqualTo("byte[]");
  }

  @Test
  void getShortNameForMultiDimensionalPrimitiveArrayClass() {
    String className = ClassUtils.getShortName(byte[][][].class);
    assertThat(className).as("Class name did not match").isEqualTo("byte[][][]");
  }

  @Test
  void getShortNameForNestedClass() {
    String className = ClassUtils.getShortName(NestedClass.class);
    assertThat(className).as("Class name did not match").isEqualTo("ClassUtilsTests.NestedClass");
  }

  @Test
  void getShortNameAsProperty() {
    String shortName = ClassUtils.getShortNameAsProperty(this.getClass());
    assertThat(shortName).as("Class name did not match").isEqualTo("classUtilsTests");
  }

  @Test
  void getClassFileName() {
    assertThat(ClassUtils.getClassFileName(String.class)).isEqualTo("String.class");
    assertThat(ClassUtils.getClassFileName(getClass())).isEqualTo("ClassUtilsTests.class");
  }

  @Test
  void getPackageName() {
    assertThat(ClassUtils.getPackageName(String.class)).isEqualTo("java.lang");
    assertThat(ClassUtils.getPackageName(getClass())).isEqualTo(getClass().getPackage().getName());
  }

  @Test
  void getQualifiedName() {
    String className = ClassUtils.getQualifiedName(getClass());
    assertThat(className).as("Class name did not match").isEqualTo("cn.taketoday.util.ClassUtilsTests");
  }

  @Test
  void getQualifiedNameForObjectArrayClass() {
    String className = ClassUtils.getQualifiedName(Object[].class);
    assertThat(className).as("Class name did not match").isEqualTo("java.lang.Object[]");
  }

  @Test
  void getQualifiedNameForMultiDimensionalObjectArrayClass() {
    String className = ClassUtils.getQualifiedName(Object[][].class);
    assertThat(className).as("Class name did not match").isEqualTo("java.lang.Object[][]");
  }

  @Test
  void getQualifiedNameForPrimitiveArrayClass() {
    String className = ClassUtils.getQualifiedName(byte[].class);
    assertThat(className).as("Class name did not match").isEqualTo("byte[]");
  }

  @Test
  void getQualifiedNameForMultiDimensionalPrimitiveArrayClass() {
    String className = ClassUtils.getQualifiedName(byte[][].class);
    assertThat(className).as("Class name did not match").isEqualTo("byte[][]");
  }

  @Test
  void isInnerClass() throws Exception {
    Class<?> innerClass = AutowirableClass.InnerAutowirableClass.class;
    assertThat(ClassUtils.isInnerClass(innerClass)).isTrue();
    Constructor<?> constructor = innerClass.getConstructor(
            AutowirableClass.class, String.class, String.class);

    assertThat(constructor).isNotNull();
  }

  public static class AutowirableClass {

    public AutowirableClass(@Autowired String firstParameter,
                            String secondParameter, String thirdParameter,
                            @Autowired(required = false) String fourthParameter) {
    }

    public AutowirableClass(String notAutowirableParameter) {
    }

    public class InnerAutowirableClass {

      public InnerAutowirableClass(@Autowired String firstParameter, String secondParameter) {
      }
    }
  }

}
