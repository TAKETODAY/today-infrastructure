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

package cn.taketoday.core.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import cn.taketoday.core.OverridingClassLoader;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests that trigger annotation introspection failures and ensure that they are
 * dealt with correctly.
 *
 * @author Phillip Webb
 * @see AnnotationUtils
 * @see AnnotatedElementUtils
 * @since 4.0
 */
class AnnotationIntrospectionFailureTests {

  @Test
  void filteredTypeThrowsTypeNotPresentException() throws Exception {
    FilteringClassLoader classLoader = new FilteringClassLoader(
            getClass().getClassLoader());
    Class<?> withExampleAnnotation = ClassUtils.forName(
            WithExampleAnnotation.class.getName(), classLoader);
    Annotation annotation = withExampleAnnotation.getAnnotations()[0];
    Method method = annotation.annotationType().getMethod("value");
    method.setAccessible(true);
    assertThatExceptionOfType(TypeNotPresentException.class).isThrownBy(() ->
                    ReflectionUtils.invokeMethod(method, annotation))
            .withCauseInstanceOf(ClassNotFoundException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void filteredTypeInMetaAnnotationWhenUsingAnnotatedElementUtilsHandlesException() throws Exception {
    FilteringClassLoader classLoader = new FilteringClassLoader(
            getClass().getClassLoader());
    Class<?> withExampleMetaAnnotation = ClassUtils.forName(
            WithExampleMetaAnnotation.class.getName(), classLoader);
    Class<Annotation> exampleAnnotationClass = ClassUtils.forName(
            ExampleAnnotation.class.getName(), classLoader);
    Class<Annotation> exampleMetaAnnotationClass = ClassUtils.forName(
            ExampleMetaAnnotation.class.getName(), classLoader);
    assertThat(AnnotatedElementUtils.getMergedAnnotationAttributes(
            withExampleMetaAnnotation, exampleAnnotationClass)).isNull();
    assertThat(AnnotatedElementUtils.getMergedAnnotationAttributes(
            withExampleMetaAnnotation, exampleMetaAnnotationClass)).isNull();
    assertThat(AnnotatedElementUtils.hasAnnotation(withExampleMetaAnnotation,
            exampleAnnotationClass)).isFalse();
    assertThat(AnnotatedElementUtils.hasAnnotation(withExampleMetaAnnotation,
            exampleMetaAnnotationClass)).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  void filteredTypeInMetaAnnotationWhenUsingMergedAnnotationsHandlesException() throws Exception {
    FilteringClassLoader classLoader = new FilteringClassLoader(
            getClass().getClassLoader());
    Class<?> withExampleMetaAnnotation = ClassUtils.forName(
            WithExampleMetaAnnotation.class.getName(), classLoader);
    Class<Annotation> exampleAnnotationClass = ClassUtils.forName(
            ExampleAnnotation.class.getName(), classLoader);
    Class<Annotation> exampleMetaAnnotationClass = ClassUtils.forName(
            ExampleMetaAnnotation.class.getName(), classLoader);
    MergedAnnotations annotations = MergedAnnotations.from(withExampleMetaAnnotation);
    assertThat(annotations.get(exampleAnnotationClass).isPresent()).isFalse();
    assertThat(annotations.get(exampleMetaAnnotationClass).isPresent()).isFalse();
    assertThat(annotations.isPresent(exampleMetaAnnotationClass)).isFalse();
    assertThat(annotations.isPresent(exampleAnnotationClass)).isFalse();
  }

  static class FilteringClassLoader extends OverridingClassLoader {

    FilteringClassLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    protected boolean isEligibleForOverriding(String className) {
      return className.startsWith(
              AnnotationIntrospectionFailureTests.class.getName());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (name.startsWith(AnnotationIntrospectionFailureTests.class.getName()) &&
              name.contains("Filtered")) {
        throw new ClassNotFoundException(name);
      }
      return super.loadClass(name, resolve);
    }
  }

  static class FilteredType {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ExampleAnnotation {

    Class<?> value() default Void.class;
  }

  @ExampleAnnotation(FilteredType.class)
  static class WithExampleAnnotation {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @ExampleAnnotation
  @interface ExampleMetaAnnotation {

    @AliasFor(annotation = ExampleAnnotation.class, attribute = "value")
    Class<?> example1() default Void.class;

    @AliasFor(annotation = ExampleAnnotation.class, attribute = "value")
    Class<?> example2() default Void.class;

  }

  @ExampleMetaAnnotation(example1 = FilteredType.class)
  static class WithExampleMetaAnnotation {
  }

}
