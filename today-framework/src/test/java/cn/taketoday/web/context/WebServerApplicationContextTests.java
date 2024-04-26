/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.context;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.web.server.context.WebServerApplicationContext;

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

}
