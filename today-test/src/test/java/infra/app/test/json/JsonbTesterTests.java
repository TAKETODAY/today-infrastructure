/*
 * Copyright 2017 - 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import infra.core.ResolvableType;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JsonbTester}.
 *
 * @author Eddú Meléndez
 */
class JsonbTesterTests extends AbstractJsonMarshalTesterTests {

  @Test
  @SuppressWarnings("NullAway")
    // Test null check
  void initFieldsWhenTestIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> JsonbTester.initFields(null, JsonbBuilder.create()))
            .withMessageContaining("'testInstance' is required");
  }

  @Test
  @SuppressWarnings("NullAway")
    // Test null check
  void initFieldsWhenMarshallerIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> JsonbTester.initFields(new InitFieldsTestClass(), (Jsonb) null))
            .withMessageContaining("'marshaller' is required");
  }

  @Test
  void initFieldsShouldSetNullFields() {
    InitFieldsTestClass test = new InitFieldsTestClass();
    assertThat(test.test).isNull();
    assertThat(test.base).isNull();
    JsonbTester.initFields(test, JsonbBuilder.create());
    assertThat(test.test).isNotNull();
    assertThat(test.base).isNotNull();
    ResolvableType type = test.test.getType();
    assertThat(type).isNotNull();
    assertThat(type.resolve()).isEqualTo(List.class);
    assertThat(type.resolveGeneric()).isEqualTo(ExampleObject.class);
  }

  @Override
  protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type) {
    return new JsonbTester<>(resourceLoadClass, type, JsonbBuilder.create());
  }

  abstract static class InitFieldsBaseClass {

    public @Nullable JsonbTester<ExampleObject> base;

    public JsonbTester<ExampleObject> baseSet = new JsonbTester<>(InitFieldsBaseClass.class,
            ResolvableType.forClass(ExampleObject.class), JsonbBuilder.create());

  }

  static class InitFieldsTestClass extends InitFieldsBaseClass {

    public @Nullable JsonbTester<List<ExampleObject>> test;

    public JsonbTester<ExampleObject> testSet = new JsonbTester<>(InitFieldsBaseClass.class,
            ResolvableType.forClass(ExampleObject.class), JsonbBuilder.create());

  }

}
