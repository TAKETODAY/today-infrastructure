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

package infra.validation.method;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import infra.core.MethodParameter;
import infra.validation.FieldError;
import infra.validation.MapBindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ParameterErrorsTests {

  @Test
  void exposesTheOriginalFieldErrorAndItsSourceWithContainerContext() throws Exception {
    var argument = Map.of("name", "");
    var container = List.of(argument);
    var binding = new MapBindingResult(argument, "input");
    var fieldError = new FieldError("input", "name", "required");
    var source = new Object();
    fieldError.wrap(source);
    binding.addError(fieldError);
    var parameter = new MethodParameter(Handler.class.getDeclaredMethod("handle", List.class), 0);
    var errors = new ParameterErrors(parameter, argument, binding, container, 0, null);

    assertThat(errors.getMethodParameter()).isSameAs(parameter);
    assertThat(errors.getArgument()).isSameAs(argument);
    assertThat(errors.getContainer()).isSameAs(container);
    assertThat(errors.getContainerIndex()).isZero();
    assertThat(errors.getContainerKey()).isNull();
    assertThat(errors.getObjectName()).isEqualTo("input");
    assertThat(errors.getResolvableErrors()).containsExactly(fieldError);
    assertThat(errors.unwrap(fieldError, Object.class)).isSameAs(source);
    assertThat(errors.toString()).contains("containerIndex[0]");
  }

  @Test
  void recordsAndQueriesGlobalAndNestedFieldErrorsThroughTheBindingResult() throws Exception {
    var argument = Map.of("profile.name", "", "profile.age", 0);
    var binding = new MapBindingResult(argument, "input");
    binding.reject("initial");
    var parameter = new MethodParameter(Handler.class.getDeclaredMethod("handle", List.class), 0);
    var errors = new ParameterErrors(parameter, argument, binding, null, null, null);

    errors.reject("conflict");
    errors.reject("invalid", "Invalid profile");
    errors.reject("unavailable", new Object[] { "input" }, "Unavailable input");
    errors.setNestedPath("profile");
    assertThat(errors.getNestedPath()).isEqualTo("profile.");
    errors.rejectValue("name", "required");
    errors.rejectValue("name", "length", "Too short");
    errors.rejectValue("age", "min", new Object[] { 18 }, "Too young");
    errors.pushNestedPath("details");
    assertThat(errors.getNestedPath()).isEqualTo("profile.details.");
    errors.popNestedPath();
    assertThat(errors.getNestedPath()).isEqualTo("profile.");
    assertThatIllegalStateException().isThrownBy(errors::popNestedPath);

    var otherErrors = new MapBindingResult(argument, "input");
    otherErrors.reject("other");
    errors.addAllErrors(otherErrors);

    assertThat(errors.hasErrors()).isTrue();
    assertThat(errors.getErrorCount()).isEqualTo(8);
    assertThat(errors.getAllErrors()).containsExactlyElementsOf(binding.getAllErrors());
    assertThat(errors.hasGlobalErrors()).isTrue();
    assertThat(errors.getGlobalErrorCount()).isEqualTo(5);
    assertThat(errors.getGlobalError()).isSameAs(binding.getGlobalError());
    assertThat(errors.getGlobalErrors()).containsExactlyElementsOf(binding.getGlobalErrors());
    assertThat(errors.hasFieldErrors()).isTrue();
    assertThat(errors.getFieldErrorCount()).isEqualTo(3);
    assertThat(errors.getFieldError()).isSameAs(binding.getFieldError());
    assertThat(errors.getFieldErrors()).containsExactlyElementsOf(binding.getFieldErrors());
    assertThat(errors.hasFieldErrors("name")).isTrue();
    assertThat(errors.hasFieldErrors("missing")).isFalse();
    assertThat(errors.getFieldErrorCount("name")).isEqualTo(2);
    assertThat(errors.getFieldErrors("name")).extracting(FieldError::getCode).containsExactly("required", "length");
    assertThat(errors.getFieldError("age")).isSameAs(binding.getFieldError("age"));
    assertThat(errors.getFieldType("age")).isEqualTo(Integer.class);
  }

  private interface Handler {

    void handle(List<?> input);
  }

}
