/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultUriTemplateHandlerTests {

  private final DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();

  @Test
  void appliesBaseUrlToRelativeTemplatesOnly() {
    handler.setBaseUrl("https://example.test:8443/api");
    assertThat(handler.getBaseUrl()).isEqualTo("https://example.test:8443/api");
    assertThat(handler.expand("/items/{id}", Map.of("id", 7)))
            .isEqualTo(URI.create("https://example.test:8443/api/items/7"));
    assertThat(handler.expand("//cdn.example.test/{id}", 7)).isEqualTo(URI.create("//cdn.example.test/7"));
    assertThat(handler.expand("https://other.test/{id}", 7)).isEqualTo(URI.create("https://other.test/7"));

    handler.setBaseUrl(null);
    assertThat(handler.getBaseUrl()).isNull();
    assertThat(handler.expand("/items/{id}", 7)).isEqualTo(URI.create("/items/7"));
  }

  @ParameterizedTest
  @CsvSource({
          "//example.test, must have a scheme",
          "https:/api, must have a host",
          "https://example.test/?foo=bar, cannot have a query",
          "https://example.test/#section, cannot have a fragment"
  })
  void rejectsInvalidBaseUrls(String baseUrl, String reason) {
    assertThatIllegalArgumentException().isThrownBy(() -> handler.setBaseUrl(baseUrl)).withMessageContaining(reason);
  }

  @Test
  void namedValuesOverrideDefaultsWithoutChangingTheConfiguration() {
    var defaults = new HashMap<>(Map.of("language", "en", "name", "books"));
    handler.setDefaultUriVariables(defaults);
    defaults.clear();

    assertThat(handler.expand("/{language}/{name}", Map.of("name", "records"))).isEqualTo(URI.create("/en/records"));
    assertThat(handler.getDefaultUriVariables()).containsOnlyKeys("language", "name");
    assertThatThrownBy(() -> handler.getDefaultUriVariables().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatIllegalArgumentException().isThrownBy(() -> handler.expand("/{language}/{name}", "fr"));

    handler.setDefaultUriVariables(null);
    assertThat(handler.getDefaultUriVariables()).isEmpty();
    assertThat(handler.expand("/{name}", Map.of("name", "items"))).isEqualTo(URI.create("/items"));
  }

  @ParameterizedTest
  @CsvSource({
          "false, false, /files/a/b;%20c",
          "true, false, /files/a%2Fb;%20c",
          "false, true, /files/a%2Fb%3B%20c",
          "true, true, /files/a%2Fb%3B%20c"
  })
  void encodesNamedAndPositionalVariablesAccordingToConfiguration(boolean parsePath, boolean strict, String expected) {
    handler.setParsePath(parsePath);
    handler.setStrictEncoding(strict);

    assertThat(handler.shouldParsePath()).isEqualTo(parsePath);
    assertThat(handler.isStrictEncoding()).isEqualTo(strict);
    assertThat(handler.expand("/files/{name}", Map.of("name", "a/b; c"))).isEqualTo(URI.create(expected));
    assertThat(handler.expand("/files/{name}", "a/b; c")).isEqualTo(URI.create(expected));
  }

  @Test
  void reportsInvalidUriProducedByStrictExpansion() {
    handler.setStrictEncoding(true);

    assertThatIllegalStateException().isThrownBy(() -> handler.expand("/a b/{name}", "value"))
            .withMessageContaining("Could not create URI object").withCauseInstanceOf(URISyntaxException.class);
  }

  @Test
  void reportsInvalidUriAfterBaseUrlInsertion() {
    handler.setBaseUrl("https://[2001:db8::1]");

    assertThatIllegalArgumentException().isThrownBy(() -> handler.expand("relative"))
            .withMessageContaining("Invalid URL after inserting base URL").withCauseInstanceOf(URISyntaxException.class);
  }

}
