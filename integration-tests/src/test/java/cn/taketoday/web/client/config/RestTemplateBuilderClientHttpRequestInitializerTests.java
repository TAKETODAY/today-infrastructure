/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.web.client.config;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.mock.http.client.MockClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestTemplateBuilderClientHttpRequestInitializer}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 * @author Phillip Webb
 */
class RestTemplateBuilderClientHttpRequestInitializerTests {

  private final MockClientHttpRequest request = new MockClientHttpRequest();

  @Test
  void createRequestWhenHasBasicAuthAndNoAuthHeaderAddsHeader() {
    new RestTemplateBuilderClientHttpRequestInitializer(new BasicAuthentication("spring", "boot", null),
            Collections.emptyMap(), Collections.emptySet()).initialize(this.request);
    assertThat(this.request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Basic c3ByaW5nOmJvb3Q=");
  }

  @Test
  void createRequestWhenHasBasicAuthAndExistingAuthHeaderDoesNotAddHeader() {
    this.request.getHeaders().setBasicAuth("boot", "spring");
    new RestTemplateBuilderClientHttpRequestInitializer(new BasicAuthentication("spring", "boot", null),
            Collections.emptyMap(), Collections.emptySet()).initialize(this.request);
    assertThat(this.request.getHeaders().get(HttpHeaders.AUTHORIZATION)).doesNotContain("Basic c3ByaW5nOmJvb3Q=");
  }

  @Test
  void createRequestWhenHasDefaultHeadersAddsMissing() {
    this.request.getHeaders().add("one", "existing");
    Map<String, List<String>> defaultHeaders = new LinkedHashMap<>();
    defaultHeaders.put("one", Collections.singletonList("1"));
    defaultHeaders.put("two", Arrays.asList("2", "3"));
    defaultHeaders.put("three", Collections.singletonList("4"));
    new RestTemplateBuilderClientHttpRequestInitializer(null, defaultHeaders, Collections.emptySet())
            .initialize(this.request);
    assertThat(this.request.getHeaders().get("one")).containsExactly("existing");
    assertThat(this.request.getHeaders().get("two")).containsExactly("2", "3");
    assertThat(this.request.getHeaders().get("three")).containsExactly("4");
  }

  @Test
  @SuppressWarnings("unchecked")
  void createRequestWhenHasRequestCustomizersAppliesThemInOrder() {
    Set<RestTemplateRequestCustomizer<?>> customizers = new LinkedHashSet<>();
    customizers.add(mock(RestTemplateRequestCustomizer.class));
    customizers.add(mock(RestTemplateRequestCustomizer.class));
    customizers.add(mock(RestTemplateRequestCustomizer.class));
    new RestTemplateBuilderClientHttpRequestInitializer(null, Collections.emptyMap(), customizers)
            .initialize(this.request);
    InOrder inOrder = inOrder(customizers.toArray());
    for (RestTemplateRequestCustomizer<?> customizer : customizers) {
      inOrder.verify((RestTemplateRequestCustomizer<ClientHttpRequest>) customizer).customize(this.request);
    }
  }

}
