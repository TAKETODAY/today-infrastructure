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

package infra.http.server.reactive;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.stream.Stream;

import infra.http.support.Netty4HttpHeaders;
import infra.util.LinkedCaseInsensitiveMap;
import infra.util.MultiValueMap;
import io.netty.handler.codec.http.DefaultHttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit tests for {@code HeadersAdapters} {@code MultiValueMap} implementations.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
class HeadersAdaptersTests {

  @ParameterizedHeadersTest
  void getWithUnknownHeaderShouldReturnNull(MultiValueMap<String, String> headers) {
    assertThat(headers.get("Unknown")).isNull();
  }

  @ParameterizedHeadersTest
  void getFirstWithUnknownHeaderShouldReturnNull(MultiValueMap<String, String> headers) {
    assertThat(headers.getFirst("Unknown")).isNull();
  }

  @ParameterizedHeadersTest
  void sizeWithMultipleValuesForHeaderShouldCountHeaders(MultiValueMap<String, String> headers) {
    headers.add("TestHeader", "first");
    headers.add("TestHeader", "second");
    assertThat(headers).hasSize(1);
  }

  @ParameterizedHeadersTest
  void keySetShouldNotDuplicateHeaderNames(MultiValueMap<String, String> headers) {
    headers.add("TestHeader", "first");
    headers.add("OtherHeader", "test");
    headers.add("TestHeader", "second");
    assertThat(headers.keySet()).hasSize(2);
  }

  @ParameterizedHeadersTest
  void containsKeyShouldBeCaseInsensitive(MultiValueMap<String, String> headers) {
    headers.add("TestHeader", "first");
    assertThat(headers.containsKey("testheader")).isTrue();
  }

  @ParameterizedHeadersTest
  void addShouldKeepOrdering(MultiValueMap<String, String> headers) {
    headers.add("TestHeader", "first");
    headers.add("TestHeader", "second");
    assertThat(headers.getFirst("TestHeader")).isEqualTo("first");
    assertThat(headers.get("TestHeader").get(0)).isEqualTo("first");
  }

  @ParameterizedHeadersTest
  void putShouldOverrideExisting(MultiValueMap<String, String> headers) {
    headers.add("TestHeader", "first");
    headers.put("TestHeader", Arrays.asList("override"));
    assertThat(headers.getFirst("TestHeader")).isEqualTo("override");
    assertThat(headers.get("TestHeader")).hasSize(1);
  }

  @ParameterizedHeadersTest
  void nullValuesShouldNotFail(MultiValueMap<String, String> headers) {
    headers.add("TestHeader", null);
    assertThat(headers.getFirst("TestHeader")).isNull();
    headers.setOrRemove("TestHeader", (String) null);
    assertThat(headers.getFirst("TestHeader")).isNull();
  }

  @ParameterizedHeadersTest
  void shouldReflectChangesOnKeyset(MultiValueMap<String, String> headers) {
    headers.add("TestHeader", "first");
    assertThat(headers.keySet()).hasSize(1);
    headers.keySet().removeIf("TestHeader"::equals);
    assertThat(headers.keySet()).isEmpty();
  }

  @ParameterizedHeadersTest
  void shouldFailIfHeaderRemovedFromKeyset(MultiValueMap<String, String> headers) {
    headers.add("TestHeader", "first");
    assertThat(headers.keySet()).hasSize(1);
    Iterator<String> names = headers.keySet().iterator();
    assertThat(names.hasNext()).isTrue();
    assertThat(names.next()).isEqualTo("TestHeader");
    names.remove();
    assertThatThrownBy(names::remove).isInstanceOf(IllegalStateException.class);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("headers")
  @interface ParameterizedHeadersTest {
  }

  static Stream<Arguments> headers() {
    return Stream.of(
            arguments(named("Map", MultiValueMap.forAdaption(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH)))),
            arguments(named("Netty", new Netty4HttpHeaders(new DefaultHttpHeaders())))
    );
  }

}
