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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:32
 */
class UriBuilderTests {

  @Test
  void shouldCreateUriComponentsBuilderViaStaticFactory() {
    // when
    UriComponentsBuilder builder = UriBuilder.forUriComponents();

    // then
    assertThat(builder).isNotNull();
    assertThat(builder.toUriString()).isEmpty();
  }

  @Test
  void shouldSetScheme() {
    // given
    UriComponentsBuilder builder = UriBuilder.forUriComponents();

    // when
    UriBuilder result = builder.scheme("https");

    // then
    assertThat(result).isSameAs(builder);
    assertThat(builder.toUriString()).isEqualTo("https:");
  }

  @Test
  void shouldSetUserInfo() {
    // given
    UriComponentsBuilder builder = UriBuilder.forUriComponents();

    // when
    UriBuilder result = builder.userInfo("user:password");

    // then
    assertThat(result).isSameAs(builder);
    assertThat(builder.toUriString()).isEqualTo("//user:password@");
  }

  @Test
  void shouldSetHost() {
    // given
    UriComponentsBuilder builder = UriBuilder.forUriComponents();

    // when
    UriBuilder result = builder.host("example.com");

    // then
    assertThat(result).isSameAs(builder);
    assertThat(builder.toUriString()).isEqualTo("//example.com");
  }

  @Test
  void shouldSetPath() {
    // given
    UriComponentsBuilder builder = UriBuilder.forUriComponents();

    // when
    UriBuilder result = builder.path("/test");

    // then
    assertThat(result).isSameAs(builder);
    assertThat(builder.toUriString()).isEqualTo("/test");
  }

  @Test
  void shouldReplacePath() {
    // given
    UriComponentsBuilder builder = UriBuilder.forUriComponents();
    builder.path("/original");

    // when
    UriBuilder result = builder.replacePath("/replacement");

    // then
    assertThat(result).isSameAs(builder);
    assertThat(builder.toUriString()).isEqualTo("/replacement");
  }

  @Test
  void shouldAppendPathSegments() {
    // given
    UriComponentsBuilder builder = UriBuilder.forUriComponents();

    // when
    UriBuilder result = builder.pathSegment("first", "second", "third");

    // then
    assertThat(result).isSameAs(builder);
    assertThat(builder.toUriString()).isEqualTo("/first/second/third");
  }

  @Test
  void shouldSetQuery() {
    // given
    UriComponentsBuilder builder = UriBuilder.forUriComponents();

    // when
    UriBuilder result = builder.query("param1=value1&param2=value2");

    // then
    assertThat(result).isSameAs(builder);
    assertThat(builder.toUriString()).isEqualTo("?param1=value1&param2=value2");
  }

  @Test
  void shouldReplaceQuery() {
    // given
    UriComponentsBuilder builder = UriBuilder.forUriComponents();
    builder.query("original=value");

    // when
    UriBuilder result = builder.replaceQuery("replacement=value");

    // then
    assertThat(result).isSameAs(builder);
    assertThat(builder.toUriString()).isEqualTo("?replacement=value");
  }

  @Test
  void shouldAddQueryParameter() {
    // given
    UriComponentsBuilder builder = UriBuilder.forUriComponents();

    // when
    UriBuilder result = builder.queryParam("param", "value");

    // then
    assertThat(result).isSameAs(builder);
    assertThat(builder.toUriString()).isEqualTo("?param=value");
  }

  @Test
  void shouldReplaceQueryParameter() {
    // given
    UriComponentsBuilder builder = UriBuilder.forUriComponents();
    builder.queryParam("param", "original");

    // when
    UriBuilder result = builder.replaceQueryParam("param", "replacement");

    // then
    assertThat(result).isSameAs(builder);
    assertThat(builder.toUriString()).isEqualTo("?param=replacement");
  }

  @Test
  void shouldSetFragment() {
    // given
    UriComponentsBuilder builder = UriBuilder.forUriComponents();

    // when
    UriBuilder result = builder.fragment("section1");

    // then
    assertThat(result).isSameAs(builder);
    assertThat(builder.toUriString()).isEqualTo("#section1");
  }

}