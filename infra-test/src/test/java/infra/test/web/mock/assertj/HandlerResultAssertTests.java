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

package infra.test.web.mock.assertj;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.http.ResponseEntity;
import infra.util.ReflectionUtils;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PostMapping;
import infra.web.annotation.RestController;
import infra.web.handler.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link infra.test.web.mock.assertj.HandlerResultAssert}.
 *
 * @author Stephane Nicoll
 */
class HandlerResultAssertTests {

  @Test
  void hasTypeUseController() {
    assertThat(handlerMethod(new TestController(), "greet")).hasType(TestController.class);
  }

  @Test
  void isMethodHandlerWithMethodHandler() {
    assertThat(handlerMethod(new TestController(), "greet")).isMethodHandler();
  }

  @Test
  void methodName() {
    assertThat(handlerMethod(new TestController(), "greet")).method().hasName("greet");
  }

  @Test
  void declaringClass() {
    assertThat(handlerMethod(new TestController(), "greet")).method().hasDeclaringClass(TestController.class);
  }

  @Test
  void method() {
    assertThat(handlerMethod(new TestController(), "greet")).method().isEqualTo(method(TestController.class, "greet"));
  }

  @Test
  void isInvokedOn() {
    assertThat(handlerMethod(new TestController(), "greet"))
            .isInvokedOn(TestController.class, TestController::greet);
  }

  @Test
  void isInvokedOnWithVoidMethod() {
    assertThat(handlerMethod(new TestController(), "update"))
            .isInvokedOn(TestController.class, controller -> {
              controller.update();
              return controller;
            });
  }

  @Test
  void isInvokedOnWithWrongMethod() {
    AssertProvider<infra.test.web.mock.assertj.HandlerResultAssert> actual = handlerMethod(new TestController(), "update");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(actual).isInvokedOn(TestController.class, TestController::greet))
            .withMessageContainingAll(
                    method(TestController.class, "greet").toGenericString(),
                    method(TestController.class, "update").toGenericString());
  }

  private static AssertProvider<infra.test.web.mock.assertj.HandlerResultAssert> handler(Object instance) {
    return () -> new infra.test.web.mock.assertj.HandlerResultAssert(instance);
  }

  private static AssertProvider<infra.test.web.mock.assertj.HandlerResultAssert> handlerMethod(Object instance, String name, Class<?>... parameterTypes) {
    HandlerMethod handlerMethod = new HandlerMethod(instance, method(instance.getClass(), name, parameterTypes));
    return () -> new infra.test.web.mock.assertj.HandlerResultAssert(handlerMethod);
  }

  private static Method method(Class<?> target, String name, Class<?>... parameterTypes) {
    Method method = ReflectionUtils.findMethod(target, name, parameterTypes);
    assertThat(method).isNotNull();
    return method;
  }

  @RestController
  static class TestController {

    @GetMapping("/greet")
    ResponseEntity<String> greet() {
      return ResponseEntity.ok().body("Hello");
    }

    @PostMapping("/update")
    void update() {
    }

  }

}
