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

package infra.app.test.mock.mockito;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.app.test.mock.mockito.example.ExampleService;
import infra.app.test.mock.mockito.example.ExampleServiceCaller;
import infra.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockitoContextCustomizer}.
 *
 * @author Phillip Webb
 */
class MockitoContextCustomizerTests {

  private static final Set<MockDefinition> NO_DEFINITIONS = Collections.emptySet();

  @Test
  void hashCodeAndEquals() {
    MockDefinition d1 = createTestMockDefinition(ExampleService.class);
    MockDefinition d2 = createTestMockDefinition(ExampleServiceCaller.class);
    MockitoContextCustomizer c1 = new MockitoContextCustomizer(NO_DEFINITIONS);
    MockitoContextCustomizer c2 = new MockitoContextCustomizer(new LinkedHashSet<>(Arrays.asList(d1, d2)));
    MockitoContextCustomizer c3 = new MockitoContextCustomizer(new LinkedHashSet<>(Arrays.asList(d2, d1)));
    assertThat(c2.hashCode()).isEqualTo(c3.hashCode());
    assertThat(c1).isEqualTo(c1).isNotEqualTo(c2);
    assertThat(c2).isEqualTo(c2).isEqualTo(c3).isNotEqualTo(c1);
  }

  private MockDefinition createTestMockDefinition(Class<?> typeToMock) {
    return new MockDefinition(null, ResolvableType.forClass(typeToMock), null, null, false, null, null);
  }

}
