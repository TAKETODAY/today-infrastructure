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

package infra.web.context;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.web.server.context.WebServerApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class WebServerApplicationContextTests {

  @Test
  void hasServerNamespaceWhenContextIsNotWebServerApplicationContextReturnsFalse() {
    ApplicationContext context = mock(ApplicationContext.class);
    assertThat(WebServerApplicationContext.hasServerNamespace(context, "test")).isFalse();
  }

  @Test
  void hasServerNamespaceWhenContextIsWebServerApplicationContextAndNamespaceDoesNotMatchReturnsFalse() {
    ApplicationContext context = mock(WebServerApplicationContext.class);
    assertThat(WebServerApplicationContext.hasServerNamespace(context, "test")).isFalse();
  }

  @Test
  void hasServerNamespaceWhenContextIsWebServerApplicationContextAndNamespaceMatchesReturnsTrue() {
    WebServerApplicationContext context = mock(WebServerApplicationContext.class);
    given(context.getServerNamespace()).willReturn("test");
    assertThat(WebServerApplicationContext.hasServerNamespace(context, "test")).isTrue();
  }

  @Test
  void getServerNamespaceWhenContextIsNotWebServerApplicationContextReturnsNull() {
    ApplicationContext context = mock(ApplicationContext.class);
    assertThat(WebServerApplicationContext.getServerNamespace(context)).isNull();
  }

  @Test
  void getServerNamespaceWhenContextIsWebServerApplicationContextAndNamespaceIsNullReturnsNull() {
    WebServerApplicationContext context = mock(WebServerApplicationContext.class);
    given(context.getServerNamespace()).willReturn(null);
    assertThat(WebServerApplicationContext.getServerNamespace(context)).isNull();
  }

  @Test
  void getServerNamespaceWhenContextIsWebServerApplicationContextAndNamespaceIsSetReturnsNamespace() {
    WebServerApplicationContext context = mock(WebServerApplicationContext.class);
    given(context.getServerNamespace()).willReturn("management");
    assertThat(WebServerApplicationContext.getServerNamespace(context)).isEqualTo("management");
  }

  @Test
  void hasServerNamespaceWhenServerNamespaceIsNullReturnsFalse() {
    WebServerApplicationContext context = mock(WebServerApplicationContext.class);
    given(context.getServerNamespace()).willReturn(null);
    assertThat(WebServerApplicationContext.hasServerNamespace(context, "test")).isFalse();
  }

  @Test
  void hasServerNamespaceWhenServerNamespaceDoesNotMatchReturnsFalse() {
    WebServerApplicationContext context = mock(WebServerApplicationContext.class);
    given(context.getServerNamespace()).willReturn("management");
    assertThat(WebServerApplicationContext.hasServerNamespace(context, "test")).isFalse();
  }

  @Test
  void hasServerNamespaceWhenServerNamespaceMatchesReturnsTrue() {
    WebServerApplicationContext context = mock(WebServerApplicationContext.class);
    given(context.getServerNamespace()).willReturn("test");
    assertThat(WebServerApplicationContext.hasServerNamespace(context, "test")).isTrue();
  }

}
