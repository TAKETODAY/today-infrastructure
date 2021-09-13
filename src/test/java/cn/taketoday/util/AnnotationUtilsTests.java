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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cn.taketoday.asm.tree.ClassNode;
import cn.taketoday.beans.Component;
import cn.taketoday.beans.DefaultComponent;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.context.Scope;
import cn.taketoday.beans.Service;
import cn.taketoday.beans.Singleton;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.core.annotation.ClassMetaReader;
import test.demo.config.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author TODAY 2021/7/28 21:41
 */
public class AnnotationUtilsTests {
  private static final Logger log = LoggerFactory.getLogger(AnnotationUtilsTests.class);

  @Component0(
          value = "annotationVisitorBean",
          scope = "singleton",
          test = TestEnum.TEST1,
          double0 = 100,
          classes = { AnnotationVisitorBean.class, AnnotationUtilsTests.class },
          service = @Service("name")
  )
  @Service
  public static class AnnotationVisitorBean {
    @Component0(
            value = "null-CONSTRUCTOR",
            test = TestEnum.TEST2,
            double0 = 122,
            classes = { AnnotationVisitorBean.class, AnnotationUtilsTests.class },
            service = @Service("name"),
            destroyMethods = { "1", "2" }
    )
    public AnnotationVisitorBean() {

    }

    @Component0(
            value = "annotationVisitorBean",
            scope = "singleton",
            test = TestEnum.TEST1,
            double0 = 100,
            classes = { AnnotationVisitorBean.class, AnnotationUtilsTests.class },
            service = @Service("name")
    )
    public AnnotationVisitorBean(int i) {

    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.PARAMETER })
  public @interface Component0 {

    int int0() default 10;

    double[] double0() default 10;

    String[] value() default {};

    String scope() default Scope.SINGLETON;

    TestEnum test() default TestEnum.TEST;

    String[] destroyMethods() default {};

    Class<?>[] classes() default {};

    Service service() default @Service;
  }

  public enum TestEnum {
    TEST,
    TEST1,
    TEST2;
  }

  @Test
  public void testMapAnnotationVisitor() throws Throwable {
    ClassNode classNode = ClassMetaReader.read(AnnotationVisitorBean.class);

//    System.out.println(classNode);

    AnnotationAttributes[] annotationAttributes = ClassMetaReader.readAnnotations(classNode);
//    Arrays.stream(annotationAttributes)
//            .forEach(System.out::println);

    AnnotationAttributes attributes = annotationAttributes[0];
//    final AnnotationAttributes attributes = attributesMap.get("Lcn/taketoday/util/AnnotationUtilsTests$Component0;");
//
    final String[] values = attributes.getStringArray("value");
    final TestEnum test = attributes.getEnum("test");

    final Class<?> clazz = attributes.getClass("classes");
//    System.out.println(clazz);
    final Class<?>[] classes = attributes.getClassArray("classes");
    final Class<?>[] classes1 = attributes.getClassArray("classes");

//    System.out.println(Arrays.toString(values));
//    System.out.println(Arrays.toString(classes));
//    System.out.println(Arrays.toString(classes1));
//    System.out.println(test);

    assertThat(values).hasSize(1).contains("annotationVisitorBean");
    assertThat(test).isEqualTo(TestEnum.TEST1);
    assertThat(clazz).isEqualTo(AnnotationVisitorBean.class);
    assertThat(classes).hasSize(2).isEqualTo(classes1);
    assertThat(classes[0]).isEqualTo(AnnotationVisitorBean.class);
    assertThat(classes[1]).isEqualTo(AnnotationUtilsTests.class);
    assertThat(attributes.getNumber("double0").doubleValue()).isEqualTo(100D);
    assertThat(attributes.getStringArray("destroyMethods")).isEmpty();
    assertThat(attributes.getString("scope")).isEqualTo("singleton");

//    attributesMap.entrySet()
//            .forEach(System.out::println);
//
//    final AnnotationAttributes service = attributes.getAttribute("service", AnnotationAttributes.class);
//    System.err.println(service);
//
//    service.entrySet()
//            .forEach(System.err::println);
  }

