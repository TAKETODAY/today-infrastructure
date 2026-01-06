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
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JacksonTester}.
 *
 * @author Phillip Webb
 */
class JacksonTesterTests extends AbstractJsonMarshalTesterTests {

  @Test
  @SuppressWarnings("NullAway")
  void initFieldsWhenTestIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> JacksonTester.initFields(null, new JsonMapper()))
            .withMessageContaining("'testInstance' is required");
  }

  @Test
  @SuppressWarnings("NullAway")
  void initFieldsWhenMarshallerIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> JacksonTester.initFields(new InitFieldsTestClass(), (JsonMapper) null))
            .withMessageContaining("'marshaller' is required");
  }

  @Test
  void initFieldsShouldSetNullFields() {
    InitFieldsTestClass test = new InitFieldsTestClass();
    assertThat(test.test).isNull();
    assertThat(test.base).isNull();
    JacksonTester.initFields(test, new JsonMapper());
    assertThat(test.test).isNotNull();
    assertThat(test.base).isNotNull();
    ResolvableType type = test.test.getType();
    assertThat(type).isNotNull();
    assertThat(type.resolve()).isEqualTo(List.class);
    assertThat(type.resolveGeneric()).isEqualTo(ExampleObject.class);
  }

  @Override
  protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type) {
    return new JacksonTester<>(resourceLoadClass, type, new JsonMapper());
  }

  abstract static class InitFieldsBaseClass {

    public @Nullable JacksonTester<ExampleObject> base;

    public JacksonTester<ExampleObject> baseSet = new JacksonTester<>(InitFieldsBaseClass.class,
            ResolvableType.forClass(ExampleObject.class), new JsonMapper());

  }

  static class InitFieldsTestClass extends InitFieldsBaseClass {

    public @Nullable JacksonTester<List<ExampleObject>> test;

    public JacksonTester<ExampleObject> testSet = new JacksonTester<>(InitFieldsBaseClass.class,
            ResolvableType.forClass(ExampleObject.class), new JsonMapper());

  }

}
