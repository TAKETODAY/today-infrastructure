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
  void registerReturnTypeHintsRegistersProblemDetailSubclasses() {
    ExceptionHandlerReflectiveProcessor processor = new ExceptionHandlerReflectiveProcessor();
    ReflectionHints hints = new ReflectionHints();

    MethodParameter methodParameter = MethodParameter.forExecutable(
            TestController.class.getDeclaredMethods()[0], -1);

    processor.registerReturnTypeHints(hints, methodParameter);

    assertThat(hints.getTypeHint(ProblemDetail.class)).isNotNull();
  }

  @Test
  void registerReturnTypeHintsDelegatesToSuperClassForNonProblemDetailTypes() {
    ExceptionHandlerReflectiveProcessor processor = new ExceptionHandlerReflectiveProcessor();
    ReflectionHints hints = new ReflectionHints();

    MethodParameter methodParameter = MethodParameter.forExecutable(
            TestController.class.getDeclaredMethods()[1], -1);

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