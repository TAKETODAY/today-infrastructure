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
import cn.taketoday.framework.test.mock.mockito.example.ExampleService;
import cn.taketoday.framework.test.mock.mockito.example.ExampleServiceCaller;
import cn.taketoday.framework.test.mock.mockito.example.RealExampleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpyDefinition}.
 *
 * @author Phillip Webb
 */
class SpyDefinitionTests {

  private static final ResolvableType REAL_SERVICE_TYPE = ResolvableType.forClass(RealExampleService.class);

  @Test
  void classToSpyMustNotBeNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> new SpyDefinition(null, null, null, true, null))
            .withMessageContaining("TypeToSpy must not be null");
  }

  @Test
  void createWithDefaults() {
    SpyDefinition definition = new SpyDefinition(null, REAL_SERVICE_TYPE, null, true, null);
    assertThat(definition.getName()).isNull();
    assertThat(definition.getTypeToSpy()).isEqualTo(REAL_SERVICE_TYPE);
    assertThat(definition.getReset()).isEqualTo(MockReset.AFTER);
    assertThat(definition.isProxyTargetAware()).isTrue();
    assertThat(definition.getQualifier()).isNull();
  }

  @Test
  void createExplicit() {
    QualifierDefinition qualifier = mock(QualifierDefinition.class);
    SpyDefinition definition = new SpyDefinition("name", REAL_SERVICE_TYPE, MockReset.BEFORE, false, qualifier);
    assertThat(definition.getName()).isEqualTo("name");
    assertThat(definition.getTypeToSpy()).isEqualTo(REAL_SERVICE_TYPE);
    assertThat(definition.getReset()).isEqualTo(MockReset.BEFORE);
    assertThat(definition.isProxyTargetAware()).isFalse();
    assertThat(definition.getQualifier()).isEqualTo(qualifier);
  }

  @Test
  void createSpy() {
    SpyDefinition definition = new SpyDefinition("name", REAL_SERVICE_TYPE, MockReset.BEFORE, true, null);
    RealExampleService spy = definition.createSpy(new RealExampleService("hello"));
    MockCreationSettings<?> settings = Mockito.mockingDetails(spy).getMockCreationSettings();
    assertThat(spy).isInstanceOf(ExampleService.class);
    assertThat(settings.getMockName().toString()).isEqualTo("name");
    assertThat(settings.getDefaultAnswer()).isEqualTo(Answers.CALLS_REAL_METHODS);
    assertThat(MockReset.get(spy)).isEqualTo(MockReset.BEFORE);
  }

  @Test
  void createSpyWhenNullInstanceShouldThrowException() {
    SpyDefinition definition = new SpyDefinition("name", REAL_SERVICE_TYPE, MockReset.BEFORE, true, null);
    assertThatIllegalArgumentException().isThrownBy(() -> definition.createSpy(null))
            .withMessageContaining("Instance must not be null");
  }

  @Test
  void createSpyWhenWrongInstanceShouldThrowException() {
    SpyDefinition definition = new SpyDefinition("name", REAL_SERVICE_TYPE, MockReset.BEFORE, true, null);
    assertThatIllegalArgumentException().isThrownBy(() -> definition.createSpy(new ExampleServiceCaller(null)))
            .withMessageContaining("must be an instance of");
  }

  @Test
  void createSpyTwice() {
    SpyDefinition definition = new SpyDefinition("name", REAL_SERVICE_TYPE, MockReset.BEFORE, true, null);
    Object instance = new RealExampleService("hello");
    instance = definition.createSpy(instance);
    definition.createSpy(instance);
  }

}
