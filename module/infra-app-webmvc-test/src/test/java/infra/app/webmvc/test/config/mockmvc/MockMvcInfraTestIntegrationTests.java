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
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.client.RestTestClient;
import infra.test.web.mock.client.RestTestClient.ResponseSpec;
import infra.test.web.mock.client.assertj.RestTestClientResponse;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfraTest @InfraTest} with
 * {@link AutoConfigureMockMvc @AutoConfigureMockMvc} (i.e. full integration test).
 * <p>
 * This uses the regular {@link MockMvc} (Hamcrest integration).
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
@InfraTest
@AutoConfigureMockMvc(print = MockMvcPrint.SYSTEM_ERR, printOnlyOnFailure = false)
@AutoConfigureRestTestClient
@ExtendWith(OutputCaptureExtension.class)
class MockMvcInfraTestIntegrationTests {

  @MockitoBean
  private ExampleMockableService service;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private MockMvc mvc;

  @Test
  void shouldFindController1(CapturedOutput output) throws Exception {
    this.mvc.perform(get("/one")).andExpect(content().string("one")).andExpect(status().isOk());
    assertThat(output).contains("Request URI = /one");
  }

  @Test
  void shouldFindController2() throws Exception {
    this.mvc.perform(get("/two")).andExpect(content().string("hellotwo")).andExpect(status().isOk());
  }

  @Test
  void shouldFindControllerAdvice() throws Exception {
    this.mvc.perform(get("/error")).andExpect(content().string("recovered")).andExpect(status().isOk());
  }

  @Test
  void shouldHaveRealService() {
    assertThat(this.applicationContext.getBean(ExampleRealService.class)).isNotNull();
  }

  @Test
  void shouldTestWithRestTestClient(@Autowired RestTestClient restTestClient) {
    ResponseSpec spec = restTestClient.get().uri("/one").exchange();
    assertThat(RestTestClientResponse.from(spec)).hasStatusOk().bodyText().isEqualTo("one");
  }

  @Test
  void shouldNotFailIfFormattingValueThrowsException(CapturedOutput output) throws Exception {
    this.mvc.perform(get("/formatting")).andExpect(content().string("formatting")).andExpect(status().isOk());
    assertThat(output).contains(
            "Session Attrs = << Exception 'java.lang.IllegalStateException: Formatting failed' occurred while formatting >>");
  }

}
