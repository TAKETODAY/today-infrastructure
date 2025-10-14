/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.util;

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
    assertThat(components.getScheme()).isEqualTo("mailto");
    assertThat(components.getSchemeSpecificPart()).isEqualTo("user@example.com");
    assertThat(components.getFragment()).isEqualTo("inbox");
  }

  @Test
  void shouldReturnNullForHierarchicalComponents() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", null);

    // when & then
    assertThat(components.getUserInfo()).isNull();
    assertThat(components.getHost()).isNull();
    assertThat(components.getPort()).isEqualTo(-1);
    assertThat(components.getPath()).isNull();
    assertThat(components.getPathSegments()).isEmpty();
    assertThat(components.getQuery()).isNull();
    assertThat(components.getQueryParams()).isEmpty();
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
    assertThat(components1).isEqualTo(components2);
    assertThat(components1).hasSameHashCodeAs(components2);
    assertThat(components1).isNotEqualTo(components3);
  }

  @Test
  void shouldHandleNullSchemeSpecificPart() {
    // when
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", null, "inbox");

    // then
    assertThat(components.getScheme()).isEqualTo("mailto");
    assertThat(components.getSchemeSpecificPart()).isNull();
    assertThat(components.getFragment()).isEqualTo("inbox");
    assertThat(components.toUriString()).isEqualTo("mailto:#inbox");
  }

  @Test
  void shouldHandleNullScheme() {
    // when
    OpaqueUriComponents components = new OpaqueUriComponents(null, "user@example.com", "inbox");

    // then
    assertThat(components.getScheme()).isNull();
    assertThat(components.getSchemeSpecificPart()).isEqualTo("user@example.com");
    assertThat(components.getFragment()).isEqualTo("inbox");
    assertThat(components.toUriString()).isEqualTo("user@example.com#inbox");
  }

  @Test
  void shouldHandleAllNullComponents() {
    // when
    OpaqueUriComponents components = new OpaqueUriComponents(null, null, null);

    // then
    assertThat(components.getScheme()).isNull();
    assertThat(components.getSchemeSpecificPart()).isNull();
    assertThat(components.getFragment()).isNull();
    assertThat(components.toUriString()).isEmpty();
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
    assertThat(components).isNotEqualTo(notAComponent);
  }

  @Test
  void shouldNotEqualNull() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", null);

    // when & then
    assertThat(components).isNotEqualTo(null);
  }

  @Test
  void shouldEqualSameInstance() {
    // given
    OpaqueUriComponents components = new OpaqueUriComponents("mailto", "user@example.com", null);

    // when & then
    assertThat(components).isEqualTo(components);
  }

}