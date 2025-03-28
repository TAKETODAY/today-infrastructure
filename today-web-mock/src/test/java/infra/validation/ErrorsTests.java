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

package infra.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 22:13
 */
class ErrorsTests {

  @Test
  void hasErrorsReturnsFalseWhenNoErrorsPresent() {
    SimpleErrors errors = new SimpleErrors("test");
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void rejectValueWithNullFieldCreatesGlobalError() {
    SimpleErrors errors = new SimpleErrors("test");
    errors.rejectValue(null, "error.code");

    assertThat(errors.hasGlobalErrors()).isTrue();
    assertThat(errors.hasFieldErrors()).isFalse();
  }

  @Test
  void getFirstErrorReturnsEarliestError() {
    SimpleErrors errors = new SimpleErrors("test");
    errors.reject("global1");
    errors.reject("global2");

    ObjectError first = errors.getGlobalError();
    assertThat(first.getCode()).isEqualTo("global1");
  }

  @Test
  void failOnErrorThrowsExceptionWithErrorMessage() {
    SimpleErrors errors = new SimpleErrors("test");
    errors.reject("error.code", "Error message");

    assertThatThrownBy(() -> errors.failOnError(IllegalStateException::new))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Error message");
  }

  @Test
  void rejectWithArgsStoresErrorArguments() {
    SimpleErrors errors = new SimpleErrors("test");
    Object[] args = new Object[] { "arg1", "arg2" };
    errors.reject("error.code", args, "default");

    ObjectError error = errors.getGlobalError();
    assertThat(error.getArguments()).containsExactly(args);
  }

  @Test
  void getFieldTypeReturnsNullForNonExistentField() {
    SimpleErrors errors = new SimpleErrors("test");
    assertThat(errors.getFieldType("nonexistent")).isNull();
  }

  @Test
  void setNestedPathThrowsUnsupportedOperationByDefault() {
    SimpleErrors errors = new SimpleErrors("test");

    assertThatThrownBy(() -> errors.setNestedPath("nested"))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void getFieldValueReturnsValueForExistingPublicField() {
    TestBean bean = new TestBean();
    bean.publicField = "test";
    SimpleErrors errors = new SimpleErrors(bean);

    assertThat(errors.getFieldValue("publicField")).isEqualTo("test");
  }

  @Test
  void hasErrorsReturnsTrueWhenFieldErrorPresent() {
    TestBean target = new TestBean();
    target.publicField = "test";
    SimpleErrors errors = new SimpleErrors(target);
    errors.rejectValue("publicField", "error.code");
    assertThat(errors.hasErrors()).isTrue();
  }

  @Test
  void hasErrorsReturnsTrueWhenGlobalErrorPresent() {
    SimpleErrors errors = new SimpleErrors("test");
    errors.reject("error.code");
    assertThat(errors.hasErrors()).isTrue();
  }

  @Test
  void getErrorCountReturnsCorrectTotal() {
    TestBean target = new TestBean();
    target.publicField = "test";
    SimpleErrors errors = new SimpleErrors(target);
    errors.reject("global.error");
    errors.rejectValue("publicField", "field.error");
    errors.rejectValue("publicField", "field.error2");

    assertThat(errors.getErrorCount()).isEqualTo(3);
  }

  @Test
  void getAllErrorsReturnsGlobalAndFieldErrors() {
    TestBean target = new TestBean();
    target.publicField = "test";
    SimpleErrors errors = new SimpleErrors(target);
    errors.reject("global.error");
    errors.rejectValue("publicField", "field.error");

    List<ObjectError> allErrors = errors.getAllErrors();
    assertThat(allErrors).hasSize(2);
    assertThat(allErrors).extracting("code").containsExactly("global.error", "field.error");
  }

  @Test
  void getFieldErrorsForSpecificFieldReturnsMatchingErrors() {
    TestBean target = new TestBean();
    target.publicField = "test";
    SimpleErrors errors = new SimpleErrors(target);
    errors.rejectValue("publicField", "error1");
    errors.rejectValue("publicField", "error2");

    List<FieldError> fieldErrors = errors.getFieldErrors("publicField");
    assertThat(fieldErrors).hasSize(2);
    assertThat(fieldErrors).extracting("code").containsExactly("error1", "error2");
  }

  @Test
  void getFirstFieldErrorReturnsEarliestFieldError() {
    TestBean target = new TestBean();
    target.publicField = "test";
    SimpleErrors errors = new SimpleErrors(target);
    errors.rejectValue("publicField", "error1");
    errors.rejectValue("publicField", "error2");

    FieldError first = errors.getFieldError();
    assertThat(first.getCode()).isEqualTo("error1");
  }

  @Test
  void addAllErrorsCopiesErrorsFromOtherInstance() {
    TestBean target = new TestBean();
    target.publicField = "test";

    SimpleErrors source = new SimpleErrors(target);
    source.reject("global");
    source.rejectValue("publicField", "field");

    SimpleErrors dest = new SimpleErrors(target);
    dest.addAllErrors(source);

    assertThat(dest.getAllErrors()).hasSize(2);
    assertThat(dest.getGlobalError().getCode()).isEqualTo("global");
    assertThat(dest.getFieldError().getCode()).isEqualTo("field");
  }

  @Test
  void rejectOnlyErrorCode() {
    SimpleErrors errors = new SimpleErrors("test");
    errors.reject("error.code");

    ObjectError error = errors.getGlobalError();
    assertThat(error.getCode()).isEqualTo("error.code");
    assertThat(error.getDefaultMessage()).isNull();
    assertThat(error.getArguments()).isNull();
  }

  @Test
  void rejectWithDefaultMessage() {
    SimpleErrors errors = new SimpleErrors("test");
    errors.reject("error.code", "default message");

    ObjectError error = errors.getGlobalError();
    assertThat(error.getCode()).isEqualTo("error.code");
    assertThat(error.getDefaultMessage()).isEqualTo("default message");
    assertThat(error.getArguments()).isNull();
  }

  @Test
  void throwsExceptionWhenPushingNestedPath() {
    SimpleErrors errors = new SimpleErrors("test");

    assertThatThrownBy(() -> errors.pushNestedPath("nested"))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("does not support nested paths");
  }

  @Test
  void throwsExceptionWhenPoppingWithoutPushing() {
    SimpleErrors errors = new SimpleErrors("test");

    assertThatThrownBy(() -> errors.popNestedPath())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no nested path on stack");
  }

  @Test
  void getFieldErrorCountReturnsZeroForNonExistentField() {
    SimpleErrors errors = new SimpleErrors("test");
    assertThat(errors.getFieldErrorCount("nonexistent")).isZero();
  }

  private static class TestBean {
    public String publicField;
    private String privateField;
    public NestedBean nested;
  }

  private static class NestedBean {
    public String field;
  }

}