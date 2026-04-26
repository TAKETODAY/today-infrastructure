/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.webmvc.test.config.mockmvc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.app.resttestclient.config.AutoConfigureRestTestClient;
import infra.app.test.context.InfraTest;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.app.webmvc.test.config.AutoConfigureMockMvc;
import infra.app.webmvc.test.config.MockMvcPrint;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.test.web.mock.client.RestTestClient;
import infra.test.web.mock.client.assertj.RestTestClientResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfraTest @InfraTest} with
 * {@link AutoConfigureMockMvc @AutoConfigureMockMvc} (i.e. full integration test).
 * <p>
 * This uses {@link MockMvcTester} (AssertJ integration).
 *
 * @author Stephane Nicoll
 */
@InfraTest
@AutoConfigureMockMvc(print = MockMvcPrint.SYSTEM_ERR, printOnlyOnFailure = false)
@AutoConfigureRestTestClient
@ExtendWith(OutputCaptureExtension.class)
class MockMvcTesterInfraTestIntegrationTests {

  @MockitoBean
  private ExampleMockableService service;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private MockMvcTester mvc;

  @Test
  void shouldFindController1(CapturedOutput output) {
    assertThat(this.mvc.get().uri("/one")).hasStatusOk().hasBodyTextEqualTo("one");
    assertThat(output).contains("Request URI = /one");
  }

  @Test
  void shouldFindController2() {
    assertThat(this.mvc.get().uri("/two")).hasStatusOk().hasBodyTextEqualTo("hellotwo");
  }

  @Test
  void shouldFindControllerAdvice() {
    assertThat(this.mvc.get().uri("/error")).hasStatusOk().hasBodyTextEqualTo("recovered");
  }

  @Test
  void shouldHaveRealService() {
    assertThat(this.applicationContext.getBean(ExampleRealService.class)).isNotNull();
  }

  @Test
  void shouldTestWithRestTestClient(@Autowired RestTestClient restTestClient) {
    RestTestClient.ResponseSpec spec = restTestClient.get().uri("/one").exchange();
    assertThat(RestTestClientResponse.from(spec)).hasStatusOk().bodyText().isEqualTo("one");
  }

  @Test
  void shouldNotFailIfFormattingValueThrowsException(CapturedOutput output) {
    assertThat(this.mvc.get().uri("/formatting")).hasStatusOk().hasBodyTextEqualTo("formatting");
    assertThat(output).contains(
            "Session Attrs = << Exception 'java.lang.IllegalStateException: Formatting failed' occurred while formatting >>");
  }

}
