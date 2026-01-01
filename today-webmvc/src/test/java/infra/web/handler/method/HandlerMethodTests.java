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

package infra.web.handler.method;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import infra.core.MethodParameter;
import infra.util.ReflectionUtils;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/8/23 14:56
 */
class HandlerMethodTests {

  @Test
  void shouldFindAnnotationOnMethodInGenericAbstractSuperclass() {
    Method processTwo = getMethod("processTwo", String.class);

    HandlerMethod annotatedMethod = new HandlerMethod(new GenericInterfaceImpl(), processTwo);

    assertThat(annotatedMethod.hasMethodAnnotation(Handler.class)).isTrue();
  }

  @Test
  void shouldFindAnnotationOnMethodInGenericInterface() {
    Method processOneAndTwo = getMethod("processOneAndTwo", Long.class, Object.class);

    HandlerMethod annotatedMethod = new HandlerMethod(new GenericInterfaceImpl(), processOneAndTwo);

    assertThat(annotatedMethod.hasMethodAnnotation(Handler.class)).isTrue();
  }

  @Test
  void shouldFindAnnotationOnMethodParameterInGenericAbstractSuperclass() {
    Method abstractMethod = ReflectionUtils.findMethod(GenericAbstractSuperclass.class, "processTwo", Object.class);
    assertThat(abstractMethod).isNotNull();
    assertThat(Modifier.isAbstract(abstractMethod.getModifiers())).as("abstract").isTrue();
    assertThat(Modifier.isPublic(abstractMethod.getModifiers())).as("public").isFalse();

    Method processTwo = getMethod("processTwo", String.class);

    HandlerMethod annotatedMethod = new HandlerMethod(new GenericInterfaceImpl(), processTwo);
    MethodParameter[] methodParameters = annotatedMethod.getMethodParameters();

    assertThat(methodParameters).hasSize(1);
    assertThat(methodParameters[0].hasParameterAnnotation(Param.class)).isTrue();
  }

  @Test
  void shouldFindAnnotationOnMethodParameterInGenericInterface() {
    Method processOneAndTwo = getMethod("processOneAndTwo", Long.class, Object.class);

    HandlerMethod annotatedMethod = new HandlerMethod(new Object(), processOneAndTwo);
    MethodParameter[] methodParameters = annotatedMethod.getMethodParameters();

    assertThat(methodParameters).hasSize(2);
    assertThat(methodParameters[0].hasParameterAnnotation(Param.class)).isFalse();
    assertThat(methodParameters[1].hasParameterAnnotation(Param.class)).isTrue();
  }

  private static Method getMethod(String name, Class<?>... parameterTypes) {
    Class<?> clazz = GenericInterfaceImpl.class;
    Method method = ReflectionUtils.findMethod(clazz, name, parameterTypes);
    if (method == null) {
      String parameterNames = stream(parameterTypes).map(Class::getName).collect(joining(", "));
      throw new IllegalStateException("Expected method not found: %s#%s(%s)"
              .formatted(clazz.getSimpleName(), name, parameterNames));
    }
    return method;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Handler {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Param {
  }

  interface GenericInterface<A, B> {

    @Handler
    void processOneAndTwo(A value1, @Param B value2);
  }

  abstract static class GenericAbstractSuperclass<C> implements GenericInterface<Long, C> {

    @Override
    public void processOneAndTwo(Long value1, C value2) {
    }

    @Handler
    // Intentionally NOT public
    abstract void processTwo(@Param C value);
  }

  static class GenericInterfaceImpl extends GenericAbstractSuperclass<String> {

    @Override
    void processTwo(String value) {
    }
  }

}