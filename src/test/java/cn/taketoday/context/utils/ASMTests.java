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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Service;
import cn.taketoday.context.utils.ASMTests.ASMTestsBean;

/**
 * @author TODAY 2021/7/29 23:33
 */
public class ASMTests {

  @Component1(
          value = "annotationVisitorBean",
          scope = "singleton",
          test = AnnotationUtilsTests.TestEnum.TEST1,
          double0 = 100,
          classes = { AnnotationUtilsTests.AnnotationVisitorBean.class, AnnotationUtilsTests.class },
          service = @Service("name")
  )
  public static class ASMTestsBean {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  public @interface Component1 {

    int int0() default 10;

    double double0() default 10;

    String[] value() default {};

    String scope() default Scope.SINGLETON;

    AnnotationUtilsTests.TestEnum test() default AnnotationUtilsTests.TestEnum.TEST;

    String[] destroyMethods() default {};

    Class<?>[] classes() default {};

    Service service() default @Service ;//("default-name");
  }

  public enum TestEnum {
    TEST,
    TEST1,
    TEST2;
  }

  void demo(){
    System.out.println("demo");
  }

  @Test
  public void testClassNode() throws IOException {

    ClassLoader classLoader = ClassUtils.getClassLoader();
    String classFile = ASMTests.class.getName()
            .replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR)
            .concat(Constant.CLASS_FILE_SUFFIX);

    try (InputStream resourceAsStream = classLoader.getResourceAsStream(classFile)) {

      HashMap<String, AnnotationAttributes> attributesMap = new HashMap<>();

      final ClassReader classReader = new ClassReader(resourceAsStream);

      final ClassNode classVisitor = new ClassNode();
      classReader.accept(classVisitor, 0);

      System.out.println(classVisitor);
      System.out.println(classVisitor);

    }
    classFile = Component1.class.getName()
            .replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR)
            .concat(Constant.CLASS_FILE_SUFFIX);

    try (InputStream resourceAsStream = classLoader.getResourceAsStream(classFile)) {

      final ClassReader classReader = new ClassReader(resourceAsStream);

      final ClassNode classVisitor = new ClassNode();
      classReader.accept(classVisitor, 1);

      System.out.println(classVisitor);
      System.out.println(classVisitor);

    }
  }

}
