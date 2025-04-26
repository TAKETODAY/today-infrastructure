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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 *
 * @since 5.0 2025/3/27 22:22
 */
class SimpleErrorsTests {

  static class TestBean implements Serializable {
    private String name;
    private int age;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }

    public void setAge(int age) { this.age = age; }
  }

  @Test
  void shouldCreateEmptyErrors() {
    TestBean target = new TestBean();
    SimpleErrors errors = new SimpleErrors(target);
    assertThat(errors.hasErrors()).isFalse();
    assertThat(errors.getErrorCount()).isEqualTo(0);
    assertThat(errors.getAllErrors()).isEmpty();
  }

  @Test
  void shouldRejectGlobalErrorValue() {
    TestBean target = new TestBean();
    SimpleErrors errors = new SimpleErrors(target);
    errors.reject("code", "defaultMessage");

    assertThat(errors.hasErrors()).isTrue();
    assertThat(errors.getErrorCount()).isEqualTo(1);

    ObjectError error = errors.getGlobalErrors().get(0);
    assertThat(error.getCode()).isEqualTo("code");
    assertThat(error.getDefaultMessage()).isEqualTo("defaultMessage");
  }

  @Test
  void shouldRejectGlobalErrorWithArgs() {
    TestBean target = new TestBean();
    SimpleErrors errors = new SimpleErrors(target);
    Object[] args = new Object[] { "arg1", "arg2" };
    errors.reject("code", args, "defaultMessage");

    ObjectError error = errors.getGlobalError();
    assertThat(error.getArguments()).isEqualTo(args);
  }

  @Test
  void shouldRejectFieldErrorValue() {
    TestBean target = new TestBean();
    SimpleErrors errors = new SimpleErrors(target);
    errors.rejectValue("name", "code", "defaultMessage");  // 修改 field 为 name

    assertThat(errors.hasFieldErrors()).isTrue();
    assertThat(errors.hasFieldErrors("name")).isTrue();

    FieldError error = errors.getFieldError("name");
    assertThat(error.getField()).isEqualTo("name");
    assertThat(error.getCode()).isEqualTo("code");
    assertThat(error.getDefaultMessage()).isEqualTo("defaultMessage");
  }

  @Test
  void shouldRejectFieldErrorWithRejectedValue() {
    TestBean target = new TestBean();
    SimpleErrors errors = new SimpleErrors(target);
    errors.rejectValue("name", "code",
            new Object[] { "invalidValue" }, "defaultMessage");

    FieldError error = errors.getFieldError("name");
    assertThat(error.getRejectedValue()).isNull();
  }

  @Test
  void shouldAddAllErrorsFromOtherErrors() {
    TestBean target1 = new TestBean();
    TestBean target2 = new TestBean();
    SimpleErrors errors1 = new SimpleErrors(target1);
    errors1.reject("code1");
    errors1.rejectValue("name", "code2");

    SimpleErrors errors2 = new SimpleErrors(target2);
    errors2.reject("code3");
    errors2.rejectValue("age", "code4");

    errors1.addAllErrors(errors2);

    assertThat(errors1.getErrorCount()).isEqualTo(4);
    assertThat(errors1.getGlobalErrors()).hasSize(2);
    assertThat(errors1.getFieldErrors()).hasSize(2);
  }

  @Test
  void shouldBeSerializable() throws Exception {
    TestBean target = new TestBean();
    target.setName("test");
    target.setAge(25);

    SimpleErrors errors = new SimpleErrors(target);
    errors.reject("globalCode", "global message");
    errors.rejectValue("name", "nameCode", "name error");

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(errors);

    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bis);
    SimpleErrors deserialized = (SimpleErrors) ois.readObject();

    assertThat(deserialized.getGlobalErrors()).hasSize(1);
    assertThat(deserialized.getFieldErrors()).hasSize(1);

    ObjectError globalError = deserialized.getGlobalError();
    assertThat(globalError.getCode()).isEqualTo("globalCode");
    assertThat(globalError.getDefaultMessage()).isEqualTo("global message");

    FieldError fieldError = deserialized.getFieldError("name");
    assertThat(fieldError.getField()).isEqualTo("name");
    assertThat(fieldError.getCode()).isEqualTo("nameCode");
    assertThat(fieldError.getDefaultMessage()).isEqualTo("name error");
  }

  @Test
  void shouldReturnNullForNonExistentErrors() {
    TestBean target = new TestBean();
    SimpleErrors errors = new SimpleErrors(target);

    assertThatThrownBy(() -> errors.getFieldValue("nonexistent"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot retrieve value for field 'nonexistent'");

    assertThat(errors.getGlobalError()).isNull();
    assertThat(errors.getFieldError("nonexistent")).isNull();
  }

  @Test
  void shouldCreateErrorsWithTarget() {
    TestBean target = new TestBean();
    SimpleErrors errors = new SimpleErrors(target);

    assertThat(errors.getObjectName()).isEqualTo("TestBean");
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void shouldCreateErrorsWithTargetAndName() {
    TestBean target = new TestBean();
    SimpleErrors errors = new SimpleErrors(target, "testBean");

    assertThat(errors.getObjectName()).isEqualTo("testBean");
  }

  @Test
  void shouldThrowExceptionForNullTarget() {
    assertThatThrownBy(() -> new SimpleErrors(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Target is required");
  }

  @Test
  void shouldGetFieldValue() {
    TestBean target = new TestBean();
    target.setName("test");
    target.setAge(25);

    SimpleErrors errors = new SimpleErrors(target);

    assertThat(errors.getFieldValue("name")).isEqualTo("test");
    assertThat(errors.getFieldValue("age")).isEqualTo(25);
  }

  @Test
  void shouldGetFieldType() {
    TestBean target = new TestBean();
    SimpleErrors errors = new SimpleErrors(target);

    assertThat(errors.getFieldType("name")).isEqualTo(String.class);
    assertThat(errors.getFieldType("age")).isEqualTo(int.class);
    assertThat(errors.getFieldType("nonexistent")).isNull();
  }

  @Test
  void shouldTestEqualsAndHashCode() {
    TestBean target = new TestBean();
    SimpleErrors errors1 = new SimpleErrors(target);
    SimpleErrors errors2 = new SimpleErrors(target);
    SimpleErrors errors3 = new SimpleErrors(new TestBean());

    errors1.reject("code");
    errors2.reject("code");

    assertThat(errors1).isEqualTo(errors1);
    assertThat(errors1).isEqualTo(errors2);
    assertThat(errors1).isNotEqualTo(errors3);
    assertThat(errors1).isNotEqualTo(null);
    assertThat(errors1).isNotEqualTo(new Object());

    assertThat(errors1.hashCode()).isEqualTo(errors2.hashCode());
  }

  @Test
  void shouldHandleToString() {
    TestBean target = new TestBean();
    SimpleErrors errors = new SimpleErrors(target);

    errors.reject("code1", "global error");
    errors.rejectValue("name", "code2", "field error");

    String result = errors.toString();
    assertThat(result).contains("code1");
    assertThat(result).contains("code2");
    assertThat(result).contains("global error");
    assertThat(result).contains("field error");
  }

  @Test
  void shouldRejectEmptyFieldAsGlobalError() {
    SimpleErrors errors = new SimpleErrors(new TestBean());
    errors.rejectValue("", "code", "message");

    assertThat(errors.hasGlobalErrors()).isTrue();
    assertThat(errors.hasFieldErrors()).isFalse();
  }

  @Test
  void shouldThrowExceptionForInvalidField() {
    TestBean target = new TestBean();
    SimpleErrors errors = new SimpleErrors(target);

    assertThatThrownBy(() -> errors.getFieldValue("invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot retrieve value for field 'invalid'");
  }

}
