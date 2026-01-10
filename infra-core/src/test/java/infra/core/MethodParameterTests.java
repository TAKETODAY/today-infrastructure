/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/19 22:23
 */
class MethodParameterTests {

  @Test
  void forExecutableWithMethod() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = MethodParameter.forExecutable(method, 0);

    assertThat(methodParameter.getMethod()).isEqualTo(method);
    assertThat(methodParameter.getExecutable()).isEqualTo(method);
    assertThat(methodParameter.getParameterIndex()).isEqualTo(0);
  }

  @Test
  void forExecutableWithConstructor() throws Exception {
    Constructor<TestClass> constructor = TestClass.class.getConstructor(String.class);
    MethodParameter methodParameter = MethodParameter.forExecutable(constructor, 0);

    assertThat(methodParameter.getConstructor()).isEqualTo(constructor);
    assertThat(methodParameter.getExecutable()).isEqualTo(constructor);
    assertThat(methodParameter.getParameterIndex()).isEqualTo(0);
  }

  @Test
  void forParameter() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    Parameter parameter = method.getParameters()[0];
    MethodParameter methodParameter = MethodParameter.forParameter(parameter);

    assertThat(methodParameter.getParameter()).isEqualTo(parameter);
    assertThat(methodParameter.getParameterIndex()).isEqualTo(0);
  }

  @Test
  void getDeclaringClass() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.getDeclaringClass()).isEqualTo(TestClass.class);
  }

  @Test
  void getMember() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.getMember()).isEqualTo(method);
  }

  @Test
  void getAnnotatedElement() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.getAnnotatedElement()).isEqualTo(method);
  }

  @Test
  void isOptionalWithOptionalParameter() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("optionalMethod", Optional.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.isOptional()).isTrue();
  }

  @Test
  void isOptionalWithNonOptionalParameter() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.isOptional()).isFalse();
  }

  @Test
  void nestedIfOptionalWithOptional() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("optionalMethod", Optional.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);
    MethodParameter nested = methodParameter.nestedIfOptional();

    assertThat(nested.getNestingLevel()).isEqualTo(2);
  }

  @Test
  void nestedIfOptionalWithNonOptional() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);
    MethodParameter nested = methodParameter.nestedIfOptional();

    assertThat(nested).isSameAs(methodParameter);
  }

  @Test
  void withContainingClass() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);
    MethodParameter withContaining = methodParameter.withContainingClass(String.class);

    assertThat(withContaining.getContainingClass()).isEqualTo(String.class);
    assertThat(withContaining).isNotSameAs(methodParameter);
  }

  @Test
  void getParameterType() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.getParameterType()).isEqualTo(String.class);
  }

  @Test
  void getGenericParameterType() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("genericMethod", List.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.getGenericParameterType()).isInstanceOf(ParameterizedType.class);
  }

  @Test
  void equalsAndHashCode() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter param1 = new MethodParameter(method, 0);
    MethodParameter param2 = new MethodParameter(method, 0);
    MethodParameter param3 = new MethodParameter(method, 1);

    assertThat(param1).isEqualTo(param2);
    assertThat(param1.hashCode()).isEqualTo(param2.hashCode());
    assertThat(param1).isNotEqualTo(param3);
  }

  @Test
  void cloneMethod() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter original = new MethodParameter(method, 0);
    MethodParameter cloned = original.clone();

    assertThat(cloned).isEqualTo(original);
    assertThat(cloned).isNotSameAs(original);
  }

  @Test
  void toStringMethod() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.toString()).contains("method 'testMethod' parameter 0");
  }

  @Test
  void constructorValidation() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new MethodParameter((Method) null, 0))
            .withMessageContaining("Method is required");

    assertThatIllegalArgumentException()
            .isThrownBy(() -> new MethodParameter((Constructor<?>) null, 0))
            .withMessageContaining("Constructor is required");
  }

  @Test
  void forFieldAwareConstructor() throws Exception {
    Constructor<FieldAwareTestClass> constructor = FieldAwareTestClass.class.getConstructor(String.class);
    MethodParameter methodParameter = MethodParameter.forFieldAwareConstructor(constructor, 0, "name");

    assertThat(methodParameter.getParameterIndex()).isEqualTo(0);
  }

  @Test
  void getParameterWithReturnType() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, -1);

    assertThatIllegalStateException()
            .isThrownBy(methodParameter::getParameter)
            .withMessage("Cannot retrieve Parameter descriptor for method return type");
  }

  @Test
  void getNestedParameterTypeWithSimpleType() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.getNestedParameterType()).isEqualTo(String.class);
  }

  @Test
  void getNestedGenericParameterType() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("genericMethod", List.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.getNestedGenericParameterType()).isInstanceOf(ParameterizedType.class);
  }

  @Test
  void hasParameterAnnotationsWithAnnotations() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("annotatedMethod", String.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.hasParameterAnnotations()).isTrue();
  }

  @Test
  void hasParameterAnnotationsWithoutAnnotations() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.hasParameterAnnotations()).isFalse();
  }

  @Test
  void getParameterAnnotationFound() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("annotatedMethod", String.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    TestAnnotation annotation = methodParameter.getParameterAnnotation(TestAnnotation.class);
    assertThat(annotation).isNotNull();
  }

  @Test
  void getParameterAnnotationNotFound() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    TestAnnotation annotation = methodParameter.getParameterAnnotation(TestAnnotation.class);
    assertThat(annotation).isNull();
  }

  @Test
  void hasParameterAnnotationTrue() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("annotatedMethod", String.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.hasParameterAnnotation(TestAnnotation.class)).isTrue();
  }

  @Test
  void hasParameterAnnotationFalse() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.hasParameterAnnotation(TestAnnotation.class)).isFalse();
  }

  @Test
  void getMethodAnnotations() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("annotatedMethod", String.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.getMethodAnnotations()).hasSize(0); // Method annotations, not parameter
  }

  @Test
  void getMethodAnnotation() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("methodWithAnnotation");
    MethodParameter methodParameter = new MethodParameter(method, -1);

    TestAnnotation annotation = methodParameter.getMethodAnnotation(TestAnnotation.class);
    assertThat(annotation).isNotNull();
  }

  @Test
  void hasMethodAnnotation() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("methodWithAnnotation");
    MethodParameter methodParameter = new MethodParameter(method, -1);

    assertThat(methodParameter.hasMethodAnnotation(TestAnnotation.class)).isTrue();
  }

  @Test
  void getParameterNameWithoutDiscovery() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.getParameterName()).isNull();
  }

  @Test
  void withTypeIndex() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("genericMethod", List.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);
    MethodParameter withTypeIndex = methodParameter.withTypeIndex(0);

    assertThat(withTypeIndex.getNestingLevel()).isEqualTo(1);
    assertThat(withTypeIndex.getTypeIndexForCurrentLevel()).isEqualTo(0);
  }

  @Test
  void nested() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("genericMethod", List.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);
    MethodParameter nested = methodParameter.nested();

    assertThat(nested.getNestingLevel()).isEqualTo(2);
  }

  @Test
  void getTypeIndexForLevel() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("genericMethod", List.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);
    MethodParameter withTypeIndex = methodParameter.withTypeIndex(1);

    assertThat(withTypeIndex.getTypeIndexForLevel(1)).isEqualTo(1);
    assertThat(withTypeIndex.getTypeIndexForLevel(2)).isNull();
  }

  @Test
  void getAnnotationsForMethodParameter() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("annotatedMethod", String.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.getAnnotations()).isEqualTo(methodParameter.getParameterAnnotations());
  }

  @Test
  void getAnnotationsForMethodReturn() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("methodWithAnnotation");
    MethodParameter methodParameter = new MethodParameter(method, -1);

    assertThat(methodParameter.getAnnotations()).isEqualTo(methodParameter.getMethodAnnotations());
  }

  @Test
  void isAnnotationPresentForParameter() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("annotatedMethod", String.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.isAnnotationPresent(TestAnnotation.class)).isTrue();
    assertThat(methodParameter.isAnnotationPresent(Deprecated.class)).isFalse();
  }

  @Test
  void getAnnotationForParameter() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("annotatedMethod", String.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    TestAnnotation annotation = methodParameter.getAnnotation(TestAnnotation.class);
    assertThat(annotation).isNotNull();

    Deprecated deprecated = methodParameter.getAnnotation(Deprecated.class);
    assertThat(deprecated).isNull();
  }

  @Test
  void getDeclaredAnnotations() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("annotatedMethod", String.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(methodParameter.getDeclaredAnnotations()).isEqualTo(methodParameter.getAnnotations());
  }

  @Test
  void emptyArrayConstant() {
    assertThat(MethodParameter.EMPTY_ARRAY).isNotNull();
    assertThat(MethodParameter.EMPTY_ARRAY).isEmpty();
  }

  @Test
  void constructorWithNestingLevel() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0, 3);

    assertThat(methodParameter.getNestingLevel()).isEqualTo(3);
  }

  @Test
  void constructorWithNestingLevelForConstructor() throws Exception {
    Constructor<TestClass> constructor = TestClass.class.getConstructor(String.class);
    MethodParameter methodParameter = new MethodParameter(constructor, 0, 2);

    assertThat(methodParameter.getNestingLevel()).isEqualTo(2);
  }

  @Test
  void internalConstructor() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, int.class);
    MethodParameter methodParameter = new MethodParameter(method, 0, String.class);

    assertThat(methodParameter.getContainingClass()).isEqualTo(String.class);
  }

  @Test
  void forExecutableWithInvalidType() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> MethodParameter.forExecutable(null, 0))
            .withMessageContaining("Not a Method/Constructor: null");
  }

  @java.lang.annotation.Target({ java.lang.annotation.ElementType.PARAMETER, java.lang.annotation.ElementType.METHOD })
  @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
  @interface TestAnnotation {
  }

  static class TestClass {

    public TestClass(String name) { }

    public void testMethod(String param1, int param2) { }

    public void optionalMethod(Optional<String> optional) { }

    public void genericMethod(List<String> list) { }

    public void annotatedMethod(@TestAnnotation String param) { }

    @TestAnnotation
    public void methodWithAnnotation() { }
  }

  static class FieldAwareTestClass {
    private String name;

    public FieldAwareTestClass(String name) {
      this.name = name;
    }
  }

}