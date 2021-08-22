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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
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
import cn.taketoday.cglib.proxy.Enhancer;
import cn.taketoday.cglib.proxy.MethodInterceptor;
import cn.taketoday.cglib.proxy.MethodProxy;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import lombok.ToString;

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

  private long start;

  private String process;

  public String getProcess() {
    return process;
  }

  public void setProcess(String process) {
    this.process = process;
  }

  @Before
  public void start() {
    start = System.currentTimeMillis();
  }

  @After
  public void end() {
    log.info("process:[{}] takes [{}]ms", getProcess(), (System.currentTimeMillis() - start));
  }

  public void test(String name, Integer i) {

  }

  @Test
  public void test_GetMethodArgsNames() throws NoSuchMethodException, SecurityException, IOException {
    setProcess("test_GetMethodArgsNames");
    String[] methodArgsNames = ClassUtils.getMethodArgsNames(ClassUtilsTest.class.getMethod("test", String.class, Integer.class));

    assert methodArgsNames.length > 0 : "Can't get Method Args Names";

    assert "name".equals(methodArgsNames[0]) : "Can't get Method Args Names";
    assert "i".equals(methodArgsNames[1]) : "Can't get Method Args Names";

    log.info("names: {}", Arrays.toString(methodArgsNames));
  }

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
    setProcess("resolvePrimitiveClassName");

    assert ClassUtils.resolvePrimitiveClassName("java.lang.Float") == null;
    assert ClassUtils.resolvePrimitiveClassName("float") == float.class;
    assert ClassUtils.resolvePrimitiveClassName(null) == null;
  }

  @Test
  public void forName() throws ClassNotFoundException {
    setProcess("forName");

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

  }

  private static class INNER {

  }

  @Test
  public void isPresent() {
    setProcess("isPresent");

    assert ClassUtils.isPresent("java.lang.Float");
    assert !ClassUtils.isPresent("Float");
  }

  @Test
  public void testAutowiredOnConstructor() {

    setProcess("AutowiredOnConstructor");

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
    setProcess("invokeMethod");

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
    setProcess("testGetGenerics");

    assertThat(ClassUtils.getGenerics(Generic.class, Genericity.class))
            .isNotNull()
            .hasSize(1)
            .contains(Integer.class);

    // param
    Constructor<Generic> constructor = BeanUtils.getSuitableConstructor(Generic.class);
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
    setProcess("testGetGenericsInterface");
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

  static class TestNewInstanceBean { }

  @ToString
  static class TestNewInstanceBeanProvidedArgs {
    Integer integer;

    TestNewInstanceBeanProvidedArgs(Integer integer) {
      this.integer = integer;
    }
  }

  //
  @Test
  public void testNewInstance() {
    final TestNewInstanceBean testNewInstanceBean = BeanUtils.newInstance(TestNewInstanceBean.class);

    System.out.println(testNewInstanceBean);

    try (StandardApplicationContext context = new StandardApplicationContext()) {

      final TestNewInstanceBeanProvidedArgs providedArgs = BeanUtils
              .newInstance(TestNewInstanceBeanProvidedArgs.class, context, new Object[] { 1, "TODAY" });

      System.out.println(providedArgs);
    }
  }

}