  @Test
  public void testReadAnnotation_onConstructors() throws Exception {
    // null
    Constructor<AnnotationVisitorBean> declaredConstructor = AnnotationVisitorBean.class.getDeclaredConstructor();
    AnnotationAttributes[] annotationAttributes = ClassMetaReader.readAnnotations(declaredConstructor);

    Arrays.stream(annotationAttributes)
            .forEach(System.out::println);

    Constructor<AnnotationVisitorBean> declaredConstructor1 = AnnotationVisitorBean.class.getDeclaredConstructor(int.class);

    // assert

    AnnotationAttributes attributes = annotationAttributes[0];

    //
    String[] values = attributes.getStringArray("value");
    TestEnum testEnum = attributes.getEnum("test");

    Class<?> clazz = attributes.getClass("classes"); // first value
    Class<?>[] classes = attributes.getClassArray("classes"); // full value
    Class<?>[] classes1 = attributes.getClassArray("classes");

    assertThat(values).hasSize(1).contains("null-CONSTRUCTOR");
    assertThat(testEnum).isEqualTo(TestEnum.TEST2);
    assertThat(clazz).isEqualTo(AnnotationVisitorBean.class);
    assertThat(classes).hasSize(2).isEqualTo(classes1);
    assertThat(classes[0]).isEqualTo(AnnotationVisitorBean.class);
    assertThat(classes[1]).isEqualTo(AnnotationUtilsTests.class);
    assertThat(attributes.getNumber("double0").doubleValue()).isEqualTo(122);
    assertThat(attributes.getStringArray("destroyMethods")).hasSize(2).contains("1", "2");
    assertThat(attributes.getString("scope")).isEqualTo("singleton");

    // read new one
    annotationAttributes = ClassMetaReader.readAnnotations(declaredConstructor1);
    attributes = annotationAttributes[0];

    values = attributes.getStringArray("value");
    testEnum = attributes.getEnum("test");

    clazz = attributes.getClass("classes"); // first value
    classes = attributes.getClassArray("classes"); // full value
    classes1 = attributes.getClassArray("classes");

    assertThat(values).hasSize(1).contains("annotationVisitorBean");
    assertThat(testEnum).isEqualTo(TestEnum.TEST1);
    assertThat(clazz).isEqualTo(AnnotationVisitorBean.class);
    assertThat(classes).hasSize(2).isEqualTo(classes1);
    assertThat(classes[0]).isEqualTo(AnnotationVisitorBean.class);
    assertThat(classes[1]).isEqualTo(AnnotationUtilsTests.class);
    assertThat(attributes.getNumber("double0").doubleValue()).isEqualTo(100);
    assertThat(attributes.getStringArray("destroyMethods")).isEmpty();
    assertThat(attributes.getString("scope")).isEqualTo("singleton");

  }

  static class OnFields {

    @Component0(
            value = "OnFields-test",
            scope = "singleton2",
            test = TestEnum.TEST1,
            double0 = 1010,
            classes = { OnFields.class, AnnotationUtilsTests.class },
            service = @Service("name2")
    )
    int test;

    @Component0(
            value = "OnFields-test1",
            scope = "singleton1",
            test = TestEnum.TEST,
            double0 = 1001,
            classes = { AnnotationVisitorBean.class, OnFields.class },
            service = @Service("name1")
    )
    double test1;

  }

  @Test
  public void testReadAnnotation_onFields() throws Exception {
    // null

    Field test = OnFields.class.getDeclaredField("test");

    Field test1 = OnFields.class.getDeclaredField("test1");

    AnnotationAttributes[] annotationAttributes = ClassMetaReader.readAnnotations(test);

    AnnotationAttributes attributes = annotationAttributes[0];

    //
    String[] values = attributes.getStringArray("value");
    TestEnum testEnum = attributes.getEnum("test");

    Class<?> clazz = attributes.getClass("classes"); // first value
    Class<?>[] classes = attributes.getClassArray("classes"); // full value
    Class<?>[] classes1 = attributes.getClassArray("classes");

    // assert

    assertThat(values).hasSize(1).contains("OnFields-test");
    assertThat(testEnum).isEqualTo(TestEnum.TEST1);
    assertThat(clazz).isEqualTo(OnFields.class);
    assertThat(classes).hasSize(2).isEqualTo(classes1);
    assertThat(classes[0]).isEqualTo(OnFields.class);
    assertThat(classes[1]).isEqualTo(AnnotationUtilsTests.class);
    assertThat(attributes.getNumber("double0").doubleValue()).isEqualTo(1010);
    assertThat(attributes.getStringArray("destroyMethods")).isEmpty();
    assertThat(attributes.getString("scope")).isEqualTo("singleton2");

    // read new one
    annotationAttributes = ClassMetaReader.readAnnotations(test1);
    attributes = annotationAttributes[0];

    values = attributes.getStringArray("value");
    testEnum = attributes.getEnum("test");

    clazz = attributes.getClass("classes"); // first value
    classes = attributes.getClassArray("classes"); // full value
    classes1 = attributes.getClassArray("classes");

    assertThat(values).hasSize(1).contains("OnFields-test1");
    assertThat(testEnum).isEqualTo(TestEnum.TEST);
    assertThat(clazz).isEqualTo(AnnotationVisitorBean.class);
    assertThat(classes).hasSize(2).isEqualTo(classes1);
    assertThat(classes[0]).isEqualTo(AnnotationVisitorBean.class);
    assertThat(classes[1]).isEqualTo(OnFields.class);
    assertThat(attributes.getNumber("double0").doubleValue()).isEqualTo(1001);
    assertThat(attributes.getStringArray("destroyMethods")).isEmpty();
    assertThat(attributes.getString("scope")).isEqualTo("singleton1");

  }

