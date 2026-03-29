/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.beans.factory.annotation.Autowired;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MockitoBean @MockitoBean} where mocks are created
 * for nonexistent beans for a supertype and subtype of that supertype.
 *
 * <p>This test class is designed to reproduce scenarios that previously failed
 * along the lines of the following.
 *
 * <p>BeanNotOfRequiredTypeException: Bean named 'Subtype#0' is expected to be
 * of type 'Subtype' but was actually of type 'Supertype$MockitoMock$XHb7Aspo'
 *
 * @author Sam Brannen
 * @since 5.0
 */
@JUnitConfig
public class MockitoBeanSuperAndSubtypeIntegrationTests {

  // The declaration order of the following fields is intentional, and prior
  // to fixing gh-34025 this test class consistently failed on JDK 17.

  @MockitoBean
  Subtype subtype;

  @MockitoBean
  Supertype supertype;

  @Autowired
  List<Supertype> supertypes;

  @Test
  void bothMocksShouldHaveBeenCreated() {
    assertThat(supertype).isNotSameAs(subtype);
    assertThat(supertypes).hasSize(2);
  }

  interface Supertype {
  }

  interface Subtype extends Supertype {
  }

}
