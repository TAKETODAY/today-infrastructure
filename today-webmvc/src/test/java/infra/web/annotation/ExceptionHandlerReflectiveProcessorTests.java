/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.annotation;

import org.junit.jupiter.api.Test;

import infra.aot.hint.ReflectionHints;
import infra.core.MethodParameter;
import infra.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 15:07
 */
class ExceptionHandlerReflectiveProcessorTests {

  @Test
  void registerReturnTypeHintsRegistersProblemDetailSubclasses() throws NoSuchMethodException {
    infra.web.annotation.ExceptionHandlerReflectiveProcessor processor = new ExceptionHandlerReflectiveProcessor();
    ReflectionHints hints = new ReflectionHints();

    MethodParameter methodParameter = MethodParameter.forExecutable(
            TestController.class.getDeclaredMethod("handleWithProblemDetail"), -1);

    processor.registerReturnTypeHints(hints, methodParameter);

    assertThat(hints.getTypeHint(ProblemDetail.class)).isNotNull();
  }

  @Test
  void registerReturnTypeHintsDelegatesToSuperClassForNonProblemDetailTypes() throws NoSuchMethodException {
    ExceptionHandlerReflectiveProcessor processor = new ExceptionHandlerReflectiveProcessor();
    ReflectionHints hints = new ReflectionHints();

    MethodParameter methodParameter = MethodParameter.forExecutable(
            TestController.class.getDeclaredMethod("handleWithString"), -1);

    processor.registerReturnTypeHints(hints, methodParameter);
  }

  static class TestController {

    @infra.web.annotation.ExceptionHandler
    public ProblemDetail handleWithProblemDetail() {
      return ProblemDetail.forStatus(infra.http.HttpStatus.BAD_REQUEST);
    }

    @infra.web.annotation.ExceptionHandler
    public String handleWithString() {
      return "error";
    }
  }

}