  static class OnParameters {

    void test(
            @Component0(
                    value = "OnParameters-test1",
                    scope = "singleton1",
                    test = TestEnum.TEST,
                    double0 = 1001,
                    classes = { AnnotationVisitorBean.class, OnFields.class },
                    service = @Service("name1")
            ) int test) {

    }
  }

  @Test
  public void testReadAnnotation_onParameters() throws Exception {
    Method test = OnParameters.class.getDeclaredMethod("test", int.class);
    Parameter[] parameters = test.getParameters();
    Parameter parameter = parameters[0];

    AnnotationAttributes[] annotationAttributes = ClassMetaReader.readAnnotations(parameter);

    AnnotationAttributes attributes = annotationAttributes[0];

    //
    String[] values = attributes.getStringArray("value");
    TestEnum testEnum = attributes.getEnum("test");

    Class<?> clazz = attributes.getClass("classes"); // first value
    Class<?>[] classes = attributes.getClassArray("classes"); // full value
    Class<?>[] classes1 = attributes.getClassArray("classes");

    assertThat(values).hasSize(1).contains("OnParameters-test1");
    assertThat(testEnum).isEqualTo(TestEnum.TEST);
    assertThat(clazz).isEqualTo(AnnotationVisitorBean.class);
    assertThat(classes).hasSize(2).isEqualTo(classes1);
    assertThat(classes[0]).isEqualTo(AnnotationVisitorBean.class);
    assertThat(classes[1]).isEqualTo(OnFields.class);
    assertThat(attributes.getNumber("double0").doubleValue()).isEqualTo(1001);
    assertThat(attributes.getStringArray("destroyMethods")).isEmpty();
    assertThat(attributes.getString("scope")).isEqualTo("singleton1");
  }

  static class OnMethod {

    @Component0(
            value = "OnMethod-test",
            scope = "OnMethod",
            test = TestEnum.TEST,
            double0 = 1001,
            classes = { OnMethod.class, OnFields.class },
            service = @Service("name1")
    )
    void test() {

    }

  }

  @Test
  public void testIsAnnotationPresent() {

    assert AnnotationUtils.isPresent(ClassUtilsTest.AutowiredOnConstructor.class, Singleton.class);
    assert AnnotationUtils.isPresent(ClassUtilsTest.AutowiredOnConstructor.class, ClassUtilsTest.MySingleton.class);

    assert ClassUtils.loadClass("") == null;
  }


  // old code

