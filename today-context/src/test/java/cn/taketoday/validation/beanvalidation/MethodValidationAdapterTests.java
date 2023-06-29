/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.validation.beanvalidation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import cn.taketoday.context.MessageSourceResolvable;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.validation.FieldError;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/15 22:44
 */
class MethodValidationAdapterTests {

  private static final Person faustino1234 = new Person("Faustino1234");

  private static final Person cayetana6789 = new Person("Cayetana6789");

  private final MethodValidationAdapter validationAdapter = new MethodValidationAdapter();

  private final Locale originalLocale = Locale.getDefault();

  @BeforeEach
  void setDefaultLocaleToEnglish() {
    Locale.setDefault(Locale.ENGLISH);
  }

  @AfterEach
  void resetDefaultLocale() {
    Locale.setDefault(this.originalLocale);
  }

  @Test
  void validateArguments() {
    MyService target = new MyService();
    Method method = getMethod(target, "addStudent");

    validateArguments(target, method, new Object[] { faustino1234, cayetana6789, 3 }, ex -> {

      assertThat(ex.getConstraintViolations()).hasSize(3);
      assertThat(ex.getAllValidationResults()).hasSize(3);

      assertBeanResult(ex.getBeanResults().get(0), 0, "student", faustino1234, List.of(
              """
                      Field error in object 'student' on field 'name': rejected value [Faustino1234]; \
                      codes [Size.student.name,Size.name,Size.java.lang.String,Size]; \
                      arguments [cn.taketoday.context.support.DefaultMessageSourceResolvable: \
                      codes [student.name,name]; arguments []; default message [name],10,1]; \
                      default message [size must be between 1 and 10]"""));

      assertBeanResult(ex.getBeanResults().get(1), 1, "guardian", cayetana6789, List.of(
              """
                      Field error in object 'guardian' on field 'name': rejected value [Cayetana6789]; \
                      codes [Size.guardian.name,Size.name,Size.java.lang.String,Size]; \
                      arguments [cn.taketoday.context.support.DefaultMessageSourceResolvable: \
                      codes [guardian.name,name]; arguments []; default message [name],10,1]; \
                      default message [size must be between 1 and 10]"""));

      assertValueResult(ex.getValueResults().get(0), 2, 3, List.of(
              """
                      cn.taketoday.context.support.DefaultMessageSourceResolvable: \
                      codes [Max.myService#addStudent.degrees,Max.degrees,Max.int,Max]; \
                      arguments [cn.taketoday.context.support.DefaultMessageSourceResolvable: \
                      codes [myService#addStudent.degrees,degrees]; arguments []; default message [degrees],2]; \
                      default message [must be less than or equal to 2]"""
      ));
    });
  }

  @Test
  void validateArgumentWithCustomObjectName() {
    MyService target = new MyService();
    Method method = getMethod(target, "addStudent");

    this.validationAdapter.setBindingResultNameResolver((parameter, value) -> "studentToAdd");

    validateArguments(target, method, new Object[] { faustino1234, new Person("Joe"), 1 }, ex -> {

      assertThat(ex.getConstraintViolations()).hasSize(1);
      assertThat(ex.getAllValidationResults()).hasSize(1);

      assertBeanResult(ex.getBeanResults().get(0), 0, "studentToAdd", faustino1234, List.of(
              """
                      Field error in object 'studentToAdd' on field 'name': rejected value [Faustino1234]; \
                      codes [Size.studentToAdd.name,Size.name,Size.java.lang.String,Size]; \
                      arguments [cn.taketoday.context.support.DefaultMessageSourceResolvable: \
                      codes [studentToAdd.name,name]; arguments []; default message [name],10,1]; \
                      default message [size must be between 1 and 10]"""));
    });
  }

  @Test
  void validateReturnValue() {
    MyService target = new MyService();

    validateReturnValue(target, getMethod(target, "getIntValue"), 4, ex -> {

      assertThat(ex.getConstraintViolations()).hasSize(1);
      assertThat(ex.getAllValidationResults()).hasSize(1);

      assertValueResult(ex.getValueResults().get(0), -1, 4, List.of(
              """
                      cn.taketoday.context.support.DefaultMessageSourceResolvable: \
                      codes [Min.myService#getIntValue,Min,Min.int]; \
                      arguments [cn.taketoday.context.support.DefaultMessageSourceResolvable: \
                      codes [myService#getIntValue]; arguments []; default message [],5]; \
                      default message [must be greater than or equal to 5]"""
      ));
    });
  }

