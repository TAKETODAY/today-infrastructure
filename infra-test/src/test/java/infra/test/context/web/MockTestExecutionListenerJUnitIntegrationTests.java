/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.context.web;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.mock.web.HttpMockRequestImpl;
import infra.test.context.junit.jupiter.web.JUnitWebConfig;
import infra.web.RequestContextHolder;
import infra.web.mock.MockUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit-based integration tests for {@link MockTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitWebConfig
class MockTestExecutionListenerJUnitIntegrationTests {

  @Configuration
  static class Config {
    /* no beans required for this test */
  }

  @Autowired
  private HttpMockRequestImpl mockRequest;

  /**
   * @see #ensureMocksAreReinjectedBetweenTests_2
   */
  @Test
  void ensureMocksAreReinjectedBetweenTests_1() {
    assertInjectedRequestEqualsRequestInRequestContextHolder();
  }

  /**
   * @see #ensureMocksAreReinjectedBetweenTests_1
   */
  @Test
  void ensureMocksAreReinjectedBetweenTests_2() {
    assertInjectedRequestEqualsRequestInRequestContextHolder();
  }

  private void assertInjectedRequestEqualsRequestInRequestContextHolder() {
    assertThat(MockUtils.getMockRequest(RequestContextHolder.get()))
            .as("Injected ServletRequest must be stored in the RequestContextHolder")
            .isEqualTo(mockRequest);
  }

}
