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

package infra.restclient.test.config;

import org.junit.jupiter.api.Test;

import infra.restclient.test.MockServerRestTemplateCustomizer;

import infra.beans.factory.annotation.Autowired;
import infra.http.MediaType;
import infra.test.web.client.MockRestServiceServer;

import static infra.test.web.client.match.MockRestRequestMatchers.requestTo;
import static infra.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link RestClientTest @RestClientTest} with two {@code RestTemplate} clients.
 *
 * @author Phillip Webb
 */
@RestClientTest({ ExampleRestTemplateService.class, AnotherExampleRestTemplateService.class })
class RestClientTestRestTemplateTwoComponentsIntegrationTests {

  @Autowired
  private ExampleRestTemplateService client1;

  @Autowired
  private AnotherExampleRestTemplateService client2;

  @Autowired
  private MockServerRestTemplateCustomizer customizer;

  @Autowired
  private MockRestServiceServer server;

  @Test
  void serverShouldNotWork() {
    assertThatIllegalStateException()
            .isThrownBy(
                    () -> this.server.expect(requestTo("/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML)))
            .withMessageContaining("Unable to use auto-configured");
  }

  @Test
  void client1RestCallViaCustomizer() {
    MockRestServiceServer server = this.customizer.getServer(this.client1.getRestTemplate());
    assertThat(server).isNotNull();
    server.expect(requestTo("/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
    assertThat(this.client1.test()).isEqualTo("hello");
  }

  @Test
  void client2RestCallViaCustomizer() {
    MockRestServiceServer server = this.customizer.getServer(this.client2.getRestTemplate());
    assertThat(server).isNotNull();
    server.expect(requestTo("/test")).andRespond(withSuccess("there", MediaType.TEXT_HTML));
    assertThat(this.client2.test()).isEqualTo("there");
  }

}
