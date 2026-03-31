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

package infra.test.context.bean.override.mockito.constructor;

import org.junit.jupiter.api.Test;

import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.mockito.MockitoAssertions.assertIsMock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link MockitoBean @MockitoBean} that use by-type lookup
 * on constructor parameters in a Java record.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@JUnitConfig
record MockitoBeanByTypeLookupForConstructorParametersIntegrationRecordTests(
        @MockitoBean ExampleService exampleService) {

  @Test
  void test() {
    assertIsMock(this.exampleService);

    when(this.exampleService.greeting()).thenReturn("Mocked greeting");

    assertThat(this.exampleService.greeting()).isEqualTo("Mocked greeting");
    verify(this.exampleService, times(1)).greeting();
    verifyNoMoreInteractions(this.exampleService);
  }

}
