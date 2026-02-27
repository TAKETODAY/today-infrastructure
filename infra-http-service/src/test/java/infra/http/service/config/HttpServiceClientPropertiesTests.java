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

package infra.http.service.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.context.annotation.Configuration;
import infra.context.properties.EnableConfigurationProperties;
import infra.http.client.HttpRedirects;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpServiceClientProperties}.
 *
 * @author Phillip Webb
 */
class HttpServiceClientPropertiesTests {

  @Test
  void bindProperties() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("http.service-client.c1.base-uri", "https://example.com/olga");
    environment.setProperty("http.service-client.c1.default-header.secure", "very,somewhat");
    environment.setProperty("http.service-client.c1.default-header.test", "true");
    environment.setProperty("http.service-client.c1.redirects", "dont-follow");
    environment.setProperty("http.service-client.c1.connect-timeout", "10s");
    environment.setProperty("http.service-client.c1.read-timeout", "20s");
    environment.setProperty("http.service-client.c1.ssl.bundle", "usual");
    environment.setProperty("http.service-client.c2.base-uri", "https://example.com/rossen");
    environment.setProperty("http.service-client.c3.base-uri", "https://example.com/phil");
    HttpServiceClientProperties properties = HttpServiceClientProperties.bind(environment);
    HttpClientProperties c1 = properties.get("c1");
    assertThat(c1).isNotNull();
    assertThat(c1.baseUri).isEqualTo("https://example.com/olga");
    assertThat(c1.defaultHeader).containsOnly(Map.entry("secure", List.of("very", "somewhat")),
            Map.entry("test", List.of("true")));
    assertThat(c1.redirects).isEqualTo(HttpRedirects.DONT_FOLLOW);
    assertThat(c1.connectTimeout).isEqualTo(Duration.ofSeconds(10));
    assertThat(c1.readTimeout).isEqualTo(Duration.ofSeconds(20));
    assertThat(c1.ssl.bundle).isEqualTo("usual");
    HttpClientProperties c2 = properties.get("c2");
    assertThat(c2).isNotNull();
    assertThat(c2.baseUri).isEqualTo("https://example.com/rossen");
    HttpClientProperties c3 = properties.get("c3");
    assertThat(c3).isNotNull();
    assertThat(c3.baseUri).isEqualTo("https://example.com/phil");
    assertThat(properties.get("c4")).isNull();

  }

  @Test
  void registersHintsForBindingOfHttpClientProperties() {
    RuntimeHints hints = new RuntimeHints();
    new HttpServiceClientProperties.Hints().registerHints(hints, getClass().getClassLoader());
    assertThat(RuntimeHintsPredicates.reflection().onType(HttpClientProperties.class)).accepts(hints);
    assertThat(RuntimeHintsPredicates.reflection().onType(ApiVersionProperties.class)).accepts(hints);
    assertThat(RuntimeHintsPredicates.reflection().onType(ApiVersionProperties.Insert.class)).accepts(hints);
  }

  @Configuration
  @EnableConfigurationProperties(HttpServiceClientProperties.class)
  static class PropertiesConfiguration {

  }

}
