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

package infra.restclient.support;

import org.junit.jupiter.api.Test;

import infra.http.client.ClientHttpRequestFactory;
import infra.restclient.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class RestGatewaySupportTests {

  @Test
  void createsDefaultTemplate() {
    assertThat(new RestGatewaySupport().getRestTemplate()).isNotNull();
  }

  @Test
  void createsTemplateWithGivenRequestFactory() {
    ClientHttpRequestFactory factory = mock(ClientHttpRequestFactory.class);
    assertThat(new RestGatewaySupport(factory).getRestTemplate().getRequestFactory()).isSameAs(factory);
  }

  @Test
  void replacesTemplateAndRejectsNull() {
    RestGatewaySupport gateway = new RestGatewaySupport();
    RestTemplate template = new RestTemplate();
    gateway.setRestTemplate(template);

    assertThat(gateway.getRestTemplate()).isSameAs(template);
    assertThatIllegalArgumentException().isThrownBy(() -> gateway.setRestTemplate(null));
    assertThat(gateway.getRestTemplate()).isSameAs(template);
    assertThatIllegalArgumentException().isThrownBy(() -> new RestGatewaySupport(null));
  }

}
