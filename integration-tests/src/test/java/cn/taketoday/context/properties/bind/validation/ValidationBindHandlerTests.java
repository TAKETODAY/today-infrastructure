/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.bind.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.context.properties.bind.AbstractBindHandler;
import cn.taketoday.context.properties.bind.BindContext;
import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.context.properties.source.MockConfigurationPropertySource;
import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.origin.Origin;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.FieldError;
import cn.taketoday.validation.ObjectError;
import cn.taketoday.validation.ValidationUtils;
import cn.taketoday.validation.Validator;
import cn.taketoday.validation.annotation.Validated;
import cn.taketoday.validation.beanvalidation.LocalValidatorFactoryBean;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ValidationBindHandler}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ValidationBindHandlerTests {

  private List<ConfigurationPropertySource> sources = new ArrayList<>();

  private ValidationBindHandler handler;

  private Binder binder;

  private LocalValidatorFactoryBean validator;

  @BeforeEach
  void setup() {
    this.binder = new Binder(this.sources);
    this.validator = new LocalValidatorFactoryBean();
    this.validator.afterPropertiesSet();
    this.handler = new ValidationBindHandler(this.validator);
  }

  @Test
  void bindShouldBindWithoutHandler() {
    this.sources.add(new MockConfigurationPropertySource("foo.age", 4));
    ExampleValidatedBean bean = this.binder.bind("foo", Bindable.of(ExampleValidatedBean.class)).get();
    assertThat(bean.getAge()).isEqualTo(4);
  }

  @Test
  void bindShouldFailWithHandler() {
    this.sources.add(new MockConfigurationPropertySource("foo.age", 4));
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bind("foo", Bindable.of(ExampleValidatedBean.class), this.handler))
            .withCauseInstanceOf(BindValidationException.class);
  }

  @Test
  void bindShouldValidateNestedProperties() {
    this.sources.add(new MockConfigurationPropertySource("foo.nested.age", 4));
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(
                    () -> this.binder.bind("foo", Bindable.of(ExampleValidatedWithNestedBean.class), this.handler))
            .withCauseInstanceOf(BindValidationException.class);
  }

  @Test
  void bindShouldFailWithAccessToOrigin() {
    this.sources.add(new MockConfigurationPropertySource("foo.age", 4, "file"));
    BindValidationException cause = bindAndExpectValidationError(() -> this.binder
            .bind(ConfigurationPropertyName.of("foo"), Bindable.of(ExampleValidatedBean.class), this.handler));
    ObjectError objectError = cause.getValidationErrors().getAllErrors().get(0);
    assertThat(Origin.from(objectError).toString()).isEqualTo("file");
  }

  @Test
  void bindShouldFailWithAccessToBoundProperties() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.nested.name", "baz");
    source.put("foo.nested.age", "4");
    source.put("faf.bar", "baz");
    this.sources.add(source);
    BindValidationException cause = bindAndExpectValidationError(() -> this.binder.bind(
            ConfigurationPropertyName.of("foo"), Bindable.of(ExampleValidatedWithNestedBean.class), this.handler));
    Set<ConfigurationProperty> boundProperties = cause.getValidationErrors().getBoundProperties();
    assertThat(boundProperties).extracting((p) -> p.getName().toString()).contains("foo.nested.age",
            "foo.nested.name");
  }

  @Test
  void bindShouldFailWithAccessToNameAndValue() {
    this.sources.add(new MockConfigurationPropertySource("foo.nested.age", "4"));
    BindValidationException cause = bindAndExpectValidationError(() -> this.binder.bind(
            ConfigurationPropertyName.of("foo"), Bindable.of(ExampleValidatedWithNestedBean.class), this.handler));
    assertThat(cause.getValidationErrors().getName().toString()).isEqualTo("foo.nested");
    assertThat(cause.getMessage()).contains("nested.age");
    assertThat(cause.getMessage()).contains("rejected value [4]");
  }

  @Test
  void bindShouldFailIfExistingValueIsInvalid() {
    ExampleValidatedBean existingValue = new ExampleValidatedBean();
    BindValidationException cause = bindAndExpectValidationError(
            () -> this.binder.bind(ConfigurationPropertyName.of("foo"),
                    Bindable.of(ExampleValidatedBean.class).withExistingValue(existingValue), this.handler));
    FieldError fieldError = (FieldError) cause.getValidationErrors().getAllErrors().get(0);
    assertThat(fieldError.getField()).isEqualTo("age");
  }

  @Test
  void bindShouldValidateWithoutAnnotation() {
    ExampleNonValidatedBean existingValue = new ExampleNonValidatedBean();
    bindAndExpectValidationError(() -> this.binder.bind(ConfigurationPropertyName.of("foo"),
            Bindable.of(ExampleNonValidatedBean.class).withExistingValue(existingValue), this.handler));
  }

  @Test
  void bindShouldNotValidateDepthGreaterThanZero() {
    // gh-12227
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.bar", "baz");
    this.sources.add(source);
    ExampleValidatedBeanWithGetterException existingValue = new ExampleValidatedBeanWithGetterException();
    this.binder.bind(ConfigurationPropertyName.of("foo"),
            Bindable.of(ExampleValidatedBeanWithGetterException.class).withExistingValue(existingValue),
            this.handler);
  }

  @Test
  void bindShouldNotValidateIfOtherHandlersInChainThrowError() {
    this.sources.add(new MockConfigurationPropertySource("foo", "hello"));
    ExampleValidatedBean bean = new ExampleValidatedBean();
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bind("foo",
                    Bindable.of(ExampleValidatedBean.class).withExistingValue(bean), this.handler))
            .withCauseInstanceOf(ConverterNotFoundException.class);
  }

  @Test
  void bindShouldValidateIfOtherHandlersInChainIgnoreError() {
    TestHandler testHandler = new TestHandler(null);
    this.handler = new ValidationBindHandler(testHandler, this.validator);
    this.sources.add(new MockConfigurationPropertySource("foo", "hello"));
    ExampleValidatedBean bean = new ExampleValidatedBean();
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bind("foo",
                    Bindable.of(ExampleValidatedBean.class).withExistingValue(bean), this.handler))
            .withCauseInstanceOf(BindValidationException.class);
  }

  @Test
  void bindShouldValidateIfOtherHandlersInChainReplaceErrorWithResult() {
    TestHandler testHandler = new TestHandler(new ExampleValidatedBeanSubclass());
    this.handler = new ValidationBindHandler(testHandler, this.validator);
    this.sources.add(new MockConfigurationPropertySource("foo", "hello"));
    this.sources.add(new MockConfigurationPropertySource("foo.age", "bad"));
    this.sources.add(new MockConfigurationPropertySource("foo.years", "99"));
    ExampleValidatedBean bean = new ExampleValidatedBean();
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bind("foo",
                    Bindable.of(ExampleValidatedBean.class).withExistingValue(bean), this.handler))
            .withCauseInstanceOf(BindValidationException.class)
            .satisfies((ex) -> assertThat(ex.getCause()).hasMessageContaining("years"));
  }

  @Test
  void validationErrorsForCamelCaseFieldsShouldContainRejectedValue() {
    this.sources.add(new MockConfigurationPropertySource("foo.inner.person-age", 2));
    BindValidationException cause = bindAndExpectValidationError(() -> this.binder
            .bind(ConfigurationPropertyName.of("foo"), Bindable.of(ExampleCamelCase.class), this.handler));
    assertThat(cause.getMessage()).contains("rejected value [2]");
  }

  @Test
  void validationShouldBeSkippedIfPreviousValidationErrorPresent() {
    this.sources.add(new MockConfigurationPropertySource("foo.inner.person-age", 2));
    BindValidationException cause = bindAndExpectValidationError(() -> this.binder
            .bind(ConfigurationPropertyName.of("foo"), Bindable.of(ExampleCamelCase.class), this.handler));
    FieldError fieldError = (FieldError) cause.getValidationErrors().getAllErrors().get(0);
    assertThat(fieldError.getField()).isEqualTo("personAge");
  }

  @Test
  void validateMapValues() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("test.items.[itemOne].number", "one");
    source.put("test.items.[ITEM2].number", "two");
    this.sources.add(source);
    Validator validator = getMapValidator();
    this.handler = new ValidationBindHandler(validator);
    this.binder.bind(ConfigurationPropertyName.of("test"), Bindable.of(ExampleWithMap.class), this.handler);
  }

  @Test
  void validateMapValuesWithNonUniformSource() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("test.items.itemOne.number", "one");
    map.put("test.items.ITEM2.number", "two");
    this.sources.add(ConfigurationPropertySources.from(new MapPropertySource("test", map)).iterator().next());
    Validator validator = getMapValidator();
    this.handler = new ValidationBindHandler(validator);
    this.binder.bind(ConfigurationPropertyName.of("test"), Bindable.of(ExampleWithMap.class), this.handler);
  }

  private Validator getMapValidator() {
    return new Validator() {

      @Override
      public boolean supports(Class<?> clazz) {
        return ExampleWithMap.class == clazz;

      }

      @Override
      public void validate(Object target, Errors errors) {
        ExampleWithMap value = (ExampleWithMap) target;
        value.getItems().forEach((k, v) -> {
          try {
            errors.pushNestedPath("items[" + k + "]");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "number", "NUMBER_ERR");
          }
          finally {
            errors.popNestedPath();
          }
        });
      }

    };
  }

  private BindValidationException bindAndExpectValidationError(Runnable action) {
    try {
      action.run();
    }
    catch (BindException ex) {
      return (BindValidationException) ex.getCause();
    }
    throw new IllegalStateException("Did not throw");
  }

  static class ExampleNonValidatedBean {

    @Min(5)
    private int age;

    int getAge() {
      return this.age;
    }

    void setAge(int age) {
      this.age = age;
    }

  }

  @Validated
  static class ExampleValidatedBean {

    @Min(5)
    private int age;

    int getAge() {
      return this.age;
    }

    void setAge(int age) {
      this.age = age;
    }

  }

  public static class ExampleValidatedBeanSubclass extends ExampleValidatedBean {

    @Min(100)
    private int years;

    ExampleValidatedBeanSubclass() {
      setAge(20);
    }

    public int getYears() {
      return this.years;
    }

    public void setYears(int years) {
      this.years = years;
    }

  }

  @Validated
  static class ExampleValidatedWithNestedBean {

    @Valid
    private ExampleNested nested = new ExampleNested();

    ExampleNested getNested() {
      return this.nested;
    }

    void setNested(ExampleNested nested) {
      this.nested = nested;
    }

  }

  static class ExampleNested {

    private String name;

    @Min(5)
    private int age;

    @NotNull
    private String address;

    String getName() {
      return this.name;
    }

    void setName(String name) {
      this.name = name;
    }

    int getAge() {
      return this.age;
    }

    void setAge(int age) {
      this.age = age;
    }

    String getAddress() {
      return this.address;
    }

    void setAddress(String address) {
      this.address = address;
    }

  }

  @Validated
  static class ExampleCamelCase {

    @Valid
    private InnerProperties inner = new InnerProperties();

    InnerProperties getInner() {
      return this.inner;
    }

    static class InnerProperties {

      @Min(5)
      private int personAge;

      int getPersonAge() {
        return this.personAge;
      }

      void setPersonAge(int personAge) {
        this.personAge = personAge;
      }

    }

  }

  @Validated
  static class ExampleValidatedBeanWithGetterException {

    int getAge() {
      throw new RuntimeException();
    }

  }

  static class ExampleWithMap {

    private Map<String, ExampleMapValue> items = new LinkedHashMap<>();

    Map<String, ExampleMapValue> getItems() {
      return this.items;
    }

  }

  static class ExampleMapValue {

    private String number;

    String getNumber() {
      return this.number;
    }

    void setNumber(String number) {
      this.number = number;
    }

  }

  static class TestHandler extends AbstractBindHandler {

    private Object result;

    TestHandler(Object result) {
      this.result = result;
    }

    @Override
    public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
            Exception error) throws Exception {
      return this.result;
    }

  }

}
