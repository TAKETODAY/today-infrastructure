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

package infra.app.test.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link GsonTester}.
 *
 * @author Phillip Webb
 */
class GsonTesterTests extends AbstractJsonMarshalTesterTests {

  @Test
  void initFieldsWhenTestIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> GsonTester.initFields(null, new GsonBuilder().create()))
            .withMessageContaining("TestInstance is required");
  }

  @Test
  void initFieldsWhenMarshallerIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> GsonTester.initFields(new InitFieldsTestClass(), (Gson) null))
            .withMessageContaining("Marshaller is required");
  }

  @Test
  void initFieldsShouldSetNullFields() {
    InitFieldsTestClass test = new InitFieldsTestClass();
    assertThat(test.test).isNull();
    assertThat(test.base).isNull();
    GsonTester.initFields(test, new GsonBuilder().create());
    assertThat(test.test).isNotNull();
    assertThat(test.base).isNotNull();
    assertThat(test.test.getType().resolve()).isEqualTo(List.class);
    assertThat(test.test.getType().resolveGeneric()).isEqualTo(ExampleObject.class);
  }

  @Override
  protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type) {
    return new GsonTester<>(resourceLoadClass, type, new GsonBuilder().create());
  }

  abstract static class InitFieldsBaseClass {

    public GsonTester<ExampleObject> base;

    public GsonTester<ExampleObject> baseSet = new GsonTester<>(InitFieldsBaseClass.class,
            ResolvableType.forClass(ExampleObject.class), new GsonBuilder().create());

  }

  static class InitFieldsTestClass extends InitFieldsBaseClass {

    public GsonTester<List<ExampleObject>> test;

    public GsonTester<ExampleObject> testSet = new GsonTester<>(InitFieldsBaseClass.class,
            ResolvableType.forClass(ExampleObject.class), new GsonBuilder().create());

  }

}
