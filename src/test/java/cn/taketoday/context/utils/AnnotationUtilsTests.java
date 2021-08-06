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

package cn.taketoday.context.utils;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

import cn.taketoday.asm.tree.ClassNode;
import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Service;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.support.ClassMetaReader;

import static org.assertj.core.api.Assertions.assertThat;

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
//    final AnnotationAttributes attributes = attributesMap.get("Lcn/taketoday/context/utils/AnnotationUtilsTests$Component0;");
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
    AnnotationAttributes[] annotationAttributes = ClassMetaReader.readAnnotation(declaredConstructor);

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
    annotationAttributes = ClassMetaReader.readAnnotation(declaredConstructor1);
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

    AnnotationAttributes[] annotationAttributes = ClassMetaReader.readAnnotation(test);

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
    annotationAttributes = ClassMetaReader.readAnnotation(test1);
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

}
