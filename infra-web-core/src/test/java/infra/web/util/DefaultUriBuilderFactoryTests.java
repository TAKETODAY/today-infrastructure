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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import infra.util.LinkedMultiValueMap;

import static infra.web.util.DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultUriBuilderFactoryTests {

  @Test
  void clonesBaseForRelativeTemplatesButDoesNotOverrideAbsoluteTemplates() {
    var factory = new DefaultUriBuilderFactory("https://example.test/api");
    assertThat(factory.hasBaseUri()).isTrue();
    assertThat(factory.expand("/items/{id}", 7)).hasToString("https://example.test/api/items/7");
    assertThat(factory.expand("https://cdn.example.test/{id}", 7)).hasToString("https://cdn.example.test/7");
    assertThat(factory.builder().path("/health").build()).hasToString("https://example.test/api/health");
    assertThat(factory.builder().build()).hasToString("https://example.test/api");
    assertThat(new DefaultUriBuilderFactory(UriComponentsBuilder.forURIString("/api")).builder().build())
            .hasToString("/api");
    assertThat(new DefaultUriBuilderFactory().hasBaseUri()).isFalse();
  }

  @Test
  void explicitWhatWgParserNormalizesHostAndDotSegments() {
    var factory = new DefaultUriBuilderFactory();
    assertThat(factory.getParserType()).isNull();
    assertThat(factory.expand("https://EXAMPLE.test/a/../b", Map.of())).hasToString("https://EXAMPLE.test/a/../b");

    factory.setParserType(UriComponentsBuilder.ParserType.WHAT_WG);

    assertThat(factory.getParserType()).isEqualTo(UriComponentsBuilder.ParserType.WHAT_WG);
    assertThat(factory.expand("https://EXAMPLE.test/a/../b", Map.of())).hasToString("https://example.test/b");
  }

  @ParameterizedTest
  @CsvSource({
          "TEMPLATE_AND_VALUES, /files/a%2Fb%3Bc/",
          "VALUES_ONLY, /files/a%2Fb%3Bc/",
          "URI_COMPONENT, /files/a%2Fb;c/",
          "NONE, /files/a/b;c/"
  })
  void appliesTheSameEncodingModeToNamedAndPositionalExpansion(DefaultUriBuilderFactory.EncodingMode mode, String expected) {
    var factory = new DefaultUriBuilderFactory();
    factory.setEncodingMode(mode);

    assertThat(factory.getEncodingMode()).isEqualTo(mode);
    assertThat(factory.expand("/files/{name}/", Map.of("name", "a/b;c"))).hasToString(expected);
    assertThat(factory.expand("/files/{name}/", "a/b;c")).hasToString(expected);
  }

  @Test
  void canPreserveSlashesWhenEncodingAfterExpansion() {
    var factory = new DefaultUriBuilderFactory();
    factory.setEncodingMode(URI_COMPONENT);
    assertThat(factory.shouldParsePath()).isTrue();
    assertThat(factory.expand("/files/{name}", "a/b")).hasToString("/files/a%2Fb");

    factory.setParsePath(false);
    assertThat(factory.shouldParsePath()).isFalse();
    assertThat(factory.expand("/files/{name}", "a/b")).hasToString("/files/a/b");
  }

  @Test
  void mergesAndClearsDefaultsAndAllowsCallerOverrides() {
    var factory = new DefaultUriBuilderFactory();
    assertThat(factory.getDefaultUriVariables()).isEmpty();
    factory.setDefaultUriVariables(null);
    factory.setDefaultUriVariables(Map.of("language", "en"));
    factory.setDefaultUriVariables(Map.of("item", "books"));

    assertThat(factory.getDefaultUriVariables()).containsOnlyKeys("language", "item");
    assertThat(factory.expand("/{language}/{item}", Map.of("language", "fr"))).hasToString("/fr/books");
    assertThat(factory.expand("/{language}/{item}")).hasToString("/en/books");
    assertThat(factory.expand("/{language}/{item}", "es", "records")).hasToString("/es/records");

    factory.setDefaultUriVariables(null);
    assertThat(factory.getDefaultUriVariables()).isEmpty();
    assertThat(factory.expand("/static")).hasToString("/static");
  }

  @Test
  void builderCanReplaceUriComponentsBeforeExpansion() {
    var factory = new DefaultUriBuilderFactory();
    var builder = factory.builder().scheme("https").userInfo("reader").host("example.test")
            .port(8080).port("{port}").path("/discarded").replacePath("/files").pathSegment("{name}")
            .query("old=1").replaceQuery("sort=asc").queryParam("tag", "red").queryParam("size", List.of("m", "l"))
            .queryParamIfPresent("limit", Optional.of(2)).queryParamIfPresent("absent", Optional.empty())
            .fragment("top");

    assertThat(builder.toUriString())
            .isEqualTo("https://reader@example.test:{port}/files/{name}?sort=asc&tag=red&size=m&size=l&limit=2#top");
    var parameters = new LinkedMultiValueMap<String, String>();
    parameters.add("tag", "blue");
    builder.replaceQueryParams(parameters).queryParams(parameters)
            .replaceQueryParam("tag", "green").replaceQueryParam("sort", List.of("date", "name"));

    assertThat(builder.build(Map.of("port", 8443, "name", "a/b")))
            .hasToString("https://reader@example.test:8443/files/a%2Fb?tag=green&sort=date&sort=name#top");
  }

}
