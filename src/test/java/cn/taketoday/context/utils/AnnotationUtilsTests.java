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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Service;
import cn.taketoday.context.asm.AnnotationVisitor;
import cn.taketoday.context.asm.Attribute;
import cn.taketoday.context.asm.ClassReader;
import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.TypePath;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
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
  public void testMapAnnotationVisitor() throws IOException {

    ClassLoader classLoader = ClassUtils.getClassLoader();
    String classFile = AnnotationVisitorBean.class.getName()
            .replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR)
            .concat(Constant.CLASS_FILE_SUFFIX);

    try (InputStream resourceAsStream = classLoader.getResourceAsStream(classFile)) {

      HashMap<String, AnnotationAttributes> attributesMap = new HashMap<>();

      final ClassReader classReader = new ClassReader(resourceAsStream);

      classReader.accept(new ClassVisitor() {

        @Override
        public void visitAttribute(Attribute attribute) {
          super.visitAttribute(attribute);
          log.info("visitAttribute: attribute: {}", attribute);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
          log.info("visitAnnotation: descriptor: {}", descriptor);
          final MapAnnotationVisitor visitor = new MapAnnotationVisitor();
          visitor.setDescriptor(descriptor);
          attributesMap.put(descriptor, visitor.getAttributes());
          return visitor;
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
          log.info("visitTypeAnnotation: descriptor: {}, typePath: {}, visible: {}", descriptor, typePath, visible);
          return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }
      }, ClassReader.SKIP_CODE);

      attributesMap.entrySet()
              .forEach(System.out::println);

      final AnnotationAttributes attributes = attributesMap.get("Lcn/taketoday/context/utils/AnnotationUtilsTests$Component0;");

      final String[] values = attributes.getStringArray("value");
      final TestEnum test = attributes.getEnum("test");

      final Class<?> clazz = attributes.getClass("classes");
      System.out.println(clazz);
      final Class<?>[] classes = attributes.getClassArray("classes");
      final Class<?>[] classes1 = attributes.getClassArray("classes");

      System.out.println(Arrays.toString(values));
      System.out.println(Arrays.toString(classes));
      System.out.println(Arrays.toString(classes1));
      System.out.println(test);

      attributesMap.entrySet()
              .forEach(System.out::println);

    }
  }

}
