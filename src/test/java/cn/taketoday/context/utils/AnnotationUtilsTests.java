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

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;

import cn.taketoday.asm.tree.ClassNode;
import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Service;
import cn.taketoday.asm.AnnotationVisitor;
import cn.taketoday.asm.Attribute;
import cn.taketoday.asm.ClassReader;
import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.TypePath;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.support.ClassMetaReader;
import cn.taketoday.context.utils.AnnotationUtils.MapAnnotationVisitor;

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
  public static class AnnotationVisitorBean {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  public @interface Component0 {

    int int0() default 10;

    double double0() default 10;

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

    System.out.println(classNode);

//    HashMap<String, AnnotationAttributes> attributesMap = new HashMap<>();
//
//    attributesMap.entrySet()
//            .forEach(System.out::println);
//
//    final AnnotationAttributes attributes = attributesMap.get("Lcn/taketoday/context/utils/AnnotationUtilsTests$Component0;");
//
//    final String[] values = attributes.getStringArray("value");
//    final TestEnum test = attributes.getEnum("test");
//
//    final Class<?> clazz = attributes.getClass("classes");
//    System.out.println(clazz);
//    final Class<?>[] classes = attributes.getClassArray("classes");
//    final Class<?>[] classes1 = attributes.getClassArray("classes");
//
//    System.out.println(Arrays.toString(values));
//    System.out.println(Arrays.toString(classes));
//    System.out.println(Arrays.toString(classes1));
//    System.out.println(test);
//
//    attributesMap.entrySet()
//            .forEach(System.out::println);
//
//    final AnnotationAttributes service = attributes.getAttribute("service", AnnotationAttributes.class);
//    System.err.println(service);
//
//    service.entrySet()
//            .forEach(System.err::println);
  }

}
