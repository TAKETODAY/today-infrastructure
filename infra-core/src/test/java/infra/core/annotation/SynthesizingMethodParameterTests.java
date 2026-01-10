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

package infra.core.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/19 22:25
 */
class SynthesizingMethodParameterTests {

  @Test
  void forExecutableWithMethod() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class);
    SynthesizingMethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(method, 0);

    assertThat(methodParameter.getMethod()).isEqualTo(method);
    assertThat(methodParameter.getExecutable()).isEqualTo(method);
    assertThat(methodParameter.getParameterIndex()).isEqualTo(0);
  }

  @Test
  void forExecutableWithConstructor() throws Exception {
    Constructor<TestClass> constructor = TestClass.class.getConstructor(String.class);
    SynthesizingMethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(constructor, 0);

    assertThat(methodParameter.getConstructor()).isEqualTo(constructor);
    assertThat(methodParameter.getExecutable()).isEqualTo(constructor);
    assertThat(methodParameter.getParameterIndex()).isEqualTo(0);
  }

  @Test
  void forParameter() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class);
    Parameter parameter = method.getParameters()[0];
    SynthesizingMethodParameter methodParameter = SynthesizingMethodParameter.forParameter(parameter);

    assertThat(methodParameter.getParameter()).isEqualTo(parameter);
    assertThat(methodParameter.getParameterIndex()).isEqualTo(0);
  }

  @Test
  void cloneMethod() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class);
    SynthesizingMethodParameter original = new SynthesizingMethodParameter(method, 0);
    SynthesizingMethodParameter cloned = original.clone();

    assertThat(cloned).isEqualTo(original);
    assertThat(cloned).isNotSameAs(original);
    assertThat(cloned).isInstanceOf(SynthesizingMethodParameter.class);
  }

  @Test
  void constructorWithMethodAndNestingLevel() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class);
    SynthesizingMethodParameter methodParameter = new SynthesizingMethodParameter(method, 0, 2);

    assertThat(methodParameter.getMethod()).isEqualTo(method);
    assertThat(methodParameter.getParameterIndex()).isEqualTo(0);
    assertThat(methodParameter.getNestingLevel()).isEqualTo(2);
  }

  @Test
  void constructorWithConstructorAndNestingLevel() throws Exception {
    Constructor<TestClass> constructor = TestClass.class.getConstructor(String.class);
    SynthesizingMethodParameter methodParameter = new SynthesizingMethodParameter(constructor, 0, 3);

    assertThat(methodParameter.getConstructor()).isEqualTo(constructor);
    assertThat(methodParameter.getParameterIndex()).isEqualTo(0);
    assertThat(methodParameter.getNestingLevel()).isEqualTo(3);
  }

  @Test
  void copyConstructor() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class);
    SynthesizingMethodParameter original = new SynthesizingMethodParameter(method, 0);
    SynthesizingMethodParameter copy = new SynthesizingMethodParameter(original);

    assertThat(copy).isEqualTo(original);
    assertThat(copy).isNotSameAs(original);
  }

  @Test
  void forExecutableWithInvalidType() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> SynthesizingMethodParameter.forExecutable(null, 0))
            .withMessageContaining("Not a Method/Constructor: null");
  }

  @Test
  void adaptAnnotationSynthesizesAnnotations() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("annotatedMethod", String.class);
    SynthesizingMethodParameter methodParameter = new SynthesizingMethodParameter(method, 0);

    TestAnnotation annotation = methodParameter.getParameterAnnotation(TestAnnotation.class);
    assertThat(annotation).isNotNull();
    // Verify that the annotation has been synthesized (implementation detail)
    assertThat(annotation.toString()).contains("Synthesizing");
  }

  @Test
  void getParameterAnnotationsReturnsSynthesizedAnnotations() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("annotatedMethod", String.class);
    SynthesizingMethodParameter methodParameter = new SynthesizingMethodParameter(method, 0);

    assertThat(methodParameter.getParameterAnnotations()).hasSize(1);
    TestAnnotation annotation = (TestAnnotation) methodParameter.getParameterAnnotations()[0];
    assertThat(annotation.value()).isEqualTo("test");
    assertThat(annotation.toString()).contains("Synthesizing");
  }

  @Test
  void getMethodAnnotationsReturnsSynthesizedAnnotations() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("methodWithAnnotation");
    SynthesizingMethodParameter methodParameter = new SynthesizingMethodParameter(method, -1);

    assertThat(methodParameter.getMethodAnnotations()).hasSize(1);
    TestAnnotation annotation = (TestAnnotation) methodParameter.getMethodAnnotations()[0];
    assertThat(annotation.value()).isEqualTo("method-test");
    assertThat(annotation.toString()).contains("Synthesizing");
  }

  @Test
  void constructorWithMethodOnly() throws Exception {
    Method method = TestClass.class.getDeclaredMethod("testMethod", String.class);
    SynthesizingMethodParameter methodParameter = new SynthesizingMethodParameter(method, 0);

    assertThat(methodParameter.getMethod()).isEqualTo(method);
    assertThat(methodParameter.getParameterIndex()).isEqualTo(0);
    assertThat(methodParameter.getNestingLevel()).isEqualTo(1); // default nesting level
  }

  @Test
  void constructorWithConstructorOnly() throws Exception {
    Constructor<TestClass> constructor = TestClass.class.getConstructor(String.class);
    SynthesizingMethodParameter methodParameter = new SynthesizingMethodParameter(constructor, 0);

    assertThat(methodParameter.getConstructor()).isEqualTo(constructor);
    assertThat(methodParameter.getParameterIndex()).isEqualTo(0);
    assertThat(methodParameter.getNestingLevel()).isEqualTo(1); // default nesting level
  }

  @java.lang.annotation.Target({ java.lang.annotation.ElementType.PARAMETER, java.lang.annotation.ElementType.METHOD })
  @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
  @interface TestAnnotation {
    String value() default "";
  }

  static class TestClass {

    public TestClass(String name) { }

    public void testMethod(String param) { }

    public void annotatedMethod(@TestAnnotation("test") String param) { }

    public void multipleAnnotatedMethod(@TestAnnotation("test2") String param) { }

    @TestAnnotation("method-test")
    public void methodWithAnnotation() { }
  }

}