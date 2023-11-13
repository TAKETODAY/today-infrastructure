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

package cn.taketoday.framework.web.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.client.config.RootUriTemplateHandler;
import cn.taketoday.web.util.UriTemplateHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link RootUriTemplateHandler}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class RootUriTemplateHandlerTests {

  private URI uri;

  @Mock
  public UriTemplateHandler delegate;

  public UriTemplateHandler handler;

  @BeforeEach
  void setup() throws URISyntaxException {
    this.uri = new URI("https://example.com/hello");
    this.handler = new RootUriTemplateHandler("https://example.com", this.delegate);
  }

  @Test
  void createWithNullRootUriShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new RootUriTemplateHandler((String) null))
            .withMessageContaining("RootUri is required");
  }

  @Test
  void createWithNullHandlerShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new RootUriTemplateHandler("https://example.com", null))
            .withMessageContaining("Handler is required");
  }

  @Test
  @SuppressWarnings("unchecked")
  void expandMapVariablesShouldPrefixRoot() {
    given(this.delegate.expand(anyString(), any(Map.class))).willReturn(this.uri);
    HashMap<String, Object> uriVariables = new HashMap<>();
    URI expanded = this.handler.expand("/hello", uriVariables);
    then(this.delegate).should().expand("https://example.com/hello", uriVariables);
    assertThat(expanded).isEqualTo(this.uri);
  }

  @Test
  @SuppressWarnings("unchecked")
  void expandMapVariablesWhenPathDoesNotStartWithSlashShouldNotPrefixRoot() {
    given(this.delegate.expand(anyString(), any(Map.class))).willReturn(this.uri);
    HashMap<String, Object> uriVariables = new HashMap<>();
    URI expanded = this.handler.expand("https://spring.io/hello", uriVariables);
    then(this.delegate).should().expand("https://spring.io/hello", uriVariables);
    assertThat(expanded).isEqualTo(this.uri);
  }

  @Test
  void expandArrayVariablesShouldPrefixRoot() {
    given(this.delegate.expand(anyString(), any(Object[].class))).willReturn(this.uri);
    Object[] uriVariables = new Object[0];
    URI expanded = this.handler.expand("/hello", uriVariables);
    then(this.delegate).should().expand("https://example.com/hello", uriVariables);
    assertThat(expanded).isEqualTo(this.uri);
  }

  @Test
  void expandArrayVariablesWhenPathDoesNotStartWithSlashShouldNotPrefixRoot() {
    given(this.delegate.expand(anyString(), any(Object[].class))).willReturn(this.uri);
    Object[] uriVariables = new Object[0];
    URI expanded = this.handler.expand("https://spring.io/hello", uriVariables);
    then(this.delegate).should().expand("https://spring.io/hello", uriVariables);
    assertThat(expanded).isEqualTo(this.uri);
  }

  @Test
  void applyShouldWrapExistingTemplate() {
    given(this.delegate.expand(anyString(), any(Object[].class))).willReturn(this.uri);
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setUriTemplateHandler(this.delegate);
    this.handler = RootUriTemplateHandler.addTo(restTemplate, "https://example.com");
    Object[] uriVariables = new Object[0];
    URI expanded = this.handler.expand("/hello", uriVariables);
    then(this.delegate).should().expand("https://example.com/hello", uriVariables);
    assertThat(expanded).isEqualTo(this.uri);
  }

}