  @Test
  void validateReturnValueBean() {
    MyService target = new MyService();

    validateReturnValue(target, getMethod(target, "getPerson"), faustino1234, ex -> {

      assertThat(ex.getConstraintViolations()).hasSize(1);
      assertThat(ex.getAllValidationResults()).hasSize(1);

      assertBeanResult(ex.getBeanResults().get(0), -1, "person", faustino1234, List.of(
              """
                      Field error in object 'person' on field 'name': rejected value [Faustino1234]; \
                      codes [Size.person.name,Size.name,Size.java.lang.String,Size]; \
                      arguments [cn.taketoday.context.support.DefaultMessageSourceResolvable: \
                      codes [person.name,name]; arguments []; default message [name],10,1]; \
                      default message [size must be between 1 and 10]"""));
    });
  }

  @Test
  void validateListArgument() {
    MyService target = new MyService();
    Method method = getMethod(target, "addPeople");

    validateArguments(target, method, new Object[] { List.of(faustino1234, cayetana6789) }, ex -> {

      assertThat(ex.getConstraintViolations()).hasSize(2);
      assertThat(ex.getAllValidationResults()).hasSize(2);

      int paramIndex = 0;
      String objectName = "people";
      List<ParameterErrors> results = ex.getBeanResults();

      assertBeanResult(results.get(0), paramIndex, objectName, faustino1234, List.of(
              """
                      Field error in object 'people' on field 'name': rejected value [Faustino1234]; \
                      codes [Size.people.name,Size.name,Size.java.lang.String,Size]; \
                      arguments [cn.taketoday.context.support.DefaultMessageSourceResolvable: \
                      codes [people.name,name]; arguments []; default message [name],10,1]; \
                      default message [size must be between 1 and 10]"""));

      assertBeanResult(results.get(1), paramIndex, objectName, cayetana6789, List.of(
              """
                      Field error in object 'people' on field 'name': rejected value [Cayetana6789]; \
                      codes [Size.people.name,Size.name,Size.java.lang.String,Size]; \
                      arguments [cn.taketoday.context.support.DefaultMessageSourceResolvable: \
                      codes [people.name,name]; arguments []; default message [name],10,1]; \
                      default message [size must be between 1 and 10]"""));
    });
  }

  private void validateArguments(
          Object target, Method method, Object[] arguments, Consumer<MethodValidationResult> assertions) {

    assertions.accept(
            this.validationAdapter.validateMethodArguments(target, method, null, arguments, new Class<?>[0]));
  }

  private void validateReturnValue(Object target, Method method,
          @Nullable Object returnValue, Consumer<MethodValidationResult> assertions) {

    assertions.accept(
            this.validationAdapter.validateMethodReturnValue(target, method, null, returnValue, new Class<?>[0]));
  }

  private static void assertBeanResult(ParameterErrors errors,
          int parameterIndex, String objectName, Object argument, List<String> fieldErrors) {

    assertThat(errors.getMethodParameter().getParameterIndex()).isEqualTo(parameterIndex);
    assertThat(errors.getObjectName()).isEqualTo(objectName);
    assertThat(errors.getArgument()).isSameAs(argument);

    assertThat(errors.getFieldErrors())
            .extracting(FieldError::toString)
            .containsExactlyInAnyOrderElementsOf(fieldErrors);
  }

  private static void assertValueResult(ParameterValidationResult result,
          int parameterIndex, Object argument, List<String> errors) {

    assertThat(result.getMethodParameter().getParameterIndex()).isEqualTo(parameterIndex);
    assertThat(result.getArgument()).isEqualTo(argument);
    assertThat(result.getResolvableErrors())
            .extracting(MessageSourceResolvable::toString)
            .containsExactlyInAnyOrderElementsOf(errors);
  }

  private static Method getMethod(Object target, String methodName) {
    return ReflectionUtils.getMethod(target.getClass(), methodName, (Class<?>[]) null);
  }

  @SuppressWarnings("unused")
  private static class MyService {

    public void addStudent(@Valid Person student, @Valid Person guardian, @Max(2) int degrees) {
    }

    @Min(5)
    public int getIntValue() {
      throw new UnsupportedOperationException();
    }

    @Valid
    public Person getPerson() {
      throw new UnsupportedOperationException();
    }

    public void addPeople(@Valid List<Person> people) {
    }

  }

  @SuppressWarnings("unused")
  private record Person(@Size(min = 1, max = 10) String name) {
  }

}