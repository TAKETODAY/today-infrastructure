/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.test.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.ResolvableType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JacksonTester}.
 *
 * @author Phillip Webb
 */
class JacksonTesterTests extends AbstractJsonMarshalTesterTests {

  @Test
  void initFieldsWhenTestIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> JacksonTester.initFields(null, new ObjectMapper()))
            .withMessageContaining("TestInstance is required");
  }

  @Test
  void initFieldsWhenMarshallerIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> JacksonTester.initFields(new InitFieldsTestClass(), (ObjectMapper) null))
            .withMessageContaining("Marshaller is required");
  }

  @Test
  void initFieldsShouldSetNullFields() {
    InitFieldsTestClass test = new InitFieldsTestClass();
    assertThat(test.test).isNull();
    assertThat(test.base).isNull();
    JacksonTester.initFields(test, new ObjectMapper());
    assertThat(test.test).isNotNull();
    assertThat(test.base).isNotNull();
    assertThat(test.test.getType().resolve()).isEqualTo(List.class);
    assertThat(test.test.getType().resolveGeneric()).isEqualTo(ExampleObject.class);
  }

  @Override
  protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type) {
    return new JacksonTester<>(resourceLoadClass, type, new ObjectMapper());
  }

  abstract static class InitFieldsBaseClass {

    public JacksonTester<ExampleObject> base;

    public JacksonTester<ExampleObject> baseSet = new JacksonTester<>(InitFieldsBaseClass.class,
            ResolvableType.forClass(ExampleObject.class), new ObjectMapper());

  }

  static class InitFieldsTestClass extends InitFieldsBaseClass {

    public JacksonTester<List<ExampleObject>> test;

    public JacksonTester<ExampleObject> testSet = new JacksonTester<>(InitFieldsBaseClass.class,
            ResolvableType.forClass(ExampleObject.class), new ObjectMapper());

  }

}
