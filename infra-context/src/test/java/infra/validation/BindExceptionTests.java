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

package infra.validation;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditorSupport;

import infra.beans.PropertyValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BindExceptionTests {

  @Test
  void preservesSubmittedValuesAndEditorsWhenBindingFails() {
    var account = new Account();
    account.age = 21;
    var binder = new DataBinder(account, "account");
    binder.initDirectFieldAccess();
    binder.setAllowedFields("age");
    var editor = new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) {
        setValue(Integer.parseInt(text));
      }
    };
    binder.registerCustomEditor(int.class, "age", editor);
    var submitted = new PropertyValues();
    submitted.add("age", "invalid");
    submitted.add("role", "admin");
    binder.bind(submitted);

    assertThatExceptionOfType(BindException.class).isThrownBy(binder::close).satisfies(exception -> {
      var result = binder.getBindingResult();
      assertThat(exception.getBindingResult()).isSameAs(result);
      assertThat(exception.getTarget()).isSameAs(account);
      assertThat(exception.getModel()).containsEntry("account", account)
              .containsEntry(BindingResult.MODEL_KEY_PREFIX + "account", result);
      assertThat(exception.hasErrors()).isTrue();
      assertThat(exception.hasFieldErrors()).isTrue();
      assertThat(exception.getFieldErrorCount()).isEqualTo(1);
      assertThat(exception.getFieldError()).isSameAs(result.getFieldError("age"));
      assertThat(exception.getFieldValue("age")).isEqualTo("invalid");
      assertThat(exception.getRawFieldValue("age")).isEqualTo(21);
      assertThat(exception.getFieldType("age")).isEqualTo(int.class);
      assertThat(exception.getSuppressedFields()).containsExactly("role");
      assertThat(exception.findEditor("age", int.class)).isSameAs(editor);
      assertThat(exception.getPropertyEditorRegistry()).isSameAs(result.getPropertyEditorRegistry());
      assertThat(exception.getMessage()).contains("account", "age", "invalid");
      assertThat(exception).isEqualTo(new BindException(result)).hasSameHashCodeAs(result);
    });
  }

  @Test
  void addsNestedAndGlobalValidationErrorsToItsBackingResult() {
    var registration = new Registration();
    var binder = new DataBinder(registration, "registration");
    binder.initDirectFieldAccess();
    var exception = new BindException(binder.getBindingResult());
    var duplicates = new DirectFieldBindingResult(registration, "registration");
    duplicates.reject("duplicate");

    exception.reject("invalid", new Object[] { "registration" }, "Registration is invalid");
    exception.addAllErrors(duplicates);
    exception.setNestedPath("account");
    exception.pushNestedPath("");
    exception.rejectValue("age", "min", new Object[] { 18 }, "Must be an adult");
    exception.popNestedPath();

    assertThat(exception.getNestedPath()).isEqualTo("account.");
    assertThat(exception.getGlobalErrorCount()).isEqualTo(2);
    assertThat(exception.getGlobalErrors()).extracting(ObjectError::getCode).containsExactly("invalid", "duplicate");
    assertThat(exception.getFieldErrorCount("age")).isEqualTo(1);
    assertThat(exception.getFieldErrors("age")).singleElement().satisfies(error -> {
      assertThat(error.getField()).isEqualTo("account.age");
      assertThat(error.getArguments()).containsExactly(18);
      assertThat(error.getCodes()).containsExactly(exception.resolveMessageCodes("min", "age"));
    });
    assertThat(exception.resolveMessageCodes("invalid")).containsExactly("invalid.registration", "invalid");
    exception.recordSuppressedField("account.role");
    assertThat(binder.getBindingResult().getSuppressedFields()).containsExactly("account.role");
  }

  private static class Registration {

    private final Account account = new Account();
  }

  private static class Account {

    private int age;
  }

}