  @Test
  @SuppressWarnings("unlikely-arg-type")
  public void test_GetAnnotation() throws Exception {

    // test: use reflect build the annotation
    Collection<Component> classAnntation = AnnotationUtils.getAnnotation(Config.class, Component.class, DefaultComponent.class);

    for (Component component : classAnntation) {
      log.info("component: [{}]", component);
      if (component.value().length != 0 && "prototype_config".equals(component.value()[0])) {
        assert component.scope().equals(Scope.PROTOTYPE);
      }
      else {
        assert component.scope().equals(Scope.SINGLETON);
      }
    }
    // use proxy
    Collection<Bean.C> annotations = AnnotationUtils.getAnnotation(Bean.class, Bean.C.class);

    List<AnnotationAttributes> attributes = AnnotationUtils.getAttributes(Bean.class.getAnnotation(Bean.S.class), Bean.C.class);
    final AnnotationAttributes next = attributes.iterator().next();
    Bean.C annotation = AnnotationUtils.getAnnotationProxy(Bean.C.class, next);
    System.err.println(annotation);
    annotation.hashCode();
    assert annotation.annotationType() == Bean.C.class;
    assert annotation.scope().equals(Scope.SINGLETON);
    assert annotations.size() == 2;

    assert !annotation.equals(null);
    assert annotation.equals(annotation);

    assert annotation.equals(AnnotationUtils.getAnnotationProxy(Bean.C.class, next));

    final AnnotationAttributes clone = new AnnotationAttributes(next);

    assert !clone.equals(annotation);
    assert !clone.equals(null);
    assert clone.equals(clone);
    assert clone.equals(next);

    clone.remove("value");
    assert !clone.equals(next);
    assert !clone.equals(new AnnotationAttributes((Map<String, Object>) next));
    assert !annotation.equals(AnnotationUtils.getAnnotationProxy(Bean.C.class, clone));

    final AnnotationAttributes fromMap = new AnnotationAttributes(clone);
    assert fromMap.equals(clone);
    assert !fromMap.equals(new AnnotationAttributes());
    assert !fromMap.equals(new AnnotationAttributes(1));
    assert !fromMap.equals(new AnnotationAttributes(Bean.C.class));

    try {

      AnnotationUtils.getAttributes(null);

      assert false;
    }
    catch (Exception e) {
      assert true;
    }
    try {
      AnnotationUtils.injectAttributes(next, null, next);
      assert false;
    }
    catch (Exception e) {
      assert true;
    }

  }

  @Test
  public void test_GetAnnotationAttributes() throws Exception {

    Bean.S annotation = Bean.class.getAnnotation(Bean.S.class);
    AnnotationAttributes annotationAttributes_ = AnnotationUtils.getAttributes(annotation);

    log.info("annotationAttributes: [{}]", annotationAttributes_);
    assertEquals(annotationAttributes_.getStringArray("value").length, 1);
    log.info("annotationType: [{}]", annotationAttributes_.annotationType());
    assertEquals(annotationAttributes_.annotationType(), Bean.S.class);

    List<AnnotationAttributes> annotationAttributes = AnnotationUtils.getAttributes(Bean.class, Bean.C.class);
    log.info("annotationAttributes: [{}]", annotationAttributes);
    for (AnnotationAttributes attributes : annotationAttributes) {
      log.info("annotationType: [{}]", attributes.annotationType());
      assertEquals(attributes.annotationType(), Bean.C.class);
      if ("s".equals(attributes.getStringArray("value")[0])) {
        assertEquals(attributes.getString("scope"), Scope.SINGLETON);
      }
      if ("p".equals(attributes.getStringArray("value")[0])) {
        assertEquals(attributes.getString("scope"), Scope.PROTOTYPE);
      }
    }

    final AnnotationAttributes attr = AnnotationUtils.getAttributes(Bean.C.class, Bean.class);

    assert attr != null;
    assertEquals(attr.getString("scope"), Scope.SINGLETON);
  }

  @Test
  public void test_GetAnnotations() throws Exception {

    // test: use reflect build the annotation
    Collection<Component> components = AnnotationUtils.getAnnotation(Config.class, Component.class, DefaultComponent.class);

    for (Component component : components) {
      System.err.println(component);
    }
  }

  @Test
  public void test_GetAnnotationArray() throws Exception {

    // test: use reflect build the annotation
    Component[] components = AnnotationUtils.getAnnotationArray(Config.class, Component.class, DefaultComponent.class);

    final Component[] annotationArray = AnnotationUtils.getAnnotationArray(Config.class, Component.class);
    assert components.length > 0;
    assert annotationArray.length > 0;
    assert annotationArray.length == annotationArray.length;

  }

  @Bean.S("s")
  @Bean.P("p")
  static class Bean {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @C(scope = Scope.PROTOTYPE)
    public @interface A {
      String[] value() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @C(scope = Scope.SINGLETON)
    public @interface S {
      @AliasFor(type = C.class)
      String[] value() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface C {
      String[] value() default {};

      String scope() default Scope.SINGLETON;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @A
    public @interface D {
      String[] value() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @D
    public @interface P {
      String[] value() default {};
    }
  }

}
