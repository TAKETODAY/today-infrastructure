/*
 * Copyright 2017 - 2024 the original author or authors.
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

import infra.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ValidationUtils}.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 */
public class ValidationUtilsTests {

  private final Validator emptyValidator = Validator.forInstanceOf(TestBean.class, (testBean, errors) -> ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY", "You must enter a name!"));

  private final Validator emptyOrWhitespaceValidator = Validator.forInstanceOf(TestBean.class,
          (testBean, errors) -> ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", "You must enter a name!"));

  @Test
  public void testInvokeValidatorWithNullValidator() throws Exception {
    TestBean tb = new TestBean();
    Errors errors = new BeanPropertyBindingResult(tb, "tb");
    assertThatIllegalArgumentException().isThrownBy(() ->
            ValidationUtils.invokeValidator(null, tb, errors));
  }

  @Test
  public void testInvokeValidatorWithNullErrors() throws Exception {
    TestBean tb = new TestBean();
    assertThatIllegalArgumentException().isThrownBy(() ->
            ValidationUtils.invokeValidator(emptyValidator, tb, null));
  }

  @Test
  public void testInvokeValidatorSunnyDay() throws Exception {
    TestBean tb = new TestBean();
    Errors errors = new BeanPropertyBindingResult(tb, "tb");
    ValidationUtils.invokeValidator(emptyValidator, tb, errors);
    assertThat(errors.hasFieldErrors("name")).isTrue();
    assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");
  }

  @Test
  public void testValidationUtilsSunnyDay() throws Exception {
    TestBean tb = new TestBean("");

    tb.setName(" ");
    Errors errors = new BeanPropertyBindingResult(tb, "tb");
    emptyValidator.validate(tb, errors);
    assertThat(errors.hasFieldErrors("name")).isFalse();

    tb.setName("Roddy");
    errors = new BeanPropertyBindingResult(tb, "tb");
    emptyValidator.validate(tb, errors);
    assertThat(errors.hasFieldErrors("name")).isFalse();
  }

  @Test
  public void testValidationUtilsNull() throws Exception {
    TestBean tb = new TestBean();
    Errors errors = new BeanPropertyBindingResult(tb, "tb");
    emptyValidator.validate(tb, errors);
    assertThat(errors.hasFieldErrors("name")).isTrue();
    assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");
  }

  @Test
  public void testValidationUtilsEmpty() throws Exception {
    TestBean tb = new TestBean("");
    Errors errors = new BeanPropertyBindingResult(tb, "tb");
    emptyValidator.validate(tb, errors);
    assertThat(errors.hasFieldErrors("name")).isTrue();
    assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");
  }

  @Test
  public void testValidationUtilsEmptyVariants() {
    TestBean tb = new TestBean();

    Errors errors = new BeanPropertyBindingResult(tb, "tb");
    ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] { "arg" });
    assertThat(errors.hasFieldErrors("name")).isTrue();
    assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
    assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");

    errors = new BeanPropertyBindingResult(tb, "tb");
    ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] { "arg" }, "msg");
    assertThat(errors.hasFieldErrors("name")).isTrue();
    assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
    assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");
    assertThat(errors.getFieldError("name").getDefaultMessage()).isEqualTo("msg");
  }

  @Test
  public void testValidationUtilsEmptyOrWhitespace() throws Exception {
    TestBean tb = new TestBean();

    // Test null
    Errors errors = new BeanPropertyBindingResult(tb, "tb");
    emptyOrWhitespaceValidator.validate(tb, errors);
    assertThat(errors.hasFieldErrors("name")).isTrue();
    assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");

    // Test empty String
    tb.setName("");
    errors = new BeanPropertyBindingResult(tb, "tb");
    emptyOrWhitespaceValidator.validate(tb, errors);
    assertThat(errors.hasFieldErrors("name")).isTrue();
    assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");

    // Test whitespace String
    tb.setName(" ");
    errors = new BeanPropertyBindingResult(tb, "tb");
    emptyOrWhitespaceValidator.validate(tb, errors);
    assertThat(errors.hasFieldErrors("name")).isTrue();
    assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");

    // Test OK
    tb.setName("Roddy");
    errors = new BeanPropertyBindingResult(tb, "tb");
    emptyOrWhitespaceValidator.validate(tb, errors);
    assertThat(errors.hasFieldErrors("name")).isFalse();
  }

  @Test
  public void testValidationUtilsEmptyOrWhitespaceVariants() {
    TestBean tb = new TestBean();
    tb.setName(" ");

    Errors errors = new BeanPropertyBindingResult(tb, "tb");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] { "arg" });
    assertThat(errors.hasFieldErrors("name")).isTrue();
    assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
    assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");

    errors = new BeanPropertyBindingResult(tb, "tb");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] { "arg" }, "msg");
    assertThat(errors.hasFieldErrors("name")).isTrue();
    assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
    assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");
    assertThat(errors.getFieldError("name").getDefaultMessage()).isEqualTo("msg");
  }

}
