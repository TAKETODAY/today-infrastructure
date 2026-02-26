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

package infra.app.rest.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import infra.mock.env.MockEnvironment;
import infra.web.util.UriTemplateHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LocalHostUriTemplateHandler}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 */
class LocalHostUriTemplateHandlerTests {

  @Test
  void createWhenEnvironmentIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new LocalHostUriTemplateHandler(null))
            .withMessageContaining("Environment is required");
  }

  @Test
  void createWhenSchemeIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new LocalHostUriTemplateHandler(new MockEnvironment(), null))
            .withMessageContaining("Scheme is required");
  }

  @Test
  void createWhenHandlerIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new LocalHostUriTemplateHandler(new MockEnvironment(), "http", null))
            .withMessageContaining("Handler is required");
  }

  @Test
  void getRootUriShouldUseLocalServerPort() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("local.server.port", "1234");
    LocalHostUriTemplateHandler handler = new LocalHostUriTemplateHandler(environment);
    assertThat(handler.getRootUri()).isEqualTo("http://localhost:1234");
  }

  @Test
  void getRootUriWhenLocalServerPortMissingShouldUsePort8080() {
    MockEnvironment environment = new MockEnvironment();
    LocalHostUriTemplateHandler handler = new LocalHostUriTemplateHandler(environment);
    assertThat(handler.getRootUri()).isEqualTo("http://localhost:8080");
  }

  @Test
  void getRootUriUsesCustomScheme() {
    MockEnvironment environment = new MockEnvironment();
    LocalHostUriTemplateHandler handler = new LocalHostUriTemplateHandler(environment, "https");
    assertThat(handler.getRootUri()).isEqualTo("https://localhost:8080");
  }

  @Test
  void getRootUriShouldUseContextPath() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("server.mock.context-path", "/foo");
    LocalHostUriTemplateHandler handler = new LocalHostUriTemplateHandler(environment);
    assertThat(handler.getRootUri()).isEqualTo("http://localhost:8080/foo");
  }

  @Test
  void expandShouldUseCustomHandler() {
    MockEnvironment environment = new MockEnvironment();
    UriTemplateHandler uriTemplateHandler = mock(UriTemplateHandler.class);
    Map<String, ?> uriVariables = new HashMap<>();
    URI uri = URI.create("https://www.example.com");
    given(uriTemplateHandler.expand("https://localhost:8080/", uriVariables)).willReturn(uri);
    LocalHostUriTemplateHandler handler = new LocalHostUriTemplateHandler(environment, "https", uriTemplateHandler);
    assertThat(handler.expand("/", uriVariables)).isEqualTo(uri);
    then(uriTemplateHandler).should().expand("https://localhost:8080/", uriVariables);
  }

}
