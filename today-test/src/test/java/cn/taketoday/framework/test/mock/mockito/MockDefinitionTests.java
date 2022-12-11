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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.mock.MockCreationSettings;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.framework.test.mock.mockito.example.ExampleExtraInterface;
import cn.taketoday.framework.test.mock.mockito.example.ExampleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MockDefinition}.
 *
 * @author Phillip Webb
 */
class MockDefinitionTests {

  private static final ResolvableType EXAMPLE_SERVICE_TYPE = ResolvableType.fromClass(ExampleService.class);

  @Test
  void classToMockMustNotBeNull() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new MockDefinition(null, null, null, null, false, null, null))
            .withMessageContaining("TypeToMock must not be null");
  }

  @Test
  void createWithDefaults() {
    MockDefinition definition = new MockDefinition(null, EXAMPLE_SERVICE_TYPE, null, null, false, null, null);
    assertThat(definition.getName()).isNull();
    assertThat(definition.getTypeToMock()).isEqualTo(EXAMPLE_SERVICE_TYPE);
    assertThat(definition.getExtraInterfaces()).isEmpty();
    assertThat(definition.getAnswer()).isEqualTo(Answers.RETURNS_DEFAULTS);
    assertThat(definition.isSerializable()).isFalse();
    assertThat(definition.getReset()).isEqualTo(MockReset.AFTER);
    assertThat(definition.getQualifier()).isNull();
  }

  @Test
  void createExplicit() {
    QualifierDefinition qualifier = mock(QualifierDefinition.class);
    MockDefinition definition = new MockDefinition("name", EXAMPLE_SERVICE_TYPE,
            new Class<?>[] { ExampleExtraInterface.class }, Answers.RETURNS_SMART_NULLS, true, MockReset.BEFORE,
            qualifier);
    assertThat(definition.getName()).isEqualTo("name");
    assertThat(definition.getTypeToMock()).isEqualTo(EXAMPLE_SERVICE_TYPE);
    assertThat(definition.getExtraInterfaces()).containsExactly(ExampleExtraInterface.class);
    assertThat(definition.getAnswer()).isEqualTo(Answers.RETURNS_SMART_NULLS);
    assertThat(definition.isSerializable()).isTrue();
    assertThat(definition.getReset()).isEqualTo(MockReset.BEFORE);
    assertThat(definition.isProxyTargetAware()).isFalse();
    assertThat(definition.getQualifier()).isEqualTo(qualifier);
  }

  @Test
  void createMock() {
    MockDefinition definition = new MockDefinition("name", EXAMPLE_SERVICE_TYPE,
            new Class<?>[] { ExampleExtraInterface.class }, Answers.RETURNS_SMART_NULLS, true, MockReset.BEFORE,
            null);
    ExampleService mock = definition.createMock();
    MockCreationSettings<?> settings = Mockito.mockingDetails(mock).getMockCreationSettings();
    assertThat(mock).isInstanceOf(ExampleService.class);
    assertThat(mock).isInstanceOf(ExampleExtraInterface.class);
    assertThat(settings.getMockName().toString()).isEqualTo("name");
    assertThat(settings.getDefaultAnswer()).isEqualTo(Answers.RETURNS_SMART_NULLS);
    assertThat(settings.isSerializable()).isTrue();
    assertThat(MockReset.get(mock)).isEqualTo(MockReset.BEFORE);
  }

}
