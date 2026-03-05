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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.web.mock.assertj;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.http.MediaType;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.web.WebAppConfiguration;
import infra.test.web.mock.MockMvc;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;
import infra.web.config.annotation.EnableWebMvc;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link MockMvcTester} that use the methods that
 * integrate with {@link MockMvc} way of building the requests and
 * asserting the responses.
 *
 * @author Stephane Nicoll
 */
@JUnitConfig
@WebAppConfiguration
class MockMvcTesterCompatibilityIntegrationTests {

  private final MockMvcTester mvc;

  MockMvcTesterCompatibilityIntegrationTests(@Autowired ApplicationContext wac) {
    this.mvc = MockMvcTester.from(wac);
  }

  @Test
  void performGet() {
    assertThat(this.mvc.perform(get("/greet"))).hasStatusOk();
  }

  @Test
  void performGetWithInvalidMediaTypeAssertion() {
    infra.test.web.mock.assertj.MvcTestResult result = this.mvc.perform(get("/greet"));
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .withMessageContaining("is compatible with 'application/json'");
  }

  @Test
  void assertHttpStatusCode() {
    assertThat(this.mvc.get().uri("/greet")).matches(status().isOk());
  }

  @Configuration
  @EnableWebMvc
  @Import(TestController.class)
  static class WebConfiguration {
  }

  @RestController
  static class TestController {

    @GetMapping(path = "/greet", produces = "text/plain")
    String greet() {
      return "hello";
    }

    @GetMapping(path = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
    String message() {
      return "{\"message\": \"hello\"}";
    }
  }

}
