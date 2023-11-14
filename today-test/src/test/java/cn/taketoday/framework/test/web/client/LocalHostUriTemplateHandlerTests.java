/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.test.web.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.mock.env.MockEnvironment;
import cn.taketoday.web.util.UriTemplateHandler;

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
    environment.setProperty("server.servlet.context-path", "/foo");
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
