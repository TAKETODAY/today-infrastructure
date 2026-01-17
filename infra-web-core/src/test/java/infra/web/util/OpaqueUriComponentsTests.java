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

package infra.web.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:37
 */
class OpaqueUriComponentsTests {

  @Test
  void shouldCreateOpaqueUriComponents() {
    // when
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", "inbox");

    // then
    Assertions.assertThat(components.getScheme()).isEqualTo("mailto");
    Assertions.assertThat(components.getSchemeSpecificPart()).isEqualTo("user@example.com");
    Assertions.assertThat(components.getFragment()).isEqualTo("inbox");
  }

  @Test
  void shouldReturnNullForHierarchicalComponents() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", null);

    // when & then
    Assertions.assertThat(components.getUserInfo()).isNull();
    Assertions.assertThat(components.getHost()).isNull();
    Assertions.assertThat(components.getPort()).isEqualTo(-1);
    Assertions.assertThat(components.getPath()).isNull();
    Assertions.assertThat(components.getPathSegments()).isEmpty();
    Assertions.assertThat(components.getQuery()).isNull();
    Assertions.assertThat(components.getQueryParams()).isEmpty();
  }

  @Test
  void shouldEncodeReturnSelf() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", null);

    // when
    UriComponents encoded = components.encode(Charset.defaultCharset());

    // then
    assertThat(encoded).isSameAs(components);
  }

  @Test
  void shouldNormalizeReturnSelf() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", null);

    // when
    UriComponents normalized = components.normalize();

    // then
    assertThat(normalized).isSameAs(components);
  }

  @Test
  void shouldExpandUriComponents() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "{email}", "{fragment}");

    // when
    UriComponents expanded = components.expand("user@example.com", "inbox");

    // then
    assertThat(expanded.getScheme()).isEqualTo("mailto");
    assertThat(expanded.getSchemeSpecificPart()).isEqualTo("user@example.com");
    assertThat(expanded.getFragment()).isEqualTo("inbox");
  }

  @Test
  void shouldBuildUriString() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", "inbox");

    // when
    String uriString = components.toUriString();

    // then
    assertThat(uriString).isEqualTo("mailto:user@example.com#inbox");
  }

  @Test
  void shouldBuildUriStringWithoutFragment() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", null);

    // when
    String uriString = components.toUriString();

    // then
    assertThat(uriString).isEqualTo("mailto:user@example.com");
  }

  @Test
  void shouldCreateURI() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", "inbox");

    // when
    URI uri = components.toURI();

    // then
    assertThat(uri).isNotNull();
    assertThat(uri.getScheme()).isEqualTo("mailto");
    assertThat(uri.getSchemeSpecificPart()).isEqualTo("user@example.com");
    assertThat(uri.getFragment()).isEqualTo("inbox");
  }

  @Test
  void shouldCopyToUriComponentsBuilder() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", "inbox");
    UriComponentsBuilder builder = UriComponentsBuilder.create();

    // when
    components.copyToUriComponentsBuilder(builder);

    // then
    UriComponents copied = builder.build();
    assertThat(copied.getScheme()).isEqualTo("mailto");
    assertThat(copied.getSchemeSpecificPart()).isEqualTo("user@example.com");
    assertThat(copied.getFragment()).isEqualTo("inbox");
  }

  @Test
  void shouldImplementEqualsAndHashCode() {
    // given
    OpaqueUriComponents components1 = new OpaqueUriComponents("mailto", "user@example.com", "inbox");
    OpaqueUriComponents components2 = new OpaqueUriComponents("mailto", "user@example.com", "inbox");
    OpaqueUriComponents components3 = new OpaqueUriComponents("mailto", "user@example.com", null);

    // when & then
    Assertions.assertThat(components1).isEqualTo(components2);
    Assertions.assertThat(components1).hasSameHashCodeAs(components2);
    Assertions.assertThat(components1).isNotEqualTo(components3);
  }

  @Test
  void shouldHandleNullSchemeSpecificPart() {
    // when
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", null, "inbox");

    // then
    Assertions.assertThat(components.getScheme()).isEqualTo("mailto");
    Assertions.assertThat(components.getSchemeSpecificPart()).isNull();
    Assertions.assertThat(components.getFragment()).isEqualTo("inbox");
    Assertions.assertThat(components.toUriString()).isEqualTo("mailto:#inbox");
  }

  @Test
  void shouldHandleNullScheme() {
    // when
    OpaqueUriComponents components = new OpaqueUriComponents(null, "user@example.com", "inbox");

    // then
    Assertions.assertThat(components.getScheme()).isNull();
    Assertions.assertThat(components.getSchemeSpecificPart()).isEqualTo("user@example.com");
    Assertions.assertThat(components.getFragment()).isEqualTo("inbox");
    Assertions.assertThat(components.toUriString()).isEqualTo("user@example.com#inbox");
  }

  @Test
  void shouldHandleAllNullComponents() {
    // when
    OpaqueUriComponents components = new OpaqueUriComponents(null, null, null);

    // then
    Assertions.assertThat(components.getScheme()).isNull();
    Assertions.assertThat(components.getSchemeSpecificPart()).isNull();
    Assertions.assertThat(components.getFragment()).isNull();
    Assertions.assertThat(components.toUriString()).isEmpty();
  }

  @Test
  void shouldExpandWithMapVariables() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("{scheme}", "{email}", "{fragment}");

    // when
    UriComponents expanded = components.expand(java.util.Map.of(
            "scheme", "mailto",
            "email", "user@example.com",
            "fragment", "inbox"
    ));

    // then
    assertThat(expanded.getScheme()).isEqualTo("mailto");
    assertThat(expanded.getSchemeSpecificPart()).isEqualTo("user@example.com");
    assertThat(expanded.getFragment()).isEqualTo("inbox");
  }

  @Test
  void shouldExpandWithArrayVariables() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("{scheme}", "{email}", "{fragment}");

    // when
    UriComponents expanded = components.expand("mailto", "user@example.com", "inbox");

    // then
    assertThat(expanded.getScheme()).isEqualTo("mailto");
    assertThat(expanded.getSchemeSpecificPart()).isEqualTo("user@example.com");
    assertThat(expanded.getFragment()).isEqualTo("inbox");
  }

  @Test
  void shouldNotModifyWhenEncodingWithSpecificCharset() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", null);

    // when
    UriComponents encoded = components.encode(Charset.forName("UTF-8"));

    // then
    assertThat(encoded).isSameAs(components);
  }

  @Test
  void shouldBuildUriStringWithOnlyScheme() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("https", null, null);

    // when
    String uriString = components.toUriString();

    // then
    assertThat(uriString).isEqualTo("https:");
  }

  @Test
  void shouldBuildUriStringWithOnlySchemeAndFragment() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("https", null, "section");

    // when
    String uriString = components.toUriString();

    // then
    assertThat(uriString).isEqualTo("https:#section");
  }

  @Test
  void shouldCreateURIWithNullComponents() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents(null, null, null);

    // when
    URI uri = components.toURI();

    // then
    assertThat(uri).isNotNull();
    assertThat(uri.getScheme()).isNull();
    assertThat(uri.getFragment()).isNull();
    assertThat(uri.getSchemeSpecificPart()).isEqualTo("");
  }

  @Test
  void shouldCopyOnlySchemeToBuilder() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("https", null, null);
    UriComponentsBuilder builder = UriComponentsBuilder.create();

    // when
    components.copyToUriComponentsBuilder(builder);

    // then
    UriComponents copied = builder.build();
    assertThat(copied.getScheme()).isEqualTo("https");
    assertThat(copied.getSchemeSpecificPart()).isNull();
    assertThat(copied.getFragment()).isNull();
  }

  @Test
  void shouldCopyOnlyFragmentToBuilder() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents(null, null, "section");
    UriComponentsBuilder builder = UriComponentsBuilder.create();

    // when
    components.copyToUriComponentsBuilder(builder);

    // then
    UriComponents copied = builder.build();
    assertThat(copied.getScheme()).isNull();
    assertThat(copied.getSchemeSpecificPart()).isNull();
    assertThat(copied.getFragment()).isEqualTo("section");
  }

  @Test
  void shouldNotEqualDifferentClass() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", null);
    String notAComponent = "not a component";

    // when & then
    Assertions.assertThat(components).isNotEqualTo(notAComponent);
  }

  @Test
  void shouldNotEqualNull() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", null);

    // when & then
    Assertions.assertThat(components).isNotEqualTo(null);
  }

  @Test
  void shouldEqualSameInstance() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", null);

    // when & then
    Assertions.assertThat(components).isEqualTo(components);
  }

}